package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;    // Always stored as BCrypt hash

    @Column(nullable = false)
    private String role = "CUSTOMER";   // CUSTOMER or ADMIN

    private String address;
    private String phone;

    public User(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
    }
}
