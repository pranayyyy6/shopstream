package com.shopstream.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * ManyToOne — many items belong to one order.
     * FetchType.LAZY — don't load the parent Order
     * when you fetch an OrderItem. Load on demand.
     * WHY LAZY? If you fetch 1000 order items, EAGER
     * would execute 1000 extra queries to load each Order.
     * This is the famous N+1 problem. Lazy prevents it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private UUID productId;    // reference to product — no FK across services

    @Column(nullable = false)
    private String productName; // snapshot — copied at order time
    /*
     * WHY copy productName here?
     * Product names can change. Your order from 2023
     * must always show what you actually ordered — not
     * the current product name. Snapshot the data.
     */

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;  // snapshot — price at time of order

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;   // unitPrice × quantity
}