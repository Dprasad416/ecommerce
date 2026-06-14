package com.ecommerce.service;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    // Get all items in a user's cart
    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    // Add a product to the cart (or increase quantity if already in cart)
    public CartItem addToCart(Long userId, Long productId, int quantity) {
        User    user    = userService.findById(userId);
        Product product = productService.getProductById(productId);

        if (product.getStock() < quantity) {
            throw new RuntimeException("Only " + product.getStock() + " units available");
        }

        // If product already in cart → increase quantity
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        }

        // Otherwise add new cart item
        return cartItemRepository.save(new CartItem(user, product, quantity));
    }

    // Update quantity of a cart item
    public CartItem updateQuantity(Long cartItemId, Long userId, int newQuantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security: make sure this cart item belongs to this user
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        if (newQuantity <= 0) {
            cartItemRepository.delete(item);
            return null;   // Removed
        }

        item.setQuantity(newQuantity);
        return cartItemRepository.save(item);
    }

    // Remove one item from cart
    public void removeFromCart(Long cartItemId, Long userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        cartItemRepository.delete(item);
    }

    // Calculate cart total
    public BigDecimal getCartTotal(Long userId) {
        return getCart(userId).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Clear entire cart (called after checkout)
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
