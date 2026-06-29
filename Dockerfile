# ── Stage 1: Build ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first and download dependencies (layer cached separately)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build the JAR (skip tests — run them in CI separately)
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# APP_MODE controls metric behaviour:
#   stable   → increments counter at ~1/sec  (CV threshold: pass)
#   unstable → increments counter at ~10-15/sec (CV threshold: fail)
ARG APP_MODE=stable
ENV APP_MODE=${APP_MODE}

WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/target/harness-ecommerce-app-1.0.0-SNAPSHOT.jar app.jar

# Expose the Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
