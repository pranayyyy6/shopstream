package com.shopstream.inventory.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Event published BY Inventory Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEvent {
    private UUID orderId;
    private UUID productId;
    private String productName;
    private String eventType;  // RESERVED, INSUFFICIENT, LOW_STOCK
    private Integer quantity;
    private Integer remainingStock;
    private LocalDateTime occurredAt;
}