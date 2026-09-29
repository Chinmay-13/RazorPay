package com.portfolio.advisor.rebalancing;

import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.model.TradeSide;
import com.portfolio.advisor.tax.TaxImpact;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Output of a {@link RebalancingStrategy}. Sells are listed before buys so executing in order
 * never needs more cash than the portfolio has.
 */
public record RebalancePlan(String portfolioId, String strategy, LocalDate asOf, List<Trade> trades,
                            double portfolioValue, Map<AssetClass, Double> currentWeights,
                            Map<AssetClass, Double> effectiveTarget, Map<AssetClass, Double> projectedWeights,
                            TaxImpact taxImpact, List<String> notes) {

    public RebalancePlan {
        trades = List.copyOf(trades);
        currentWeights = Map.copyOf(currentWeights);
        effectiveTarget = Map.copyOf(effectiveTarget);
        projectedWeights = Map.copyOf(projectedWeights);
        notes = List.copyOf(notes);
    }

    public boolean isEmpty() { return trades.isEmpty(); }

    public double totalTransactionCost() {
        return trades.stream().mapToDouble(Trade::transactionCost).sum();
    }

    public double totalEstimatedTax() { return taxImpact.estimatedTax(); }

    /** Transaction costs plus tax: the all-in price of executing this plan. */
    public double totalFriction() { return totalTransactionCost() + totalEstimatedTax(); }

    public double turnover() {
        return portfolioValue == 0 ? 0
                : trades.stream().mapToDouble(Trade::grossValue).sum() / portfolioValue;
    }

    public List<Trade> sells() { return trades.stream().filter(t -> t.side() == TradeSide.SELL).toList(); }
    public List<Trade> buys() { return trades.stream().filter(t -> t.side() == TradeSide.BUY).toList(); }
}
