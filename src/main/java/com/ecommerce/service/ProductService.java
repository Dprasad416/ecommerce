package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get one product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    // Search products by name or category
    public List<Product> searchProducts(String name, String category) {
        if (name != null && category != null) {
            return productRepository.findByNameContainingIgnoreCaseAndCategory(name, category);
        } else if (name != null) {
            return productRepository.findByNameContainingIgnoreCase(name);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    // Get only in-stock products
    public List<Product> getInStockProducts() {
        return productRepository.findByStockGreaterThan(0);
    }

    // Admin: add a new product
    public Product addProduct(String name, String description, BigDecimal price,
                              Integer stock, String category, String imageUrl) {
        Product product = new Product(name, description, price, stock, category);
        product.setImageUrl(imageUrl);
        return productRepository.save(product);
    }

    // Admin: update product stock after purchase
    public void reduceStock(Long productId, int quantity) {
        Product product = getProductById(productId);
        if (product.getStock() < quantity) {
            throw new RuntimeException("Not enough stock for: " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    // Admin: delete a product
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
