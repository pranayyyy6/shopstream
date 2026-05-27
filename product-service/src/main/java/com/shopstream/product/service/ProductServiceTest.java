package com.shopstream.product.service;

import com.shopstream.product.dto.ProductRequest;
import com.shopstream.product.dto.ProductResponse;
import com.shopstream.product.exception.ProductNotFoundException;
import com.shopstream.product.model.Product;
import com.shopstream.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    /*
     * @InjectMocks creates ProductService and injects mocks.
     * BUT @Cacheable is a Spring AOP proxy — it only works
     * inside a Spring context. In pure Mockito tests,
     * @Cacheable annotations are completely ignored.
     * This is actually GOOD for unit tests — we test
     * pure business logic without cache interference.
     */
    @InjectMocks
    private ProductService productService;

    private ProductRequest validRequest;
    private Product savedProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        validRequest = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .description("Latest Apple flagship")
                .price(BigDecimal.valueOf(134900))
                .stockQuantity(50)
                .category("Electronics")
                .build();

        savedProduct = Product.builder()
                .id(productId)
                .name("iPhone 15 Pro")
                .description("Latest Apple flagship")
                .price(BigDecimal.valueOf(134900))
                .stockQuantity(50)
                .category("Electronics")
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should create product and return response")
    void shouldCreateProduct() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getPrice()).isEqualByComparingTo("134900");
        assertThat(response.getId()).isNotNull();
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should return all non-deleted products")
    void shouldGetAllProducts() {
        when(productRepository.findByDeletedFalse())
                .thenReturn(List.of(savedProduct));

        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("iPhone 15 Pro");
        verify(productRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("Should return product by ID")
    void shouldGetProductById() {
        when(productRepository.findByIdAndDeletedFalse(productId))
                .thenReturn(Optional.of(savedProduct));

        ProductResponse response = productService.getProductById(productId);

        assertThat(response.getId()).isEqualTo(productId);
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException for missing product")
    void shouldThrowWhenProductNotFound() {
        UUID randomId = UUID.randomUUID();
        when(productRepository.findByIdAndDeletedFalse(randomId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(randomId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }

    @Test
    @DisplayName("Should soft delete — never hard delete")
    void shouldSoftDeleteProduct() {
        when(productRepository.findByIdAndDeletedFalse(productId))
                .thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        productService.deleteProduct(productId);

        assertThat(savedProduct.getDeleted()).isTrue();
        verify(productRepository).save(savedProduct);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should update product fields")
    void shouldUpdateProduct() {
        when(productRepository.findByIdAndDeletedFalse(productId))
                .thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("iPhone 15 Pro Max")
                .description("Updated")
                .price(BigDecimal.valueOf(159900))
                .stockQuantity(30)
                .category("Electronics")
                .build();

        productService.updateProduct(productId, updateRequest);

        assertThat(savedProduct.getName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(savedProduct.getPrice())
                .isEqualByComparingTo("159900");
    }

    @Test
    @DisplayName("Price must be BigDecimal — not double or float")
    void priceShouldBeBigDecimal() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(validRequest);

        assertThat(response.getPrice()).isInstanceOf(BigDecimal.class);
    }
}