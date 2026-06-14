package com.ecommerce.controller;

import com.ecommerce.model.Product;
import com.ecommerce.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // GET /api/products
    // GET /api/products?name=shirt
    // GET /api/products?category=Electronics
    // GET /api/products?name=shirt&category=Clothing
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {

        if (name == null && category == null) {
            return ResponseEntity.ok(productService.getAllProducts());
        }
        return ResponseEntity.ok(productService.searchProducts(name, category));
    }

    // GET /api/products/in-stock
    @GetMapping("/in-stock")
    public ResponseEntity<List<Product>> getInStock() {
        return ResponseEntity.ok(productService.getInStockProducts());
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/products  (Admin)
    // Body: { "name": "...", "description": "...", "price": 999.99, "stock": 50, "category": "Electronics", "imageUrl": "..." }
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Map<String, Object> body) {
        try {
            String name        = (String) body.get("name");
            String description = (String) body.get("description");
            BigDecimal price   = new BigDecimal(body.get("price").toString());
            Integer stock      = Integer.valueOf(body.get("stock").toString());
            String category    = (String) body.get("category");
            String imageUrl    = (String) body.get("imageUrl");

            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
            }

            Product product = productService.addProduct(name, description, price, stock, category, imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid product data: " + e.getMessage()));
        }
    }

    // DELETE /api/products/{id}  (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
