package com.harness.ecommerce.controller;

import com.harness.ecommerce.model.Order;
import com.harness.ecommerce.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/users/{userId}/checkout")
    public ResponseEntity<Order> checkout(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(userId));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders(
            @RequestParam(required = false) Order.Status status) {
        if (status != null) {
            return ResponseEntity.ok(orderService.getByStatus(status));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id, @RequestParam Order.Status status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/users/{userId}/orders/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
                "totalSpend", orderService.getTotalSpend(userId),
                "pendingOrders", orderService.countPending()
        ));
    }
}
