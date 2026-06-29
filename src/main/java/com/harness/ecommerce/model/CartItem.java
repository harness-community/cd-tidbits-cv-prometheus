package com.harness.ecommerce.model;

import javax.persistence.*;
import javax.validation.constraints.Min;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    public CartItem() {}

    public CartItem(Long id, Cart cart, Product product, int quantity) {
        this.id = id;
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Cart getCart() { return cart; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    public void setId(Long id) { this.id = id; }
    public void setCart(Cart cart) { this.cart = cart; }
    public void setProduct(Product product) { this.product = product; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Cart cart;
        private Product product;
        private int quantity;

        public Builder id(Long id)               { this.id = id; return this; }
        public Builder cart(Cart cart)           { this.cart = cart; return this; }
        public Builder product(Product product)  { this.product = product; return this; }
        public Builder quantity(int quantity)    { this.quantity = quantity; return this; }

        public CartItem build() {
            CartItem ci = new CartItem();
            ci.id = this.id; ci.cart = this.cart;
            ci.product = this.product; ci.quantity = this.quantity;
            return ci;
        }
    }
}
