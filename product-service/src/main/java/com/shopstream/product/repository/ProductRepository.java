package com.shopstream.product.repository;

import com.shopstream.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
// JpaRepository gives you save(), findById(), findAll(), delete()
// for FREE — zero SQL written for basic operations
// <Product, UUID> = entity type, primary key type
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Spring Data JPA reads the method name and generates SQL automatically
    // findBy + Deleted + False = WHERE deleted = false
    List<Product> findByDeletedFalse();

    // WHERE deleted = false AND id = ?
    Optional<Product> findByIdAndDeletedFalse(UUID id);

    // WHERE deleted = false AND category = ?
    List<Product> findByCategoryAndDeletedFalse(String category);

    // Custom JPQL query — search by name containing a keyword
    // LOWER() makes it case-insensitive
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND p.deleted = false")
    List<Product> searchByName(String keyword);
}