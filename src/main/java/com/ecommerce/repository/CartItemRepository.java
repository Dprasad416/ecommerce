package com.ecommerce.repository;

import com.ecommerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Get all cart items for a user
    List<CartItem> findByUserId(Long userId);

    // Find a specific product already in the cart (to update quantity)
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    // Remove all items from a user's cart (after checkout)
    void deleteByUserId(Long userId);
}
