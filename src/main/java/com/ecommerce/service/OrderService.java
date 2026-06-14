package com.ecommerce.service;

import com.ecommerce.model.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    // Checkout: convert cart items into a real order
    @Transactional   // If anything fails, the whole checkout rolls back
    public Order checkout(Long userId, String shippingAddress) {
        List<CartItem> cartItems = cartService.getCart(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cannot checkout with an empty cart");
        }

        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new RuntimeException("Shipping address is required");
        }

        User user = userService.findById(userId);

        // Calculate total
        BigDecimal total = cartService.getCartTotal(userId);

        // Create the order
        Order order = new Order(user, total, shippingAddress);
        Order savedOrder = orderRepository.save(order);

        // Create an OrderItem for each cart item
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem(
                savedOrder,
                cartItem.getProduct(),
                cartItem.getQuantity(),
                cartItem.getProduct().getPrice()   // Snapshot price right now
            );
            savedOrder.getItems().add(orderItem);

            // Reduce stock for each product
            productService.reduceStock(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        orderRepository.save(savedOrder);

        // Clear the cart after successful checkout
        cartService.clearCart(userId);

        return savedOrder;
    }

    // Get all orders for a user
    public List<Order> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get one order by ID (with ownership check)
    public Order getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return order;
    }

    // Admin: update order status
    public Order updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // Cancel an order (only if still PENDING)
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = getOrderById(orderId, userId);
        if (!order.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING orders can be cancelled");
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productService.addProduct(
                product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getCategory(), product.getImageUrl()
            );
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}
