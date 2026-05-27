package com.shopstream.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstream.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;

    /*
     * @KafkaListener — Spring Kafka polls this topic continuously.
     * groupId = "inventory-group" — consumer group.
     *
     * WHY consumer groups matter:
     * If you have 3 instances of Inventory Service all in
     * "inventory-group", Kafka splits the 3 partitions between them.
     * Each message is processed by exactly ONE instance.
     * Scale consumers = scale throughput. Linear scaling.
     *
     * If Notification Service also listens with groupId="notification-group",
     * it gets its OWN copy of every message — both groups get all messages.
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-group"
    )
    public void handleOrderCreated(String eventJson) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);

            log.info("=== INVENTORY SERVICE RECEIVED ORDER EVENT ===");
            log.info("Order ID: {}", event.getOrderId());
            log.info("Customer: {}", event.getCustomerName());
            log.info("Total: {}", event.getTotalAmount());
            log.info("Items to reserve:");

            event.getItems().forEach(item ->
                    log.info("  - {} x{} (productId: {})",
                            item.getProductName(),
                            item.getQuantity(),
                            item.getProductId())
            );

            // In real Inventory Service: reduce stock in inventory DB here
            log.info("Stock reserved for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order event", e);
        }
    }

    @KafkaListener(
            topics = "order.created",
            groupId = "notification-group"
    )
    public void handleOrderNotification(String eventJson) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);

            log.info("=== NOTIFICATION SERVICE RECEIVED ORDER EVENT ===");
            log.info("Sending confirmation email to: {}", event.getCustomerEmail());
            log.info("Order {} confirmed. Total: {}",
                    event.getOrderId(), event.getTotalAmount());

            // In real Notification Service: send email via SendGrid/SES here

        } catch (Exception e) {
            log.error("Failed to process notification event", e);
        }
    }
}