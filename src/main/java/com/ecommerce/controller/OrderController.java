package com.ecommerce.controller;

import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // POST /api/orders/checkout
    // Body: { "userId": 1, "shippingAddress": "123 Main St, City" }
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, String> body) {
        try {
            Long userId = Long.valueOf(body.get("userId"));
            String address = body.get("shippingAddress");

            Order order = orderService.checkout(userId, address);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/orders?userId=1
    @GetMapping
    public ResponseEntity<List<Order>> getOrderHistory(@RequestParam Long userId) {
        return ResponseEntity.ok(orderService.getOrderHistory(userId));
    }

    // GET /api/orders/{id}?userId=1
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, @RequestParam Long userId) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/orders/{id}/cancel?userId=1
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestParam Long userId) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/orders/{id}/status  (Admin)
    // Body: { "status": "SHIPPED" }
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
