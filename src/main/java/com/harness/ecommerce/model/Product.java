package com.harness.ecommerce.model;

import javax.persistence.*;
import javax.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    public Product() {}

    public Product(Long id, String name, String description, BigDecimal price,
                   Integer stock, String category, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getCategory() { return category; }
    public boolean isActive() { return active; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setCategory(String category) { this.category = category; }
    public void setActive(boolean active) { this.active = active; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String category;
        private boolean active = true;

        public Builder id(Long id)                   { this.id = id; return this; }
        public Builder name(String name)             { this.name = name; return this; }
        public Builder description(String d)         { this.description = d; return this; }
        public Builder price(BigDecimal price)       { this.price = price; return this; }
        public Builder stock(Integer stock)          { this.stock = stock; return this; }
        public Builder category(String category)     { this.category = category; return this; }
        public Builder active(boolean active)        { this.active = active; return this; }

        public Product build() {
            Product p = new Product();
            p.id = this.id; p.name = this.name; p.description = this.description;
            p.price = this.price; p.stock = this.stock; p.category = this.category;
            p.active = this.active;
            return p;
        }
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price + ", stock=" + stock + "}";
    }
}
