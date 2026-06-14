package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;   // Use BigDecimal for money, never double!

    @Column(nullable = false)
    private Integer stock;      // How many are available

    private String category;    // Electronics, Clothing, Books, etc.

    private String imageUrl;    // URL to product image

    public Product(String name, String description, BigDecimal price, Integer stock, String category) {
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.stock       = stock;
        this.category    = category;
    }
}
