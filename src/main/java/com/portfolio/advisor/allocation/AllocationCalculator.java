package com.portfolio.advisor.allocation;

import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.Portfolio;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class AllocationCalculator {

    public AllocationSnapshot calculate(Portfolio portfolio, Map<String, Double> prices) {
        Map<AssetClass, Double> classValues = new EnumMap<>(AssetClass.class);
        for (AssetClass ac : AssetClass.values()) classValues.put(ac, 0.0);
        Map<String, Double> assetValues = new HashMap<>();

        for (Holding holding : portfolio.holdings()) {
            String symbol = holding.asset().symbol();
            Double price = prices.get(symbol);
            if (price == null) throw new IllegalArgumentException("no price for " + symbol);
            double value = holding.marketValue(price);
            assetValues.put(symbol, value);
            classValues.merge(holding.asset().assetClass(), value, Double::sum);
        }
        classValues.merge(AssetClass.CASH, portfolio.cash(), Double::sum);

        double total = classValues.values().stream().mapToDouble(Double::doubleValue).sum();
        return new AllocationSnapshot(total, classValues, assetValues);
    }
}
