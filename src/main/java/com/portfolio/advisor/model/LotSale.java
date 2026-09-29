package com.portfolio.advisor.model;

/** A (planned or executed) sale of part of a tax lot. */
public record LotSale(TaxLot lot, double quantity, double salePrice, boolean longTerm) {

    public double proceeds() { return quantity * salePrice; }
    public double costBasis() { return quantity * lot.costPerUnit(); }
    public double gain() { return proceeds() - costBasis(); }
}
