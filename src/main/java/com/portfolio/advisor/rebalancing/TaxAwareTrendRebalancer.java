package com.portfolio.advisor.rebalancing;

import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.drift.DriftSeverity;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.TaxLot;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.prediction.TrendDirection;
import com.portfolio.advisor.prediction.TrendForecast;
import com.portfolio.advisor.tax.LotSelectionStrategy;
import com.portfolio.advisor.tax.TaxCalculator;
import com.portfolio.advisor.tax.TaxEfficientLotSelection;
import com.portfolio.advisor.util.LinearAlgebra;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The "optimal" strategy. On top of the template it:
 * <ol>
 *   <li><b>Tilts</b> the target a little toward the asset class the AI outlook prefers. The tilt is
 *       capped by risk tolerance and kept well inside the drift band, so the long-term target is kept.</li>
 *   <li><b>Chooses what to sell</b> by minimising (tax per $ sold + forecast return given up per $).</li>
 *   <li><b>Chooses lots</b> tax-efficiently (losses first, short-term gains last) and refuses to sell lots
 *       whose tax exceeds {@code maxMarginalTaxRate} of the proceeds, unless drift is CRITICAL.</li>
 *   <li><b>Chooses what to buy</b> by forecast score: bearish assets are skipped, the top candidates are
 *       weighted by (1 + k * score).</li>
 * </ol>
 */
public final class TaxAwareTrendRebalancer extends AbstractRebalancer {

    private static final int MAX_BUY_CANDIDATES = 3;
    private static final double BUY_TILT_STRENGTH = 20.0;
    private static final double CLASS_TILT_SENSITIVITY = 2.0;

    private final LotSelectionStrategy lotSelection;
    private final double maxMarginalTaxRate;

    public TaxAwareTrendRebalancer(TransactionCostModel costModel, TaxCalculator taxCalculator) {
        this(costModel, taxCalculator, 1000, 0.08);
    }

    public TaxAwareTrendRebalancer(TransactionCostModel costModel, TaxCalculator taxCalculator,
                                   double minTradeValue, double maxMarginalTaxRate) {
        super(costModel, taxCalculator, minTradeValue);
        this.lotSelection = new TaxEfficientLotSelection(taxCalculator);
        this.maxMarginalTaxRate = maxMarginalTaxRate;
    }

    @Override
    public String name() { return "Tax-aware + AI trend tilt"; }

    static double maxTilt(RiskTolerance tolerance) {
        return switch (tolerance) {
            case CONSERVATIVE -> 0.01;
            case MODERATE -> 0.02;
            case AGGRESSIVE -> 0.03;
        };
    }

    @Override
    protected Map<AssetClass, Double> effectiveTarget(RebalanceContext ctx, List<String> notes) {
        Map<AssetClass, Double> target = new EnumMap<>(ctx.target().weights());
        double stockScore = ctx.outlook().classScore(AssetClass.STOCK);
        double bondScore = ctx.outlook().classScore(AssetClass.BOND);
        double cap = maxTilt(ctx.profile().riskTolerance());
        double tilt = LinearAlgebra.clamp((stockScore - bondScore) * CLASS_TILT_SENSITIVITY, -cap, cap);
        // never tilt a class below zero
        tilt = LinearAlgebra.clamp(tilt, -target.get(AssetClass.STOCK), target.get(AssetClass.BOND));
        if (Math.abs(tilt) >= 0.001) {
            target.merge(AssetClass.STOCK, tilt, Double::sum);
            target.merge(AssetClass.BOND, -tilt, Double::sum);
            notes.add(String.format("AI tilt %+.1f%% toward %s (score stock %+.2f%% vs bond %+.2f%%, cap %.0f%% for %s).",
                    Math.abs(tilt) * 100, tilt > 0 ? "STOCK" : "BOND", stockScore * 100, bondScore * 100,
                    cap * 100, ctx.profile().riskTolerance()));
        }
        return target;
    }

