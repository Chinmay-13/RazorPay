package com.portfolio.advisor.rebalancing;

import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.prediction.MarketOutlook;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Everything a strategy needs to plan, gathered once by the service (parameter object). */
public record RebalanceContext(Portfolio portfolio, InvestorProfile profile, TargetAllocation target,
                               AllocationSnapshot snapshot, DriftReport drift, MarketOutlook outlook,
                               Map<String, Double> prices, List<Asset> universe, LocalDate asOf) {

    public double price(String symbol) {
        Double price = prices.get(symbol);
        if (price == null) throw new IllegalArgumentException("no price for " + symbol);
        return price;
    }

    public List<Asset> universeOf(AssetClass assetClass) {
        return universe.stream().filter(a -> a.assetClass() == assetClass).toList();
    }
}
