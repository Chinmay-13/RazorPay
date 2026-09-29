package com.portfolio.advisor.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable target weights per asset class; weights must sum to 1. */
public final class TargetAllocation {

    private static final double TOLERANCE = 1e-6;

    private final Map<AssetClass, Double> weights;

    private TargetAllocation(Map<AssetClass, Double> weights) {
        this.weights = Collections.unmodifiableMap(weights);
    }

    public static TargetAllocation of(Map<AssetClass, Double> weights) {
        EnumMap<AssetClass, Double> copy = new EnumMap<>(AssetClass.class);
        double sum = 0;
        for (AssetClass ac : AssetClass.values()) {
            double w = weights.getOrDefault(ac, 0.0);
            if (w < 0 || w > 1) {
                throw new IllegalArgumentException("weight for " + ac + " must be in [0,1]: " + w);
            }
            copy.put(ac, w);
            sum += w;
        }
        if (Math.abs(sum - 1.0) > TOLERANCE) {
            throw new IllegalArgumentException("target weights must sum to 1.0 but were " + sum);
        }
        return new TargetAllocation(copy);
    }

    public static TargetAllocation of(double stock, double bond, double cash) {
        return of(Map.of(AssetClass.STOCK, stock, AssetClass.BOND, bond, AssetClass.CASH, cash));
    }

    public double weight(AssetClass assetClass) { return weights.get(assetClass); }
    public Map<AssetClass, Double> weights() { return weights; }

    @Override
    public String toString() {
        return String.format("STOCK %.1f%% / BOND %.1f%% / CASH %.1f%%",
                weight(AssetClass.STOCK) * 100, weight(AssetClass.BOND) * 100, weight(AssetClass.CASH) * 100);
    }
}
