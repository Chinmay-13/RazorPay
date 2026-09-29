package com.portfolio.advisor;

import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.cost.TieredTransactionCostModel;
import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.market.MarketDataService;
import com.portfolio.advisor.market.RegimeSwitchingPriceSimulator;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Bond;
import com.portfolio.advisor.model.FinancialGoal;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.Stock;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.rebalancing.ProportionalRebalancer;
import com.portfolio.advisor.rebalancing.RebalancePlan;
import com.portfolio.advisor.rebalancing.TaxAwareTrendRebalancer;
import com.portfolio.advisor.service.AdvisorFactory;
import com.portfolio.advisor.service.PortfolioAdvisorService;
import com.portfolio.advisor.tax.SimulatedTaxCalculator;
import com.portfolio.advisor.tax.TaxCalculator;

import java.time.LocalDate;
import java.util.List;

/** End-to-end demo: simulate a market, build a portfolio, let it drift, then advise and rebalance. */
public final class App {

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 7L;
        ConsoleReport report = new ConsoleReport(System.out);

        // 1. Simulated market (all tickers are fictional)
        MarketDataService market = new MarketDataService(new RegimeSwitchingPriceSimulator(seed), LocalDate.of(2023, 1, 2));
        market.listAsset(new Stock("NOVA", "Nova Technologies", "Technology", 1.3, 0.10, 0.30), 180);
        market.listAsset(new Stock("MEDX", "Medix Health", "Healthcare", 0.8, 0.07, 0.20), 95);
        market.listAsset(new Stock("VOLT", "Volt Energy", "Energy", 1.1, 0.06, 0.28), 60);
        market.listAsset(new Stock("FINQ", "FinQ Bancorp", "Financials", 1.2, 0.08, 0.24), 45);
        market.listAsset(new Stock("HOME", "HomeGoods Retail", "Consumer", 0.9, 0.07, 0.18), 130);
        market.listAsset(new Bond("GOV10", "Govt 10Y Treasury", 0.040, 10, "AAA", 0.035, 0.07), 100);
        market.listAsset(new Bond("CORP5", "Investment-Grade Corp 5Y", 0.050, 5, "A", 0.045, 0.06), 100);
        market.listAsset(new Bond("MUNI7", "Municipal 7Y", 0.030, 7, "AA", 0.030, 0.04), 100);
        market.advance(260);   // one year of history so the models have something to learn from

        TransactionCostModel costModel = new TieredTransactionCostModel();
        TaxCalculator taxCalculator = new SimulatedTaxCalculator();
        PortfolioAdvisorService advisor = AdvisorFactory.create(market, costModel, taxCalculator);
        advisor.onDriftAlert((portfolio, drift) -> drift.drifts().stream()
                .filter(d -> d.severity() == drift.severity())
                .forEach(d -> report.line(String.format("  >> DRIFT ALERT %s on %s: %s %s, %.1f%% vs target %.1f%% (band %.1f pts)",
                        portfolio.id(), drift.asOf(), d.assetClass(), d.severity(), d.currentWeight() * 100,
                        d.targetWeight() * 100, d.threshold() * 100))));

        // 2. Investor profile and target allocation
        InvestorProfile investor = new InvestorProfile("INV-1", "Demo Investor", 32, RiskTolerance.MODERATE, 20,
                12_000, 0.30, 0.15, List.of(
                new FinancialGoal("Emergency travel fund", 25_000, 2, FinancialGoal.Priority.LOW),
                new FinancialGoal("House down payment", 250_000, 6, FinancialGoal.Priority.HIGH),
                new FinancialGoal("Retirement corpus", 4_000_000, 28, FinancialGoal.Priority.HIGH)));
        advisor.registerInvestor(investor);
        TargetAllocation recommended = advisor.recommendAllocation(investor.investorId());
        TargetAllocation target = TargetAllocation.of(0.60, 0.35, 0.05);

        report.heading("1. PROFILE & TARGET ALLOCATION  (" + market.currentDate() + ")");
        report.line("Investor: " + investor.name() + ", age " + investor.age() + ", " + investor.riskTolerance()
                + ", horizon " + investor.investmentHorizonYears() + " yrs");
        report.line("Recommended by risk profiler: " + recommended);
        report.line("Target chosen by user:        " + target);

        Portfolio portfolio = advisor.createPortfolio(investor.investorId(), 500_000);
        advisor.setTargetAllocation(portfolio.id(), target);
        investToTarget(advisor, portfolio, target);
        market.advance(120);
        advisor.deposit(portfolio.id(), 40_000);          // a later top-up creates newer (short-term) lots
        advisor.buy(portfolio.id(), "NOVA", 80);
        advisor.buy(portfolio.id(), "VOLT", 200);
        advisor.buy(portfolio.id(), "GOV10", 150);

        report.heading("2. SIMULATING THE MARKET (automated drift monitoring is live)");
        market.advance(200);
        report.line("Simulated to " + market.currentDate());

        report.heading("3. CURRENT PORTFOLIO & ALLOCATION  (" + market.currentDate() + ")");
        AllocationSnapshot snapshot = advisor.currentAllocation(portfolio.id());
        report.holdings(portfolio, market.currentPrices(), snapshot);

        report.heading("4. DRIFT DETECTION (5/25 tolerance bands)");
        report.drift(advisor.checkDrift(portfolio.id()));

        report.heading("5. AI MARKET OUTLOOK (Ridge regression + momentum ensemble, "
                + AdvisorFactory.FORECAST_HORIZON_DAYS + "-day horizon)");
        report.outlook(advisor.marketOutlook());

        report.heading("6. REBALANCING PLANS");
        RebalancePlan naive = advisor.proposeRebalance(portfolio.id(), new ProportionalRebalancer(costModel, taxCalculator));
        report.plan(naive);
        report.line("");
        RebalancePlan smart = advisor.proposeRebalance(portfolio.id(), new TaxAwareTrendRebalancer(costModel, taxCalculator));
        report.plan(smart);
        report.line("");
        report.line(String.format("Tax-aware plan saves %s in friction versus the naive plan.",
                ConsoleReport.money(naive.totalFriction() - smart.totalFriction())));

        advisor.execute(smart);
        report.heading("7. AFTER EXECUTING THE TAX-AWARE PLAN");
        report.holdings(portfolio, market.currentPrices(), advisor.currentAllocation(portfolio.id()));
        report.drift(advisor.checkDrift(portfolio.id()));

        report.heading("8. PERSONALISED INVESTMENT ADVICE");
        report.advice(advisor.advise(portfolio.id()));
    }

    /** Initial funding: spread each class's target value equally across that class's assets. */
    private static void investToTarget(PortfolioAdvisorService advisor, Portfolio portfolio, TargetAllocation target) {
        double total = portfolio.cash();
        for (AssetClass ac : List.of(AssetClass.STOCK, AssetClass.BOND)) {
            List<Asset> assets = advisor.market().assetsOf(ac);
            double perAsset = total * target.weight(ac) / assets.size();
            for (Asset a : assets) {
                double qty = Math.floor(perAsset * 0.998 / advisor.market().price(a.symbol()));
                if (qty > 0) advisor.buy(portfolio.id(), a.symbol(), qty);
            }
        }
    }
}
