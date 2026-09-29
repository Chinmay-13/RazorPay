package com.portfolio.advisor.cost;

import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.TradeSide;

import java.util.EnumMap;
import java.util.Map;

/**
 * Simulated brokerage costs: a flat ticket fee plus commission and half the bid/ask
 * spread, both in basis points and both depending on asset class (bonds are less liquid).
 */
public final class TieredTransactionCostModel implements TransactionCostModel {

    private final double flatFee;
    private final Map<AssetClass, Double> commissionBps;
    private final Map<AssetClass, Double> halfSpreadBps;

    public TieredTransactionCostModel() {
        this(1.0,
                Map.of(AssetClass.STOCK, 5.0, AssetClass.BOND, 10.0),
                Map.of(AssetClass.STOCK, 2.0, AssetClass.BOND, 15.0));
    }

    public TieredTransactionCostModel(double flatFee, Map<AssetClass, Double> commissionBps,
                                      Map<AssetClass, Double> halfSpreadBps) {
        this.flatFee = flatFee;
        this.commissionBps = new EnumMap<>(commissionBps);
        this.halfSpreadBps = new EnumMap<>(halfSpreadBps);
    }

    @Override
    public double estimate(Asset asset, TradeSide side, double quantity, double price) {
        if (quantity <= 0) return 0;
        double notional = quantity * price;
        double bps = commissionBps.getOrDefault(asset.assetClass(), 0.0)
                + halfSpreadBps.getOrDefault(asset.assetClass(), 0.0);
        return flatFee + notional * bps / 10_000;
    }
}
