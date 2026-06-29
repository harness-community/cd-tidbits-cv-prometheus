# CD | Tidbits | Continuous Verification with Prometheus

> **Bite-sized how-to** | ~20 min setup

---

## What is Continuous Verification?

Deployments can introduce regressions that aren't caught by tests — a spike in request rate, a memory leak, a service that starts misbehaving under load. By the time you notice, the bad version is already running.

Harness Continuous Verification (CV) addresses this by automatically querying your monitoring tool during each deployment. It evaluates the metrics from the new version against a defined threshold. If the numbers deviate — Harness fails the pipeline and triggers a rollback before the bad version can cause damage.

**How it works:**

1. You add a **Verify** step to your pipeline after the deployment step.
2. Harness queries Prometheus during the verification window.
3. If a metric breaches the configured threshold, Harness marks the verification as failed and pauses for manual intervention.
4. You choose to perform a **Stage Rollback** — reverting to the previous good version.

---

## What does this Tidbit demonstrate?

A full continuous verification cycle using the Harness e-commerce Spring Boot application deployed to Kubernetes:

1. **Deploy `stable`** — app emits normal metrics (~1 request/sec). CV passes.
2. **Deploy `unstable`** — app emits a high request spike (~50–100 requests/sec). CV detects the breach and fails the pipeline.
3. **Stage Rollback** — Harness automatically rolls back to the previous stable deployment.

---

## How the app emits metrics

The e-commerce app uses **Spring Boot Actuator** and **Micrometer** to expose a Prometheus counter. Here is the instrumentation code:

```java
// src/main/java/com/harness/ecommerce/metrics/MetricsGenerator.java

@Component
public class MetricsGenerator {

    private final Counter requestCounter;
    private final String appMode;

    public MetricsGenerator(MeterRegistry registry) {
        this.requestCounter = Counter.builder("ecommerce_requests_total")
                .description("Total number of requests processed by the ecommerce app")
                .register(registry);
        this.appMode = System.getenv().getOrDefault("APP_MODE", "stable");
    }

    @Scheduled(fixedRate = 1000)
    public void generateMetrics() {
        if ("unstable".equals(appMode)) {
            // Regression behaviour — high spike
            int spike = 50 + (int) (Math.random() * 51);
            requestCounter.increment(spike);
        } else {
            // Normal stable behaviour
            requestCounter.increment();
        }
    }
}
```

**What Prometheus scrapes** — the `/actuator/prometheus` endpoint exposes:
```
# HELP ecommerce_requests_total Total number of requests processed by the ecommerce app
# TYPE ecommerce_requests_total counter
ecommerce_requests_total 42.0
```

**What Harness CV evaluates** — the PromQL query in the health source:
```
rate(ecommerce_requests_total{namespace="cv-staging"}[5m])
```

- `stable` image: rate ≈ **1/sec** → below threshold → CV passes
- `unstable` image: rate ≈ **50–100/sec** → above threshold → CV fails

---

## Prerequisites

Before you start, make sure you have:

- A Harness project with CD enabled
- A Kubernetes cluster with a Harness Kubernetes connector configured
- A Harness delegate running inside the cluster
- Prometheus installed on the cluster (Step 1 walks you through this)
- kubectl and Helm installed

---

## Project Structure

```
cd-tidbits-cv-prometheus/
├── .harness/
│   └── pipeline.yaml                    — CD pipeline (Rolling Deploy → Verify)
├── k8s/
│   ├── deployment.yaml                  — Kubernetes Deployment manifest
│   └── values.yaml                      — Harness artifact expression for image injection
├── src/
│   └── main/java/com/harness/ecommerce/
│       └── metrics/MetricsGenerator.java — Prometheus counter instrumentation
├── Dockerfile                           — Two-stage Docker build
└── pom.xml                              — Maven build with Actuator + Micrometer
```

---

## Step 1 — Install Prometheus

Install Prometheus on your cluster using Helm:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install prometheus prometheus-community/prometheus \
  --namespace monitoring --create-namespace
