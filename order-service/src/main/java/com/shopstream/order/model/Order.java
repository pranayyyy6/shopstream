package com.shopstream.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")   // "order" is a reserved SQL keyword — always use "orders"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerName;

    /*
     * OrderStatus as enum stored as String in DB.
     * WHY STRING not ORDINAL?
     * ORDINAL stores 0,1,2... If you reorder the enum,
     * all existing data becomes wrong silently.
     * STRING stores "PENDING","CONFIRMED" — safe forever.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /*
     * CascadeType.ALL — when Order is saved, all its OrderItems
     * are saved automatically. No manual save for each item.
     * orphanRemoval — if you remove an item from the list,
     * it gets deleted from DB automatically.
     */
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column
    private String shippingAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}