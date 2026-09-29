package com.portfolio.advisor.tax;

import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.TaxLot;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** First-in-first-out: the broker default. Simple but often realises the largest gains. */
public final class FifoLotSelection implements LotSelectionStrategy {

    @Override
    public List<TaxLot> order(Holding holding, double price, LocalDate asOf, InvestorProfile profile) {
        return holding.lots().stream()
                .sorted(Comparator.comparing(TaxLot::acquiredOn).thenComparingLong(TaxLot::id))
                .toList();
    }
}
