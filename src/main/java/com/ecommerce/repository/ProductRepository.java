package com.ecommerce.repository;

import com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search products by name (case-insensitive, partial match)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Filter by category
    List<Product> findByCategory(String category);

    // Get only in-stock products
    List<Product> findByStockGreaterThan(Integer stock);

    // Search by name AND category
    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);
}
