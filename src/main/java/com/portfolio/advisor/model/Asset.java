package com.portfolio.advisor.model;

import java.util.Objects;

/**
 * A tradable instrument. Carries the parameters the market simulator needs
 * (long-run drift and volatility) so every asset type can be simulated uniformly.
 */
public abstract class Asset {

    private final String symbol;
    private final String name;
    private final double annualDrift;
    private final double annualVolatility;

    protected Asset(String symbol, String name, double annualDrift, double annualVolatility) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (annualVolatility < 0) {
            throw new IllegalArgumentException("volatility must be >= 0");
        }
        this.symbol = symbol;
        this.name = Objects.requireNonNull(name, "name");
        this.annualDrift = annualDrift;
        this.annualVolatility = annualVolatility;
    }

    public abstract AssetClass assetClass();

    /** How strongly the asset reacts to the overall market regime (beta-like). */
    public abstract double regimeSensitivity();

    public String symbol() { return symbol; }
    public String name() { return name; }
    public double annualDrift() { return annualDrift; }
    public double annualVolatility() { return annualVolatility; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Asset other && symbol.equals(other.symbol));
    }

    @Override
    public int hashCode() { return symbol.hashCode(); }

    @Override
    public String toString() { return symbol + " (" + assetClass() + ")"; }
}
