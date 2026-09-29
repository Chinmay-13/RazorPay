package com.portfolio.advisor.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

/** A single purchase of an asset. Tax is computed per lot, so lots are never merged. */
public final class TaxLot {

    public static final int LONG_TERM_HOLDING_DAYS = 365;
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final long id;
    private final double costPerUnit;
    private final LocalDate acquiredOn;
    private double quantity;

    public TaxLot(double quantity, double costPerUnit, LocalDate acquiredOn) {
        if (quantity <= 0 || costPerUnit <= 0) {
            throw new IllegalArgumentException("quantity and cost must be positive");
        }
        this.id = SEQUENCE.incrementAndGet();
        this.quantity = quantity;
        this.costPerUnit = costPerUnit;
        this.acquiredOn = acquiredOn;
    }

    void reduce(double qty) {
        if (qty <= 0 || qty > quantity + 1e-9) {
            throw new IllegalArgumentException("cannot sell " + qty + " from lot of " + quantity);
        }
        quantity = Math.max(0, quantity - qty);
    }

    public boolean isLongTerm(LocalDate asOf) {
        return ChronoUnit.DAYS.between(acquiredOn, asOf) > LONG_TERM_HOLDING_DAYS;
    }

    public double unrealizedGain(double price) { return (price - costPerUnit) * quantity; }

    public long id() { return id; }
    public double quantity() { return quantity; }
    public double costPerUnit() { return costPerUnit; }
    public LocalDate acquiredOn() { return acquiredOn; }
}
