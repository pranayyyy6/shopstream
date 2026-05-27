package com.shopstream.product.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity                          // tells JPA this is a database table
@Table(name = "products")        // table name in PostgreSQL
@Getter                          // Lombok generates all getters
@Setter                          // Lombok generates all setters
@NoArgsConstructor               // Lombok generates empty constructor (JPA needs this)
@AllArgsConstructor              // Lombok generates constructor with all fields
@Builder                         // lets you do Product.builder().name("x").build()
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    // UUID instead of Long — why?
    // Long IDs leak business info (competitor sees product ID 1234,
    // knows you have ~1234 products). UUID reveals nothing.
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    // BigDecimal for money — NEVER use double/float for currency.
    // double: 0.1 + 0.2 = 0.30000000000000004 (floating point error)
    // BigDecimal: exact. Banks use it. You use it.
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    // false = product is active/visible
    // true  = product is deleted (soft delete — never hard delete in prod)
    // Why soft delete? You need order history. If a product is hard deleted
    // and an old order references it — your DB has an orphaned reference.
    private Boolean deleted = false;

    @CreationTimestamp
    // Hibernate sets this automatically when row is inserted
    // You never set this manually
    private LocalDateTime createdAt;

    @UpdateTimestamp
    // Hibernate updates this automatically on every save
    private LocalDateTime updatedAt;
}