package com.harness.ecommerce.model;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "orders")
public class Order {

    public enum Status {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public Order() {}

    public Order(Long id, User user, List<OrderItem> items, BigDecimal total,
                 Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private User user;
        private List<OrderItem> items = new ArrayList<>();
        private BigDecimal total;
        private Status status = Status.PENDING;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt;

        public Builder id(Long id)                       { this.id = id; return this; }
        public Builder user(User user)                   { this.user = user; return this; }
        public Builder items(List<OrderItem> items)      { this.items = items; return this; }
        public Builder total(BigDecimal total)           { this.total = total; return this; }
        public Builder status(Status status)             { this.status = status; return this; }
        public Builder createdAt(LocalDateTime t)        { this.createdAt = t; return this; }
        public Builder updatedAt(LocalDateTime t)        { this.updatedAt = t; return this; }

        public Order build() {
            Order o = new Order();
            o.id = this.id; o.user = this.user; o.items = this.items;
            o.total = this.total; o.status = this.status;
            o.createdAt = this.createdAt; o.updatedAt = this.updatedAt;
            return o;
        }
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", status=" + status + ", total=" + total + "}";
    }
}
