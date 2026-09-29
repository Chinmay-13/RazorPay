package com.portfolio.advisor.rebalancing;

import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.tax.FifoLotSelection;
import com.portfolio.advisor.tax.LotSelectionStrategy;
import com.portfolio.advisor.tax.TaxCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Baseline "textbook" rebalancer: back to the exact target, trimming and adding to every
 * holding pro-rata, FIFO lots, no forecasts. Used as the benchmark the smart strategy is
 * compared against.
 */
public final class ProportionalRebalancer extends AbstractRebalancer {

    private final LotSelectionStrategy lotSelection = new FifoLotSelection();

    public ProportionalRebalancer(TransactionCostModel costModel, TaxCalculator taxCalculator) {
        super(costModel, taxCalculator, 500);
    }

    @Override
    public String name() { return "Proportional (FIFO, no forecast)"; }

    @Override
    protected List<Trade> sellFromClass(RebalanceContext ctx, AssetClass assetClass, double amount, List<String> notes) {
        List<Holding> holdings = holdingsOf(ctx, assetClass);
        double classValue = ctx.snapshot().value(assetClass);
        List<Trade> trades = new ArrayList<>();
        for (Holding h : holdings) {
            double price = ctx.price(h.asset().symbol());
            double share = h.marketValue(price) / classValue;
            double qty = Math.min(h.quantity(), Math.ceil(amount * share / price));
            if (qty <= 0) continue;
            sellTrade(ctx, h.asset(), lotSelection.select(h, qty, price, ctx.asOf(), ctx.profile()),
                    "Trim overweight " + assetClass + " pro-rata").ifPresent(trades::add);
        }
        return trades;
    }

    @Override
    protected List<Trade> buyIntoClass(RebalanceContext ctx, AssetClass assetClass, double amount, List<String> notes) {
        List<Holding> holdings = holdingsOf(ctx, assetClass);
        List<Trade> trades = new ArrayList<>();
        if (holdings.isEmpty()) {
            List<Asset> universe = ctx.universeOf(assetClass);
            for (Asset a : universe) {
                buyTrade(ctx, a, amount / universe.size(), "Add to underweight " + assetClass).ifPresent(trades::add);
            }
            return trades;
        }
        double classValue = ctx.snapshot().value(assetClass);
        for (Holding h : holdings) {
            double share = h.marketValue(ctx.price(h.asset().symbol())) / classValue;
            buyTrade(ctx, h.asset(), amount * share, "Add to underweight " + assetClass + " pro-rata")
                    .ifPresent(trades::add);
        }
        return trades;
    }
}
