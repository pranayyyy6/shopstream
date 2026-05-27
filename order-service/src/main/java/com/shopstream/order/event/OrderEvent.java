package com.shopstream.order.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/*
 * This is the message that goes onto the Kafka topic.
 * Every service that consumes this event gets this object.
 * Keep it STABLE — changing field names breaks consumers.
 * In production: use Avro schema + Schema Registry
 * to enforce backward compatibility.
 */
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