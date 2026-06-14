package com.ecommerce.controller;

import com.ecommerce.model.CartItem;
import com.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // GET /api/cart?userId=1
    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(@RequestParam Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    // GET /api/cart/total?userId=1
    @GetMapping("/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotal(@RequestParam Long userId) {
        return ResponseEntity.ok(Map.of("total", cartService.getCartTotal(userId)));
    }

    // POST /api/cart
    // Body: { "userId": 1, "productId": 5, "quantity": 2 }
    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body) {
        try {
            Long userId    = Long.valueOf(body.get("userId").toString());
            Long productId = Long.valueOf(body.get("productId").toString());
            int quantity   = Integer.parseInt(body.getOrDefault("quantity", 1).toString());

            CartItem item = cartService.addToCart(userId, productId, quantity);
            return ResponseEntity.ok(item);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/cart/{cartItemId}
    // Body: { "userId": 1, "quantity": 3 }
    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long cartItemId, @RequestBody Map<String, Object> body) {
        try {
            Long userId  = Long.valueOf(body.get("userId").toString());
            int quantity = Integer.parseInt(body.get("quantity").toString());

            CartItem item = cartService.updateQuantity(cartItemId, userId, quantity);
            if (item == null) {
                return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
            }
            return ResponseEntity.ok(item);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/cart/{cartItemId}?userId=1
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId, @RequestParam Long userId) {
        try {
            cartService.removeFromCart(cartItemId, userId);
            return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
