package com.shopstream.order.service;

import com.shopstream.order.dto.*;
import com.shopstream.order.event.OrderEvent;
import com.shopstream.order.exception.OrderNotFoundException;
import com.shopstream.order.model.*;
import com.shopstream.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    /*
     * KafkaTemplate<String, String>
     * Key = String (we use orderId as key)
     * Value = String (JSON serialized OrderEvent)
     *
     * WHY String key?
     * Kafka uses the key to determine which partition
     * a message goes to. Same key = same partition =
     * guaranteed ordering for that key.
     * All events for order "abc-123" go to the same
     * partition in the same order. Critical for
     * financial event streams.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerEmail());

        // Calculate total amount from items
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build Order entity
        Order order = Order.builder()
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        // Build OrderItems and link to Order
        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> OrderItem.builder()
                        .order(order)
                        .productId(itemReq.getProductId())
                        .productName(itemReq.getProductName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .subtotal(itemReq.getUnitPrice()
                                .multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        log.info("Order saved with id: {}", savedOrder.getId());

        // Publish event to Kafka AFTER successful DB save
        // WHY after? If DB save fails, we don't want a Kafka
        // event for an order that doesn't exist.
        publishOrderEvent(savedOrder, "order.created");

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String email) {
        return orderRepository.findByCustomerEmail(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        publishOrderEvent(updated, "order.updated");

        return mapToResponse(updated);
    }

    private void publishOrderEvent(Order order, String topic) {
        try {
            // Build the event payload
            OrderEvent event = OrderEvent.builder()
                    .orderId(order.getId())
                    .customerEmail(order.getCustomerEmail())
                    .customerName(order.getCustomerName())
                    .status(order.getStatus().name())
                    .totalAmount(order.getTotalAmount())
                    .occurredAt(LocalDateTime.now())
                    .items(order.getItems().stream()
                            .map(item -> OrderEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            // Serialize to JSON string
            String eventJson = objectMapper.writeValueAsString(event);

            /*
             * Send to Kafka.
             * topic = "order.created" or "order.updated"
             * key = orderId.toString() — ensures all events
             *       for the same order go to the same partition
             * value = JSON string of the event
             */
            kafkaTemplate.send(topic, order.getId().toString(), eventJson);

            log.info("Published event to topic '{}' for order: {}",
                    topic, order.getId());

        } catch (Exception e) {
            // Log but don't fail the order — event publishing
            // failure should not roll back a successful order.
            // In production: use Outbox pattern for guaranteed delivery.
            log.error("Failed to publish event for order: {}",
                    order.getId(), e);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerEmail(order.getCustomerEmail())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}