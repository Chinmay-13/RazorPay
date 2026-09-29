package com.portfolio.advisor;

import com.portfolio.advisor.allocation.AllocationCalculator;
import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.cost.TieredTransactionCostModel;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.drift.ToleranceBandDriftDetector;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.model.TradeSide;
import com.portfolio.advisor.prediction.MarketOutlook;
import com.portfolio.advisor.prediction.TrendDirection;
import com.portfolio.advisor.prediction.TrendForecast;
import com.portfolio.advisor.rebalancing.ProportionalRebalancer;
import com.portfolio.advisor.rebalancing.RebalanceContext;
import com.portfolio.advisor.rebalancing.RebalancePlan;
import com.portfolio.advisor.rebalancing.TaxAwareTrendRebalancer;
import com.portfolio.advisor.tax.SimulatedTaxCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RebalancingTest {

    private static final LocalDate BOUGHT_OLD = LocalDate.of(2023, 1, 3);
    private static final LocalDate BOUGHT_RECENT = LocalDate.of(2025, 4, 1);
    private static final LocalDate TODAY = LocalDate.of(2025, 6, 2);

    private final TieredTransactionCostModel costs = new TieredTransactionCostModel();
    private final SimulatedTaxCalculator tax = new SimulatedTaxCalculator();
    private final TargetAllocation target = TargetAllocation.of(0.60, 0.35, 0.05);
    private final InvestorProfile profile = TestFixtures.profile(RiskTolerance.MODERATE);
    private final List<Asset> universe = List.of(TestFixtures.STOCK_A, TestFixtures.STOCK_B, TestFixtures.BOND_A);

    /** Stocks have rallied: $800k stock / $300k bond / $0 cash = ~73/27/0 against a 60/35/5 target. */
    private Portfolio driftedPortfolio() {
        Portfolio p = new Portfolio("P", "I1", 830_000);
        p.buy(TestFixtures.STOCK_A, 1_500, 100, 0, BOUGHT_OLD);     // big long-term gain
        p.buy(TestFixtures.STOCK_B, 1_500, 100, 0, BOUGHT_OLD);
        p.buy(TestFixtures.STOCK_B, 1_000, 230, 0, BOUGHT_RECENT);  // short-term loss
        p.buy(TestFixtures.BOND_A, 3_000, 100, 0, BOUGHT_OLD);
        p.setTargetAllocation(target);
        return p;
    }

    private RebalanceContext context(Portfolio p, MarketOutlook outlook) {
        Map<String, Double> prices = Map.of("AAA", 200.0, "BBB", 200.0, "BND", 100.0);
        AllocationSnapshot snapshot = new AllocationCalculator().calculate(p, prices);
        DriftReport drift = new ToleranceBandDriftDetector().detect(p.id(), snapshot, target, TODAY);
        return new RebalanceContext(p, profile, target, snapshot, drift, outlook, prices, universe, TODAY);
    }

    private static MarketOutlook neutralOutlook() {
        return new MarketOutlook(Map.of(), Map.of());
    }

    private static void assertBudgetRespected(Portfolio p, RebalancePlan plan) {
        double cash = p.cash() + plan.trades().stream().mapToDouble(Trade::netCashFlow).sum();
        assertTrue(cash >= 0, "plan must never spend more cash than it has: " + cash);
        List<Trade> trades = plan.trades();
        int lastSell = -1, firstBuy = trades.size();
        for (int i = 0; i < trades.size(); i++) {
            if (trades.get(i).side() == TradeSide.SELL) lastSell = i;
            else firstBuy = Math.min(firstBuy, i);
        }
        assertTrue(lastSell < firstBuy, "sells must precede buys");
    }

    @Test
    void driftedPortfolioNeedsRebalance() {
        assertTrue(context(driftedPortfolio(), neutralOutlook()).drift().rebalanceRequired());
    }

    @Test
    void proportionalPlanRestoresTarget() {
        Portfolio p = driftedPortfolio();
        RebalancePlan plan = new ProportionalRebalancer(costs, tax).plan(context(p, neutralOutlook()));
        assertFalse(plan.isEmpty());
        for (AssetClass ac : AssetClass.values()) {
            assertEquals(target.weight(ac), plan.projectedWeights().get(ac), 0.01, ac.name());
        }
        assertBudgetRespected(p, plan);
    }

    @Test
    void taxAwarePlanHarvestsLossesAndPaysLessTax() {
        Portfolio p = driftedPortfolio();
        RebalanceContext ctx = context(p, neutralOutlook());
        RebalancePlan naive = new ProportionalRebalancer(costs, tax).plan(ctx);
        RebalancePlan smart = new TaxAwareTrendRebalancer(costs, tax).plan(ctx);

        assertTrue(smart.totalEstimatedTax() < naive.totalEstimatedTax());
        assertTrue(smart.sells().stream().anyMatch(t -> t.realizedGain() < 0), "should sell the loss lot");
        assertEquals(target.weight(AssetClass.STOCK), smart.projectedWeights().get(AssetClass.STOCK), 0.03);
        assertBudgetRespected(p, smart);
    }

    @Test
    void executingAPlanLeavesPortfolioWithinBands() {
        Portfolio p = driftedPortfolio();
        RebalanceContext ctx = context(p, neutralOutlook());
        RebalancePlan plan = new TaxAwareTrendRebalancer(costs, tax, 1000, 1.0).plan(ctx);
        plan.trades().forEach(t -> p.execute(t, TODAY));
        AllocationSnapshot after = new AllocationCalculator().calculate(p, ctx.prices());
        DriftReport drift = new ToleranceBandDriftDetector().detect(p.id(), after, target, TODAY);
        assertFalse(drift.rebalanceRequired());
        assertTrue(p.cash() >= 0);
    }

    @Test
    void bullishStockOutlookTiltsTargetWithinCap() {
        Portfolio p = driftedPortfolio();
        MarketOutlook bullish = new MarketOutlook(
                Map.of("AAA", new TrendForecast("AAA", TrendDirection.BULLISH, 0.05, 0.9, 21, "t"),
                       "BBB", new TrendForecast("BBB", TrendDirection.BEARISH, -0.03, 0.9, 21, "t")),
                Map.of(AssetClass.STOCK, 0.03, AssetClass.BOND, 0.0));
        RebalancePlan plan = new TaxAwareTrendRebalancer(costs, tax).plan(context(p, bullish));
        double tilted = plan.effectiveTarget().get(AssetClass.STOCK);
        assertEquals(0.62, tilted, 1e-9, "MODERATE cap is 2 pts");
        assertTrue(plan.buys().stream().noneMatch(t -> t.asset().symbol().equals("BBB")), "never buy a bearish name");
    }
}
