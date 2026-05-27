package com.shopstream.order.model;

/*
 * Every possible state an order can be in.
 * This is a state machine — orders flow in one direction.
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 *         ↘ CANCELLED
 * In interviews: "I modelled order lifecycle as a finite state machine"
 */
public enum OrderStatus {
    PENDING,      // just created, payment not confirmed
    CONFIRMED,    // payment successful, being prepared
    SHIPPED,      // dispatched from warehouse
    DELIVERED,    // received by customer
    CANCELLED     // cancelled at any stage before shipping
}