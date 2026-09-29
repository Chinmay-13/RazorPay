package com.portfolio.advisor.allocation;

import com.portfolio.advisor.model.AssetClass;

import java.util.Map;

/** Point-in-time valuation of a portfolio, by asset class and by symbol. */
public record AllocationSnapshot(double totalValue, Map<AssetClass, Double> classValues,
                                 Map<String, Double> assetValues) {

    public AllocationSnapshot {
        classValues = Map.copyOf(classValues);
        assetValues = Map.copyOf(assetValues);
    }

    public double value(AssetClass assetClass) { return classValues.getOrDefault(assetClass, 0.0); }

    public double weight(AssetClass assetClass) {
        return totalValue == 0 ? 0 : value(assetClass) / totalValue;
    }

    public double assetValue(String symbol) { return assetValues.getOrDefault(symbol, 0.0); }

    public double assetWeight(String symbol) {
        return totalValue == 0 ? 0 : assetValue(symbol) / totalValue;
    }
}
