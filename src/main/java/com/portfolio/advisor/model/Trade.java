package com.portfolio.advisor.model;

import java.util.List;

/**
 * A proposed or executed order. For sells, {@code lotSales} records exactly which
 * lots are disposed of, so the tax estimate and the execution always agree.
 */
public record Trade(TradeSide side, Asset asset, double quantity, double price,
                    double transactionCost, double estimatedTax,
                    List<LotSale> lotSales, String rationale) {

    public Trade {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        lotSales = List.copyOf(lotSales);
    }

    public double grossValue() { return quantity * price; }

    /** Cash change caused by the trade (tax is settled later, so it is excluded). */
    public double netCashFlow() {
        return side == TradeSide.BUY ? -(grossValue() + transactionCost) : grossValue() - transactionCost;
    }

    public double realizedGain() {
        return lotSales.stream().mapToDouble(LotSale::gain).sum();
    }
}
