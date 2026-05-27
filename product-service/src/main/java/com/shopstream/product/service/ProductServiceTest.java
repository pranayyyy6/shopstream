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

/*
 * Unit tests — no Spring context, no DB, no Redis.
 * We mock the repository and test ONLY the service logic.
 * Fast — runs in milliseconds.
 * Tests one thing at a time.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest validRequest;
    private Product savedProduct;

    @BeforeEach
    void setUp() {
        validRequest = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .description("Latest Apple flagship")
                .price(BigDecimal.valueOf(134900))
                .stockQuantity(50)
                .category("Electronics")
                .build();

        savedProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("iPhone 15 Pro")
                .description("Latest Apple flagship")
                .price(BigDecimal.valueOf(134900))
                .stockQuantity(50)
                .category("Electronics")
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProduct() {
        // given
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        // when
        ProductResponse response = productService.createProduct(validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getPrice()).isEqualByComparingTo("134900");
        assertThat(response.getId()).isNotNull();

        // verify repository was called exactly once
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should return all non-deleted products")
    void shouldGetAllProducts() {
        // given
        when(productRepository.findByDeletedFalse())
                .thenReturn(List.of(savedProduct));

        // when
        List<ProductResponse> products = productService.getAllProducts();

        // then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("iPhone 15 Pro");
        verify(productRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product not found")
    void shouldThrowWhenProductNotFound() {
        // given
        UUID randomId = UUID.randomUUID();
        when(productRepository.findByIdAndDeletedFalse(randomId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductById(randomId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(randomId.toString());

        // Repository was called — exception came from service logic
        verify(productRepository).findByIdAndDeletedFalse(randomId);
    }

    @Test
    @DisplayName("Should soft delete product — not hard delete")
    void shouldSoftDeleteProduct() {
        // given
        UUID productId = savedProduct.getId();
        when(productRepository.findByIdAndDeletedFalse(productId))
                .thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        // when
        productService.deleteProduct(productId);

        // then — product is marked deleted, not removed
        assertThat(savedProduct.getDeleted()).isTrue();
        verify(productRepository).save(savedProduct);
        // Hard delete should NEVER be called
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should update product fields correctly")
    void shouldUpdateProduct() {
        // given
        UUID productId = savedProduct.getId();
        ProductRequest updateRequest = ProductRequest.builder()
                .name("iPhone 15 Pro Max")
                .description("Updated description")
                .price(BigDecimal.valueOf(159900))
                .stockQuantity(30)
                .category("Electronics")
                .build();

        when(productRepository.findByIdAndDeletedFalse(productId))
                .thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        // when
        productService.updateProduct(productId, updateRequest);

        // then — verify the product fields were updated
        assertThat(savedProduct.getName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(savedProduct.getPrice())
                .isEqualByComparingTo("159900");
        verify(productRepository).save(savedProduct);
    }

    @Test
    @DisplayName("Should use BigDecimal for price — not double")
    void shouldUseBigDecimalForPrice() {
        // This test documents an architectural decision —
        // BigDecimal prevents floating point errors in financial data
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(validRequest);

        assertThat(response.getPrice())
                .isInstanceOf(BigDecimal.class);
    }
}