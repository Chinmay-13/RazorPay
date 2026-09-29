package com.portfolio.advisor.rebalancing;

import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.model.TradeSide;
import com.portfolio.advisor.tax.TaxCalculator;
import com.portfolio.advisor.tax.TaxImpact;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Template method for rebalancing. The skeleton is fixed:
 * <ol>
 *   <li>decide the effective target (subclasses may tilt it),</li>
 *   <li>sell overweight classes,</li>
 *   <li>spend the freed cash (above the cash target) on underweight classes, pro-rata to their gaps,</li>
 *   <li>project the post-trade allocation and the net tax bill.</li>
 * </ol>
 * Subclasses decide <em>which</em> assets and lots to trade.
 */
public abstract class AbstractRebalancer implements RebalancingStrategy {

    protected static final List<AssetClass> INVESTABLE = List.of(AssetClass.STOCK, AssetClass.BOND);

    protected final TransactionCostModel costModel;
    protected final TaxCalculator taxCalculator;
    protected final double minTradeValue;

    protected AbstractRebalancer(TransactionCostModel costModel, TaxCalculator taxCalculator, double minTradeValue) {
        this.costModel = costModel;
        this.taxCalculator = taxCalculator;
        this.minTradeValue = minTradeValue;
    }

    @Override
    public final RebalancePlan plan(RebalanceContext ctx) {
        List<String> notes = new ArrayList<>();
        Map<AssetClass, Double> target = effectiveTarget(ctx, notes);
        double total = ctx.snapshot().totalValue();
        List<Trade> trades = new ArrayList<>();
        double cash = ctx.portfolio().cash();

        for (AssetClass ac : INVESTABLE) {
            double excess = ctx.snapshot().value(ac) - target.get(ac) * total;
            if (excess > minTradeValue) {
                List<Trade> sells = sellFromClass(ctx, ac, excess, notes);
                trades.addAll(sells);
                cash += sells.stream().mapToDouble(Trade::netCashFlow).sum();
            }
        }

        double budget = cash - target.get(AssetClass.CASH) * total;
        Map<AssetClass, Double> deficits = new EnumMap<>(AssetClass.class);
        double totalDeficit = 0;
        for (AssetClass ac : INVESTABLE) {
            double deficit = target.get(ac) * total - ctx.snapshot().value(ac);
            if (deficit > 0) {
                deficits.put(ac, deficit);
                totalDeficit += deficit;
            }
        }
        if (budget > minTradeValue && totalDeficit > 0) {
            double scale = Math.min(1.0, budget / totalDeficit);
            if (scale < 1.0) {
                notes.add(String.format("Buys scaled to %.0f%% of the gap: only $%,.0f of cash is available above the cash target.",
                        scale * 100, budget));
            }
            for (Map.Entry<AssetClass, Double> e : deficits.entrySet()) {
                double amount = e.getValue() * scale;
                if (amount > minTradeValue) trades.addAll(buyIntoClass(ctx, e.getKey(), amount, notes));
            }
        }

        List<LotSale> allSales = trades.stream().flatMap(t -> t.lotSales().stream()).toList();
        TaxImpact tax = allSales.isEmpty() ? TaxImpact.NONE : taxCalculator.estimate(allSales, ctx.profile());
        if (trades.isEmpty()) notes.add("No trades needed: portfolio is within tolerance of its target.");

        return new RebalancePlan(ctx.portfolio().id(), name(), ctx.asOf(), trades, total,
                weights(ctx.snapshot().classValues(), total), target, project(ctx, trades), tax, notes);
    }

    /** Target weights to rebalance towards. Default: the user's target, unchanged. */
    protected Map<AssetClass, Double> effectiveTarget(RebalanceContext ctx, List<String> notes) {
        return new EnumMap<>(ctx.target().weights());
    }

    protected abstract List<Trade> sellFromClass(RebalanceContext ctx, AssetClass assetClass, double amount,
                                                 List<String> notes);

    protected abstract List<Trade> buyIntoClass(RebalanceContext ctx, AssetClass assetClass, double amount,
                                                List<String> notes);

    // ---- helpers shared by concrete strategies -------------------------------------------------

    protected List<Holding> holdingsOf(RebalanceContext ctx, AssetClass assetClass) {
        return ctx.portfolio().holdings().stream().filter(h -> h.asset().assetClass() == assetClass).toList();
    }

    protected Optional<Trade> sellTrade(RebalanceContext ctx, Asset asset, List<LotSale> sales, String rationale) {
        double qty = sales.stream().mapToDouble(LotSale::quantity).sum();
        double price = ctx.price(asset.symbol());
        if (qty <= 0 || qty * price < minTradeValue) return Optional.empty();
        double cost = costModel.estimate(asset, TradeSide.SELL, qty, price);
        double tax = taxCalculator.estimate(sales, ctx.profile()).estimatedTax();
        return Optional.of(new Trade(TradeSide.SELL, asset, qty, price, cost, tax, sales, rationale));
    }

    /** Largest whole-unit buy whose value plus costs fits in {@code amount}. */
    protected Optional<Trade> buyTrade(RebalanceContext ctx, Asset asset, double amount, String rationale) {
        double price = ctx.price(asset.symbol());
        double qty = Math.floor(amount / price);
        while (qty > 0 && qty * price + costModel.estimate(asset, TradeSide.BUY, qty, price) > amount) qty--;
        if (qty <= 0 || qty * price < minTradeValue) return Optional.empty();
        double cost = costModel.estimate(asset, TradeSide.BUY, qty, price);
        return Optional.of(new Trade(TradeSide.BUY, asset, qty, price, cost, 0, List.of(), rationale));
    }

    private Map<AssetClass, Double> project(RebalanceContext ctx, List<Trade> trades) {
        Map<AssetClass, Double> values = new EnumMap<>(ctx.snapshot().classValues());
        for (Trade t : trades) {
            double signed = t.side() == TradeSide.BUY ? t.grossValue() : -t.grossValue();
            values.merge(t.asset().assetClass(), signed, Double::sum);
            values.merge(AssetClass.CASH, t.netCashFlow(), Double::sum);
        }
        double total = values.values().stream().mapToDouble(Double::doubleValue).sum();
        return weights(values, total);
    }

    private static Map<AssetClass, Double> weights(Map<AssetClass, Double> values, double total) {
        Map<AssetClass, Double> w = new EnumMap<>(AssetClass.class);
        for (AssetClass ac : AssetClass.values()) w.put(ac, total == 0 ? 0 : values.getOrDefault(ac, 0.0) / total);
        return w;
    }
}
