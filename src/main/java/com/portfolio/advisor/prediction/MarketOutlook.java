package com.portfolio.advisor.prediction;

import com.portfolio.advisor.model.AssetClass;

import java.util.Map;
import java.util.Optional;

/** Forecasts for every symbol plus an equal-weighted score per asset class. */
public record MarketOutlook(Map<String, TrendForecast> bySymbol, Map<AssetClass, Double> classScores) {

    public MarketOutlook {
        bySymbol = Map.copyOf(bySymbol);
        classScores = Map.copyOf(classScores);
    }

    public Optional<TrendForecast> forecast(String symbol) { return Optional.ofNullable(bySymbol.get(symbol)); }

    public double score(String symbol) { return forecast(symbol).map(TrendForecast::score).orElse(0.0); }

    public double classScore(AssetClass assetClass) { return classScores.getOrDefault(assetClass, 0.0); }
}
