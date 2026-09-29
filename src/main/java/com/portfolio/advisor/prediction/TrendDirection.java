package com.portfolio.advisor.prediction;

public enum TrendDirection {
    BULLISH,
    NEUTRAL,
    BEARISH;

    public static final double NEUTRAL_BAND = 0.005;

    public static TrendDirection fromExpectedReturn(double expectedReturn) {
        if (expectedReturn > NEUTRAL_BAND) return BULLISH;
        if (expectedReturn < -NEUTRAL_BAND) return BEARISH;
        return NEUTRAL;
    }
}
