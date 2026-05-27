package com.shopstream.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstream.product.dto.ProductRequest;
import com.shopstream.product.dto.ProductResponse;
import com.shopstream.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)  // disables security filters in tests
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("POST /api/products should return 201 Created")
    void shouldReturn201OnCreate() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .description("Latest flagship")
                .price(BigDecimal.valueOf(134900))
                .stockQuantity(50)
                .category("Electronics")
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .name("iPhone 15 Pro")
                .price(BigDecimal.valueOf(134900))
                .build();

        when(productService.createProduct(any())).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST with blank name should return 400")
    void shouldReturn400OnValidationFailure() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("")
                .price(BigDecimal.valueOf(-10))
                .stockQuantity(-1)
                .category("")
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products should return 200 with list")
    void shouldReturnAllProducts() throws Exception {
        when(productService.getAllProducts())
                .thenReturn(List.of(
                        ProductResponse.builder()
                                .id(UUID.randomUUID())
                                .name("iPhone 15 Pro")
                                .build()
                ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("iPhone 15 Pro"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} should return 204")
    void shouldReturn204OnDelete() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());
    }
}