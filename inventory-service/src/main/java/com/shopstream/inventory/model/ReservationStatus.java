package com.shopstream.inventory.model;

public enum ReservationStatus {
    RESERVED,   // stock held for this order
    CONFIRMED,  // payment succeeded — stock permanently deducted
    RELEASED    // payment failed — stock released back
}