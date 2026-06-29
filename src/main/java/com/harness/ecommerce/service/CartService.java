package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.InsufficientStockException;
import com.harness.ecommerce.exception.ResourceNotFoundException;
import com.harness.ecommerce.model.Cart;
import com.harness.ecommerce.model.CartItem;
import com.harness.ecommerce.model.Product;
import com.harness.ecommerce.model.User;
import com.harness.ecommerce.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;

    public Cart getOrCreateCart(Long userId) {
        User user = userService.getById(userId);
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    public Cart addItem(Long userId, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Cart cart = getOrCreateCart(userId);
        Product product = productService.getById(productId);

        if (!product.isActive()) throw new IllegalArgumentException("Product is not available");
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            int newQty = existing.get().getQuantity() + quantity;
            if (product.getStock() < newQty) {
                throw new InsufficientStockException(product.getName(), newQty, product.getStock());
            }
            existing.get().setQuantity(newQty);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(item);
        }
        return cartRepository.save(cart);
    }

    public Cart removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            return removeItem(userId, productId);
        }
        Cart cart = getOrCreateCart(userId);
        cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    Product product = item.getProduct();
                    if (product.getStock() < quantity) {
                        throw new InsufficientStockException(product.getName(), quantity, product.getStock());
                    }
                    item.setQuantity(quantity);
                });
        return cartRepository.save(cart);
    }

    public Cart clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getTotal();
    }

    @Transactional(readOnly = true)
    public int getItemCount(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cart.getTotalItems();
    }
}
