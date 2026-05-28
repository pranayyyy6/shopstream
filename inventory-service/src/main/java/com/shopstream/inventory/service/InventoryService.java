package com.shopstream.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopstream.inventory.event.OrderEvent;
import com.shopstream.inventory.event.StockEvent;
import com.shopstream.inventory.model.*;
import com.shopstream.inventory.repository.InventoryRepository;
import com.shopstream.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /*
     * Process order.created event.
     * For each item in the order:
     * 1. Lock the inventory row (prevent overselling)
     * 2. Check if sufficient stock
     * 3. If yes → reserve stock, publish stock.reserved
     * 4. If no  → publish stock.insufficient
     *             (Order Service will cancel the order)
     */
    @Transactional
    public void processOrderCreated(OrderEvent event) {
        log.info("Processing stock reservation for order: {}",
                event.getOrderId());

        for (OrderEvent.OrderItemEvent item : event.getItems()) {
            processItem(event.getOrderId(), item);
        }
    }

    private void processItem(UUID orderId,
                             OrderEvent.OrderItemEvent item) {
        // Lock the row — prevents concurrent overselling
        Inventory inventory = inventoryRepository
                .findByProductIdWithLock(item.getProductId())
                .orElseGet(() -> {
                    // Product not in inventory yet — create it
                    log.warn("Product {} not found in inventory — creating",
                            item.getProductId());
                    return createInventory(item);
                });

        if (inventory.getAvailableQuantity() >= item.getQuantity()) {
            // Sufficient stock — reserve it
            inventory.setAvailableQuantity(
                    inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() + item.getQuantity());

            // Update stock status
            updateStockStatus(inventory);
            inventoryRepository.save(inventory);

            // Record the reservation for potential rollback
            StockReservation reservation = StockReservation.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .build();
            reservationRepository.save(reservation);

            log.info("Stock reserved — product: {}, quantity: {}, remaining: {}",
                    item.getProductName(), item.getQuantity(),
                    inventory.getAvailableQuantity());

            // Publish stock reserved event
            publishStockEvent(orderId, item, inventory,
                    "STOCK_RESERVED");

            // Warn if stock is low
            if (inventory.getStatus() == StockStatus.LOW_STOCK) {
                publishStockEvent(orderId, item, inventory,
                        "LOW_STOCK_ALERT");
                log.warn("LOW STOCK ALERT — product: {}, remaining: {}",
                        item.getProductName(),
                        inventory.getAvailableQuantity());
            }

        } else {
            // Insufficient stock — publish event for Order Service
            log.warn("Insufficient stock — product: {}, requested: {}, available: {}",
                    item.getProductName(), item.getQuantity(),
                    inventory.getAvailableQuantity());

            publishStockEvent(orderId, item, inventory,
                    "STOCK_INSUFFICIENT");
        }
    }

    private void updateStockStatus(Inventory inventory) {
        if (inventory.getAvailableQuantity() == 0) {
            inventory.setStatus(StockStatus.OUT_OF_STOCK);
        } else if (inventory.getAvailableQuantity()
                <= inventory.getLowStockThreshold()) {
            inventory.setStatus(StockStatus.LOW_STOCK);
        } else {
            inventory.setStatus(StockStatus.IN_STOCK);
        }
    }

    private Inventory createInventory(OrderEvent.OrderItemEvent item) {
        return inventoryRepository.save(Inventory.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .availableQuantity(100) // default stock
                .reservedQuantity(0)
                .lowStockThreshold(10)
                .status(StockStatus.IN_STOCK)
                .build());
    }

    private void publishStockEvent(UUID orderId,
                                   OrderEvent.OrderItemEvent item,
                                   Inventory inventory,
                                   String eventType) {
        try {
            StockEvent event = StockEvent.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .eventType(eventType)
                    .quantity(item.getQuantity())
                    .remainingStock(inventory.getAvailableQuantity())
                    .occurredAt(LocalDateTime.now())
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("stock.events",
                    orderId.toString(), eventJson);

        } catch (Exception e) {
            log.error("Failed to publish stock event", e);
        }
    }
}