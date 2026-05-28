package com.shopstream.inventory.repository;

import com.shopstream.inventory.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StockReservationRepository
        extends JpaRepository<StockReservation, UUID> {
    List<StockReservation> findByOrderId(UUID orderId);
}