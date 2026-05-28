package com.shopstream.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstream.inventory.event.OrderEvent;
import com.shopstream.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    /*
     * Real Inventory Service listening to order.created.
     * This replaces the simulated consumer in Order Service.
     * Now stock is actually tracked in its own database.
     *
     * groupId = "inventory-service-group"
     * Different from order-service's "inventory-group"
     * — ensures this gets its own copy of each message
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service-group"
    )
    public void onOrderCreated(String eventJson) {
        try {
            OrderEvent event = objectMapper
                    .readValue(eventJson, OrderEvent.class);

            log.info("=== INVENTORY SERVICE — Order received: {} ===",
                    event.getOrderId());

            inventoryService.processOrderCreated(event);

        } catch (Exception e) {
            log.error("Failed to process order event in inventory", e);
            // In production: send to Dead Letter Topic
            // so the message isn't lost
        }
    }
}