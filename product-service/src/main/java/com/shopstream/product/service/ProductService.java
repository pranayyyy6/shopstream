package com.shopstream.product.service;

import com.shopstream.product.dto.ProductRequest;
import com.shopstream.product.dto.ProductResponse;
import com.shopstream.product.exception.ProductNotFoundException;
import com.shopstream.product.model.Product;
import com.shopstream.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    // @CacheEvict on create — why?
    // "products::all" cache is now stale (new product added)
    // Evict it so next GET /products fetches fresh from DB
    @CacheEvict(value = "products", key = "'all'")
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .deleted(false)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    // @Cacheable — first call hits DB and stores result in Redis
    // key = "products::all"
    // Every subsequent call returns from Redis — zero DB query
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        log.info("CACHE MISS — fetching all products from DB");
        return productRepository.findByDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // key = "products::uuid-value"
    // Each product cached individually by its ID
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        log.info("CACHE MISS — fetching product {} from DB", id);
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'category::' + #category")
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndDeletedFalse(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        // Search not cached — results too dynamic
        return productRepository.searchByName(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    // @CachePut — updates cache with new value after DB update
    // @CacheEvict — invalidates the "all" list (it's now stale)
    // Both happen together using @Caching
    @Caching(
            put = @CachePut(value = "products", key = "#id"),
            evict = @CacheEvict(value = "products", key = "'all'")
    )
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    // Evict both the individual product cache AND the all-products cache
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public void deleteProduct(UUID id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Product soft-deleted: {}", id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}