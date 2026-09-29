package com.portfolio.advisor.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All lots of one asset inside a portfolio. */
public final class Holding {

    private static final double EPS = 1e-9;

    private final Asset asset;
    private final List<TaxLot> lots = new ArrayList<>();

    public Holding(Asset asset) {
        this.asset = asset;
    }

    public void addLot(double quantity, double price, LocalDate date) {
        lots.add(new TaxLot(quantity, price, date));
    }

    public void applySale(List<LotSale> sales) {
        for (LotSale sale : sales) {
            if (!lots.contains(sale.lot())) {
                throw new IllegalStateException("lot " + sale.lot().id() + " does not belong to " + asset.symbol());
            }
            sale.lot().reduce(sale.quantity());
        }
        lots.removeIf(l -> l.quantity() <= EPS);
    }

    public double quantity() {
        return lots.stream().mapToDouble(TaxLot::quantity).sum();
    }

    public double marketValue(double price) { return quantity() * price; }

    public double costBasis() {
        return lots.stream().mapToDouble(l -> l.quantity() * l.costPerUnit()).sum();
    }

    public double unrealizedGain(double price) { return marketValue(price) - costBasis(); }

    public boolean isEmpty() { return lots.isEmpty(); }
    public Asset asset() { return asset; }
    public List<TaxLot> lots() { return Collections.unmodifiableList(lots); }
}
