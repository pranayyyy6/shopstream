package com.shopstream.inventory.model;

public enum StockStatus {
    IN_STOCK,      // available > lowStockThreshold
    LOW_STOCK,     // available > 0 but <= lowStockThreshold
    OUT_OF_STOCK   // available = 0
}