package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.MarketDataService;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs the configured predictor across the whole universe and aggregates per asset class. */
public final class TrendAnalysisService {

    private final MarketDataService market;
    private final TrendPredictor predictor;
    private final int horizonDays;

    public TrendAnalysisService(MarketDataService market, TrendPredictor predictor, int horizonDays) {
        this.market = market;
        this.predictor = predictor;
        this.horizonDays = horizonDays;
    }

    public MarketOutlook outlook() {
        Map<String, TrendForecast> bySymbol = new HashMap<>();
        for (Asset asset : market.assets()) {
            bySymbol.put(asset.symbol(), predictor.predict(asset.symbol(), market.history(asset.symbol()), horizonDays));
        }
        Map<AssetClass, Double> classScores = new EnumMap<>(AssetClass.class);
        for (AssetClass ac : List.of(AssetClass.STOCK, AssetClass.BOND)) {
            classScores.put(ac, market.assetsOf(ac).stream()
                    .mapToDouble(a -> bySymbol.get(a.symbol()).score()).average().orElse(0));
        }
        classScores.put(AssetClass.CASH, 0.0);
        return new MarketOutlook(bySymbol, classScores);
    }

    public TrendPredictor predictor() { return predictor; }
    public int horizonDays() { return horizonDays; }
}
