package com.shopstream.inventory.repository;

import com.shopstream.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    /*
     * PESSIMISTIC_WRITE lock — critical for stock operations.
     * WHY? Without a lock, two concurrent orders for the
     * last item could both succeed — overselling.
     *
     * Thread 1 reads stock = 1
     * Thread 2 reads stock = 1
     * Thread 1 reduces to 0, saves
     * Thread 2 reduces to 0, saves
     * Result: -1 stock. Disaster.
     *
     * With PESSIMISTIC_WRITE:
     * Thread 1 locks the row
     * Thread 2 waits
     * Thread 1 reduces to 0, saves, releases lock
     * Thread 2 reads stock = 0, returns insufficient
     * Result: correct behavior
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(UUID productId);
}