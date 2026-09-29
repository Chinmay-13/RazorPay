package com.portfolio.advisor.tax;

import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.TaxLot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Decides which tax lots are sold first. Planning only; nothing is mutated. */
public interface LotSelectionStrategy {

    List<TaxLot> order(Holding holding, double price, LocalDate asOf, InvestorProfile profile);

    default List<LotSale> select(Holding holding, double quantity, double price, LocalDate asOf,
                                 InvestorProfile profile) {
        List<LotSale> sales = new ArrayList<>();
        double remaining = quantity;
        for (TaxLot lot : order(holding, price, asOf, profile)) {
            if (remaining <= 1e-9) break;
            double qty = Math.min(remaining, lot.quantity());
            sales.add(new LotSale(lot, qty, price, lot.isLongTerm(asOf)));
            remaining -= qty;
        }
        if (remaining > 1e-9) {
            throw new IllegalArgumentException("cannot sell " + quantity + " of " + holding.asset().symbol());
        }
        return sales;
    }
}
