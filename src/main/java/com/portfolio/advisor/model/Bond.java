package com.portfolio.advisor.model;

public final class Bond extends Asset {

    private final double couponRate;
    private final int maturityYears;
    private final String creditRating;

    public Bond(String symbol, String name, double couponRate, int maturityYears, String creditRating,
                double annualDrift, double annualVolatility) {
        super(symbol, name, annualDrift, annualVolatility);
        this.couponRate = couponRate;
        this.maturityYears = maturityYears;
        this.creditRating = creditRating;
    }

    @Override public AssetClass assetClass() { return AssetClass.BOND; }

    /** Bonds tend to move slightly against equity regimes (flight to quality). */
    @Override public double regimeSensitivity() { return -0.15; }

    public double couponRate() { return couponRate; }
    public int maturityYears() { return maturityYears; }
    public String creditRating() { return creditRating; }
}
