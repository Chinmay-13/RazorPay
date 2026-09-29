package com.portfolio.advisor.advice;

import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.TargetAllocation;

import java.util.Map;

/** Long-run annual return / volatility assumptions used for goal planning (simulated). */
public final class CapitalMarketAssumptions {

    private static final Map<AssetClass, Double> EXPECTED_RETURN =
            Map.of(AssetClass.STOCK, 0.08, AssetClass.BOND, 0.04, AssetClass.CASH, 0.02);
    private static final Map<AssetClass, Double> VOLATILITY =
            Map.of(AssetClass.STOCK, 0.17, AssetClass.BOND, 0.06, AssetClass.CASH, 0.005);

    private CapitalMarketAssumptions() { }

    public static double expectedReturn(TargetAllocation allocation) {
        return allocation.weights().entrySet().stream()
                .mapToDouble(e -> e.getValue() * EXPECTED_RETURN.get(e.getKey())).sum();
    }

    /** Portfolio volatility assuming stock/bond correlation of 0.1 and uncorrelated cash. */
    public static double volatility(TargetAllocation allocation) {
        double ws = allocation.weight(AssetClass.STOCK) * VOLATILITY.get(AssetClass.STOCK);
        double wb = allocation.weight(AssetClass.BOND) * VOLATILITY.get(AssetClass.BOND);
        double wc = allocation.weight(AssetClass.CASH) * VOLATILITY.get(AssetClass.CASH);
        return Math.sqrt(ws * ws + wb * wb + wc * wc + 2 * 0.1 * ws * wb);
    }
}
