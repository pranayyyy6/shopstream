package com.shopstream.product.exception;

// Extends RuntimeException — unchecked, no forced try-catch
// WHY custom exception instead of generic RuntimeException?
// You can map it to a specific HTTP 404 status in the handler below.
// Generic exception would give a 500 (server error) which is wrong —
// "product not found" is a CLIENT error (404), not a server error (500).
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String id) {
        super("Product not found with id: " + id);
    }
}