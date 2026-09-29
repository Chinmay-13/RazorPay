package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.PricePoint;

import java.util.List;

/**
 * Composite predictor: confidence-weighted average of its members. Confidence is the
 * mean member confidence, scaled down when the members disagree on direction.
 */
public final class EnsemblePredictor implements TrendPredictor {

    private final List<TrendPredictor> members;

    public EnsemblePredictor(List<TrendPredictor> members) {
        if (members.isEmpty()) throw new IllegalArgumentException("ensemble needs at least one model");
        this.members = List.copyOf(members);
    }

    @Override
    public String name() { return "Ensemble"; }

    @Override
    public TrendForecast predict(String symbol, List<PricePoint> history, int horizonDays) {
        List<TrendForecast> forecasts = members.stream().map(m -> m.predict(symbol, history, horizonDays)).toList();

        double weightSum = 0, weighted = 0, confidenceSum = 0;
        for (TrendForecast f : forecasts) {
            double w = f.confidence() + 1e-6;
            weighted += w * f.expectedReturn();
            weightSum += w;
            confidenceSum += f.confidence();
        }
        double expected = weighted / weightSum;
        TrendDirection direction = TrendDirection.fromExpectedReturn(expected);
        long agreeing = forecasts.stream().filter(f -> f.direction() == direction).count();
        double agreement = (double) agreeing / forecasts.size();
        double confidence = (confidenceSum / forecasts.size()) * agreement;
        return new TrendForecast(symbol, direction, expected, confidence, horizonDays, name());
    }
}