```

Verify the pods are running:

```bash
kubectl get pods -n monitoring
```

Prometheus is accessible inside the cluster at:
```
http://prometheus-server.monitoring.svc.cluster.local
```

> **Note:** This is an internal cluster DNS address — only reachable from inside the cluster via your Harness delegate. Enter this URL in the Harness connector configuration, not in a browser.

---

## Step 2 — Create a Prometheus Connector

1. Go to **Project Settings → Connectors → New Connector → Prometheus**
2. Name: `prometheus-connector`
3. URL: `http://prometheus-server.monitoring.svc.cluster.local`
4. Connectivity Mode: **Connect through a Harness Delegate**
5. Select your delegate and test the connection

---

## Step 3 — Import the Pipeline

1. Open `.harness/pipeline.yaml` and update the placeholders:
   - `YOUR_PROJECT_ID` → your Harness project identifier
   - `YOUR_ORG_ID` → your Harness org identifier
   - Commit the changes
2. Go to **Deployments → Pipelines → Import from Git**
3. Select your GitHub connector, point it to this repository, and select `.harness/pipeline.yaml`
4. Hit **Import**

---

## Step 4 — Set Up Service, Environment, and Infrastructure

**Service**

1. Go to **Deployments → Services → New Service**
2. Name: `cv-prometheus-service`, Deployment Type: **Kubernetes**
3. Add Manifest → K8s Manifest → point to the `k8s/` folder in this repo
4. Add Primary Artifact → Docker Registry → image: `nidayra/ecommerce-cv-app`, tag: `<+input>`

**Environment and Infrastructure**

1. Create an environment: `cv-staging` (Pre-Production)
2. Add an Infrastructure Definition pointing to your Kubernetes cluster, name: `cv-staging-infra`, namespace: `cv-staging`

---

## Step 5 — Set Up the Monitored Service

1. Go to **Deployments → Services → cv-prometheus-service → Monitored Services → New Monitored Service**
2. Service: `cv-prometheus-service`, Environment: `cv-staging`
3. Add a Health Source:
   - Name: `cvprometheushealth`
   - Provider Type: **Prometheus**
   - Connector: `prometheus-connector`
4. Configure the metric:
   - Metric Name: `Ecommerce Requests`
   - Group Name: `ecommerce-metrics`
   - PromQL Query:
     ```
     rate(ecommerce_requests_total{namespace="cv-staging"}[5m])
     ```
   - Service Instance Identifier: `pod`
   - Assign to: **Continuous Verification**
5. Under **Advanced → Fail-Fast Thresholds**, add a threshold:
   - Metric: `Ecommerce Requests`
   - Action: **Fail Immediately**
   - Criteria: **Absolute Value**, Greater than: `20`
6. Save the health source and monitored service

---

## Step 6 — Run the Demo

**Establish the baseline — deploy `stable`**

Run the pipeline with artifact tag `stable`. The Verify step runs for 5 minutes. With a rate of ~1/sec, it stays well below the threshold of 20 — CV passes.

**Detect the regression — deploy `unstable`**

Run the pipeline with artifact tag `unstable`. The `unstable` image emits 50–100 requests/sec — far above the threshold. Harness detects the breach and fails the Verify step. The pipeline pauses for manual intervention — click **Perform Action → StageRollback** to revert to the previous stable deployment.

Navigate to the Verify step → **Metrics** tab to see the failure details.

---

## Common Issues & Tips

**Prometheus connector test fails**
- Use the internal cluster DNS URL (`http://prometheus-server.monitoring.svc.cluster.local`), not a NodePort or external URL. The connector must go through your delegate.

**"No records found" when saving the health source**
- Expected — the app hasn't been deployed yet. Save and proceed. Records appear after the first pipeline run.

**CV passes for both stable and unstable**
- Check the PromQL in the health source includes `rate(...)` — not the raw counter value.
- Confirm the threshold is set under **Fail-Fast Thresholds**, not Ignore Thresholds.
- Verify the correct image tags are being deployed by checking `kubectl get pods -n cv-staging -o wide`.

---

## Resources

- [Configure the Verify Step](https://developer.harness.io/docs/continuous-delivery/verify/configure-cv/verify-deployments)
- [Verify Kubernetes Deployments with Prometheus](https://developer.harness.io/docs/continuous-delivery/get-started/tutorials/kubernetes-container-deployments/prometheus/)
- [Configure Prometheus as a Health Source](https://developer.harness.io/docs/continuous-delivery/verify/configure-cv/health-sources/prometheus/)
- [Monitored Services Overview](https://developer.harness.io/docs/service-reliability-management/monitored-service/create-monitored-service/)
