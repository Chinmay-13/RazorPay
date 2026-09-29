package com.portfolio.advisor.prediction;

/**
 * @param expectedReturn predicted simple return over {@code horizonDays}
 * @param confidence     0..1, how much the model trusts itself (from out-of-sample accuracy)
 */
public record TrendForecast(String symbol, TrendDirection direction, double expectedReturn,
                            double confidence, int horizonDays, String model) {

    public static TrendForecast neutral(String symbol, int horizonDays, String model) {
        return new TrendForecast(symbol, TrendDirection.NEUTRAL, 0, 0, horizonDays, model);
    }

    /** Confidence-weighted expected return; the single number the rebalancer consumes. */
    public double score() { return expectedReturn * confidence; }
}
