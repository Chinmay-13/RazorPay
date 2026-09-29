package com.portfolio.advisor.tax;

import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.TaxLot;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Sells the lots with the lowest tax per unit first: short-term losses (most valuable),
 * then long-term losses, then long-term gains, and short-term gains last.
 */
public final class TaxEfficientLotSelection implements LotSelectionStrategy {

    private final TaxCalculator taxCalculator;

    public TaxEfficientLotSelection(TaxCalculator taxCalculator) {
        this.taxCalculator = taxCalculator;
    }

    @Override
    public List<TaxLot> order(Holding holding, double price, LocalDate asOf, InvestorProfile profile) {
        Comparator<TaxLot> byTax = Comparator.comparingDouble(
                lot -> taxCalculator.taxPerUnit(price, lot.costPerUnit(), lot.isLongTerm(asOf), profile));
        return holding.lots().stream().sorted(byTax.thenComparingLong(TaxLot::id)).toList();
    }
}