    @Override
    protected List<Trade> sellFromClass(RebalanceContext ctx, AssetClass assetClass, double amount, List<String> notes) {
        boolean critical = ctx.drift().severity().atLeast(DriftSeverity.CRITICAL);
        List<Holding> ranked = holdingsOf(ctx, assetClass).stream()
                .sorted(Comparator.comparingDouble(h -> sellPenalty(ctx, h)))
                .toList();

        List<Trade> trades = new ArrayList<>();
        double remaining = amount;
        for (Holding h : ranked) {
            if (remaining < minTradeValue) break;
            Asset asset = h.asset();
            double price = ctx.price(asset.symbol());
            List<LotSale> sales = new ArrayList<>();
            double toSell = remaining;
            boolean deferred = false;
            for (TaxLot lot : lotSelection.order(h, price, ctx.asOf(), ctx.profile())) {
                if (toSell < minTradeValue) break;
                boolean longTerm = lot.isLongTerm(ctx.asOf());
                double taxRate = taxCalculator.taxPerUnit(price, lot.costPerUnit(), longTerm, ctx.profile()) / price;
                if (!critical && taxRate > maxMarginalTaxRate) {
                    deferred = true;
                    break;   // lots are sorted by tax, the rest are worse
                }
                double qty = Math.min(lot.quantity(), Math.ceil(toSell / price));
                sales.add(new LotSale(lot, qty, price, longTerm));
                toSell -= qty * price;
            }
            if (deferred) {
                notes.add(String.format("Kept high-gain lots of %s: selling them would cost > %.0f%% of proceeds in tax "
                        + "and drift is not critical.", asset.symbol(), maxMarginalTaxRate * 100));
            }
            Optional<Trade> trade = sellTrade(ctx, asset, sales, sellRationale(ctx, asset, assetClass, sales));
            if (trade.isPresent()) {
                trades.add(trade.get());
                remaining -= trade.get().grossValue();
            }
        }
        if (remaining >= minTradeValue) {
            notes.add(String.format("%s left $%,.0f overweight to avoid expensive tax; new contributions "
                    + "should go to underweight classes.", assetClass, remaining));
        }
        return trades;
    }

    /** Cost of selling $1: tax on the cheapest lot + expected return forgone. Lower is sold first. */
    private double sellPenalty(RebalanceContext ctx, Holding h) {
        double price = ctx.price(h.asset().symbol());
        List<TaxLot> lots = lotSelection.order(h, price, ctx.asOf(), ctx.profile());
        TaxLot first = lots.get(0);
        double taxPerDollar = taxCalculator.taxPerUnit(price, first.costPerUnit(), first.isLongTerm(ctx.asOf()),
                ctx.profile()) / price;
        return taxPerDollar + ctx.outlook().score(h.asset().symbol());
    }

    private String sellRationale(RebalanceContext ctx, Asset asset, AssetClass ac, List<LotSale> sales) {
        double gain = sales.stream().mapToDouble(LotSale::gain).sum();
        String outlook = ctx.outlook().forecast(asset.symbol()).map(TaxAwareTrendRebalancer::describe).orElse("n/a");
        return String.format("Trim overweight %s; %s; lots realise %s$%,.0f", ac, outlook,
                gain < 0 ? "loss " : "gain ", Math.abs(gain));
    }

    @Override
    protected List<Trade> buyIntoClass(RebalanceContext ctx, AssetClass assetClass, double amount, List<String> notes) {
        record Candidate(Asset asset, double weight) { }
        List<Candidate> candidates = ctx.universeOf(assetClass).stream()
                .filter(a -> ctx.outlook().forecast(a.symbol())
                        .map(f -> f.direction() != TrendDirection.BEARISH).orElse(true))
                .map(a -> new Candidate(a, Math.max(0, 1 + BUY_TILT_STRENGTH * ctx.outlook().score(a.symbol()))))
                .filter(c -> c.weight() > 0)
                .sorted(Comparator.comparingDouble(Candidate::weight).reversed())
                .limit(MAX_BUY_CANDIDATES)
                .toList();
        if (candidates.isEmpty()) {
            candidates = ctx.universeOf(assetClass).stream().map(a -> new Candidate(a, 1)).toList();
            notes.add("All " + assetClass + " forecasts are bearish; buying equally to hold the target anyway.");
        }
        double weightSum = candidates.stream().mapToDouble(Candidate::weight).sum();
        List<Trade> trades = new ArrayList<>();
        for (Candidate c : candidates) {
            String outlook = ctx.outlook().forecast(c.asset().symbol()).map(TaxAwareTrendRebalancer::describe).orElse("n/a");
            buyTrade(ctx, c.asset(), amount * c.weight() / weightSum,
                    "Fill underweight " + assetClass + "; " + outlook).ifPresent(trades::add);
        }
        return trades;
    }

    private static String describe(TrendForecast f) {
        return String.format("AI %s %+.1f%% (conf %.0f%%)", f.direction(), f.expectedReturn() * 100, f.confidence() * 100);
    }
}
