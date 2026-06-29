package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.InsufficientStockException;
import com.harness.ecommerce.exception.ResourceNotFoundException;
import com.harness.ecommerce.model.*;
import com.harness.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;

    /**
     * Checkout: converts the user's cart into an Order, decrements stock.
     */
    public Order checkout(Long userId) {
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with an empty cart");
        }

        // Validate stock before committing
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), item.getQuantity(), product.getStock());
            }
        }

        User user = userService.getById(userId);
        Order order = Order.builder().user(user).total(BigDecimal.ZERO).build();

        for (CartItem item : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(item.getProduct())
                    .quantity(item.getQuantity())
                    .priceAtPurchase(item.getProduct().getPrice())
                    .build();
            order.getItems().add(orderItem);
            productService.updateStock(item.getProduct().getId(), -item.getQuantity());
        }

        order.setTotal(order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Order saved = orderRepository.save(order);
        cartService.clearCart(userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> getByStatus(Order.Status status) {
        return orderRepository.findByStatus(status);
    }

    public Order updateStatus(Long id, Order.Status newStatus) {
        Order order = getById(id);
        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new IllegalArgumentException("Cannot update a cancelled order");
        }
        if (order.getStatus() == Order.Status.DELIVERED) {
            throw new IllegalArgumentException("Cannot update a delivered order");
        }
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    public Order cancelOrder(Long id) {
        Order order = getById(id);
        if (order.getStatus() == Order.Status.SHIPPED || order.getStatus() == Order.Status.DELIVERED) {
            throw new IllegalArgumentException("Cannot cancel a " + order.getStatus() + " order");
        }
        // Restore stock
        for (OrderItem item : order.getItems()) {
            productService.updateStock(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus(Order.Status.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpend(Long userId) {
        BigDecimal total = orderRepository.sumTotalByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return orderRepository.countPendingOrders();
    }
}
