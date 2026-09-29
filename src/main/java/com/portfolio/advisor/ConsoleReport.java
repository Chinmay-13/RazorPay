package com.portfolio.advisor;

import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.GoalAssessment;
import com.portfolio.advisor.advice.InvestmentAdvice;
import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.drift.AssetClassDrift;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.prediction.MarketOutlook;
import com.portfolio.advisor.prediction.TrendForecast;
import com.portfolio.advisor.rebalancing.RebalancePlan;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map;

/** Plain-text rendering of the advisor's outputs. */
public final class ConsoleReport {

    private final PrintStream out;

    public ConsoleReport(PrintStream out) { this.out = out; }

    public void heading(String title) {
        out.println();
        out.println("=".repeat(96));
        out.println(" " + title);
        out.println("=".repeat(96));
    }

    public void line(String text) { out.println(text); }

    public void holdings(Portfolio portfolio, Map<String, Double> prices, AllocationSnapshot snapshot) {
        out.printf("%-8s %-6s %10s %12s %14s %14s %8s %5s%n",
                "Symbol", "Class", "Qty", "Price", "Value", "Unrealized", "Weight", "Lots");
        for (Holding h : portfolio.holdings()) {
            String s = h.asset().symbol();
            double price = prices.get(s);
            out.printf("%-8s %-6s %10.0f %12.2f %14s %14s %7.1f%% %5d%n", s, h.asset().assetClass(), h.quantity(), price,
                    money(h.marketValue(price)), money(h.unrealizedGain(price)), snapshot.assetWeight(s) * 100, h.lots().size());
        }
        out.printf("%-8s %-6s %10s %12s %14s %14s %7.1f%%%n", "CASH", "CASH", "", "", money(portfolio.cash()), "",
                snapshot.weight(AssetClass.CASH) * 100);
        out.printf("Total portfolio value: %s%n", money(snapshot.totalValue()));
    }

    public void drift(DriftReport report) {
        out.printf("%-6s %9s %9s %9s %9s  %s%n", "Class", "Current", "Target", "Drift", "Band", "Severity");
        for (AssetClassDrift d : report.drifts()) {
            out.printf("%-6s %8.1f%% %8.1f%% %+8.1f%% %8.1f%%  %s%n", d.assetClass(), d.currentWeight() * 100,
                    d.targetWeight() * 100, d.absoluteDrift() * 100, d.threshold() * 100, d.severity());
        }
        out.printf("Overall: %s -> %s%n", report.severity(),
                report.rebalanceRequired() ? "REBALANCE RECOMMENDED" : "no rebalance needed");
    }

    public void outlook(MarketOutlook outlook) {
        out.printf("%-8s %-9s %10s %11s%n", "Symbol", "Trend", "E[return]", "Confidence");
        outlook.bySymbol().values().stream().sorted(Comparator.comparing(TrendForecast::symbol)).forEach(f ->
                out.printf("%-8s %-9s %+9.2f%% %10.0f%%%n", f.symbol(), f.direction(), f.expectedReturn() * 100,
                        f.confidence() * 100));
        out.printf("Class scores (E[return] x confidence): STOCK %+.2f%%  BOND %+.2f%%%n",
                outlook.classScore(AssetClass.STOCK) * 100, outlook.classScore(AssetClass.BOND) * 100);
    }

    public void plan(RebalancePlan plan) {
        out.println("Strategy: " + plan.strategy());
        for (Trade t : plan.trades()) {
            out.printf("  %-4s %6.0f %-7s @ %9.2f = %12s  cost %8s  tax %10s  | %s%n", t.side(), t.quantity(),
                    t.asset().symbol(), t.price(), money(t.grossValue()), money(t.transactionCost()),
                    money(t.estimatedTax()), t.rationale());
        }
        out.printf("  Weights  before: %s%n", weights(plan.currentWeights()));
        out.printf("           target: %s%n", weights(plan.effectiveTarget()));
        out.printf("            after: %s%n", weights(plan.projectedWeights()));
        out.printf("  Trades %d | turnover %.1f%% | transaction cost %s | est. tax %s (ST gain %s, LT gain %s) | total friction %s%n",
                plan.trades().size(), plan.turnover() * 100, money(plan.totalTransactionCost()),
                money(plan.totalEstimatedTax()), money(plan.taxImpact().shortTermGain()),
                money(plan.taxImpact().longTermGain()), money(plan.totalFriction()));
        plan.notes().forEach(n -> out.println("  note: " + n));
    }

    public void advice(InvestmentAdvice advice) {
        out.printf("Recommended allocation for this profile: %s%n", advice.recommendedAllocation());
        out.printf("Current target: expected return %.1f%%/yr, volatility %.1f%%/yr%n",
                advice.expectedAnnualReturn() * 100, advice.expectedVolatility() * 100);
        out.println("Goals:");
        for (GoalAssessment g : advice.goals()) {
            out.printf("  %-22s target %12s in %2d yrs | projected %12s | %5.0f%% funded | %s%n", g.goal().name(),
                    money(g.goal().targetAmount()), g.goal().yearsToGoal(), money(g.projectedValue()),
                    g.fundedRatio() * 100, g.onTrack() ? "ON TRACK" : "SHORTFALL");
        }
        out.println("Advice:");
        for (AdviceItem item : advice.items()) {
            out.printf("  [%-6s] %-15s %s%n", item.priority(), item.category(), item.title());
            out.printf("           %s%n", item.detail());
        }
    }

    private static String weights(Map<AssetClass, Double> w) {
        return String.format("STOCK %5.1f%% | BOND %5.1f%% | CASH %5.1f%%", w.get(AssetClass.STOCK) * 100,
                w.get(AssetClass.BOND) * 100, w.get(AssetClass.CASH) * 100);
    }

    static String money(double v) {
        return (v < 0 ? "-$" : "$") + String.format("%,.0f", Math.abs(v));
    }
}
