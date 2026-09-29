package com.portfolio.advisor.model;

public final class Stock extends Asset {

    private final String sector;
    private final double beta;

    public Stock(String symbol, String name, String sector, double beta,
                 double annualDrift, double annualVolatility) {
        super(symbol, name, annualDrift, annualVolatility);
        this.sector = sector;
        this.beta = beta;
    }

    @Override public AssetClass assetClass() { return AssetClass.STOCK; }
    @Override public double regimeSensitivity() { return beta; }

    public String sector() { return sector; }
    public double beta() { return beta; }
}
