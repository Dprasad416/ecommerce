package com.ecommerce.repository;

import com.ecommerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for a user, newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Get orders by status (e.g. all PENDING orders)
    List<Order> findByStatus(String status);
}
