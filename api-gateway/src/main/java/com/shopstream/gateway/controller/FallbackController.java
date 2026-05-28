package com.shopstream.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /*
     * Circuit breaker opens after 5 consecutive failures.
     * Instead of Gateway hanging for 30 seconds,
     * it immediately returns this fallback response.
     * After 10 seconds it half-opens to test recovery.
     * This prevents cascade failures across services.
     */
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productsFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 503,
                        "message", "Product Service is temporarily unavailable. " +
                                "Please try again in a moment.",
                        "fallback", true
                ));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 503,
                        "message", "Order Service is temporarily unavailable.",
                        "fallback", true
                ));
    }
}