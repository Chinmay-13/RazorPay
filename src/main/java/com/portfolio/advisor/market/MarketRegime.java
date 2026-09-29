package com.portfolio.advisor.market;

/** Hidden market state; gives simulated prices persistent trends a model can learn. */
public enum MarketRegime {
    BULL(0.18),
    SIDEWAYS(0.0),
    BEAR(-0.22);

    private final double annualDriftShift;

    MarketRegime(double annualDriftShift) { this.annualDriftShift = annualDriftShift; }

    public double annualDriftShift() { return annualDriftShift; }
}
