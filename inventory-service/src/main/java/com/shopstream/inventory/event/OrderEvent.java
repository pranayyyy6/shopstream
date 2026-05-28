package com.shopstream.inventory.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Mirror of Order Service's OrderEvent — same structure
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    private UUID orderId;
    private String customerEmail;
    private String customerName;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private LocalDateTime occurredAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent {
        private UUID productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}