package com.portfolio.advisor.service;

import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceEngine;
import com.portfolio.advisor.advice.GoalAssessment;
import com.portfolio.advisor.advice.GoalPlanner;
import com.portfolio.advisor.advice.InvestmentAdvice;
import com.portfolio.advisor.advice.RiskProfiler;
import com.portfolio.advisor.allocation.AllocationCalculator;
import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.drift.DriftDetector;
import com.portfolio.advisor.drift.DriftListener;
import com.portfolio.advisor.drift.DriftMonitor;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.market.MarketDataService;
import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.model.Trade;
import com.portfolio.advisor.model.TradeSide;
import com.portfolio.advisor.prediction.MarketOutlook;
import com.portfolio.advisor.prediction.TrendAnalysisService;
import com.portfolio.advisor.rebalancing.RebalanceContext;
import com.portfolio.advisor.rebalancing.RebalancePlan;
import com.portfolio.advisor.rebalancing.RebalancingStrategy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Facade: the single entry point a UI / REST layer would call. It wires the independent
 * components together and owns investors and portfolios (in-memory repositories).
 */
public final class PortfolioAdvisorService {

    private final MarketDataService market;
    private final AllocationCalculator calculator;
    private final DriftDetector driftDetector;
    private final DriftMonitor driftMonitor;
    private final TrendAnalysisService trendService;
    private final TransactionCostModel costModel;
    private final RiskProfiler riskProfiler;
    private final GoalPlanner goalPlanner;
    private final AdviceEngine adviceEngine;

    private final Map<String, InvestorProfile> investors = new ConcurrentHashMap<>();
    private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final AtomicLong portfolioSequence = new AtomicLong();

    private LocalDate outlookDate;
    private MarketOutlook cachedOutlook;

    public PortfolioAdvisorService(MarketDataService market, AllocationCalculator calculator,
                                   DriftDetector driftDetector, TrendAnalysisService trendService,
                                   TransactionCostModel costModel, RiskProfiler riskProfiler,
                                   GoalPlanner goalPlanner, AdviceEngine adviceEngine) {
        this.market = market;
        this.calculator = calculator;
        this.driftDetector = driftDetector;
        this.trendService = trendService;
        this.costModel = costModel;
        this.riskProfiler = riskProfiler;
        this.goalPlanner = goalPlanner;
        this.adviceEngine = adviceEngine;
        this.driftMonitor = new DriftMonitor(calculator, driftDetector);
        market.addListener(driftMonitor);
    }

    // ---- investors & portfolios ---------------------------------------------------------------

    public void registerInvestor(InvestorProfile profile) {
        investors.put(profile.investorId(), profile);
    }

    public Portfolio createPortfolio(String investorId, double initialCash) {
        investor(investorId);
        Portfolio portfolio = new Portfolio("PF-" + portfolioSequence.incrementAndGet(), investorId, initialCash);
        portfolios.put(portfolio.id(), portfolio);
        driftMonitor.watch(portfolio);
        return portfolio;
    }

    public TargetAllocation recommendAllocation(String investorId) {
        return riskProfiler.recommend(investor(investorId));
    }

    public void setTargetAllocation(String portfolioId, TargetAllocation target) {
        portfolio(portfolioId).setTargetAllocation(target);
        driftMonitor.reset(portfolioId);
    }

    public void deposit(String portfolioId, double amount) {
        portfolio(portfolioId).deposit(amount);
    }

    /** Market order at today's simulated close. */
    public Trade buy(String portfolioId, String symbol, double quantity) {
        Asset asset = market.asset(symbol);
        double price = market.price(symbol);
        Trade trade = new Trade(TradeSide.BUY, asset, quantity, price,
                costModel.estimate(asset, TradeSide.BUY, quantity, price), 0, List.of(), "Manual buy");
        portfolio(portfolioId).execute(trade, market.currentDate());
        return trade;
    }

    // ---- analytics ----------------------------------------------------------------------------

    public AllocationSnapshot currentAllocation(String portfolioId) {
        return calculator.calculate(portfolio(portfolioId), market.currentPrices());
    }

    public DriftReport checkDrift(String portfolioId) {
        Portfolio portfolio = portfolio(portfolioId);
        return driftDetector.detect(portfolioId, currentAllocation(portfolioId), requireTarget(portfolio),
                market.currentDate());
    }

    /** AI forecasts for the whole universe, cached per simulated day (training is the expensive part). */
    public synchronized MarketOutlook marketOutlook() {
        if (cachedOutlook == null || !market.currentDate().equals(outlookDate)) {
            cachedOutlook = trendService.outlook();
            outlookDate = market.currentDate();
        }
        return cachedOutlook;
    }

    public void onDriftAlert(DriftListener listener) {
        driftMonitor.addListener(listener);
    }

    // ---- rebalancing --------------------------------------------------------------------------

    public RebalancePlan proposeRebalance(String portfolioId, RebalancingStrategy strategy) {
        Portfolio portfolio = portfolio(portfolioId);
        Map<String, Double> prices = market.currentPrices();
        AllocationSnapshot snapshot = calculator.calculate(portfolio, prices);
        TargetAllocation target = requireTarget(portfolio);
        DriftReport drift = driftDetector.detect(portfolioId, snapshot, target, market.currentDate());
        RebalanceContext ctx = new RebalanceContext(portfolio, investor(portfolio.ownerId()), target, snapshot, drift,
                marketOutlook(), prices, List.copyOf(market.assets()), market.currentDate());
        return strategy.plan(ctx);
    }

    /** Executes a plan produced today. Trades run in plan order (sells first). */
    public void execute(RebalancePlan plan) {
        if (!plan.asOf().equals(market.currentDate())) {
            throw new IllegalStateException("plan from " + plan.asOf() + " is stale; re-plan at today's prices");
        }
        Portfolio portfolio = portfolio(plan.portfolioId());
        for (Trade trade : plan.trades()) portfolio.execute(trade, market.currentDate());
        driftMonitor.reset(plan.portfolioId());
    }

    // ---- advice -------------------------------------------------------------------------------

    public InvestmentAdvice advise(String portfolioId) {
        Portfolio portfolio = portfolio(portfolioId);
        InvestorProfile profile = investor(portfolio.ownerId());
        TargetAllocation recommended = riskProfiler.recommend(profile);
        TargetAllocation target = portfolio.targetAllocation().orElse(recommended);
        AllocationSnapshot snapshot = currentAllocation(portfolioId);
        DriftReport drift = driftDetector.detect(portfolioId, snapshot, target, market.currentDate());
        List<GoalAssessment> goals = goalPlanner.assess(profile, snapshot.totalValue(), target);
        return adviceEngine.advise(new AdviceContext(portfolio, profile, snapshot, target, recommended, drift,
                marketOutlook(), goals, market.currentPrices(), market.currentDate()));
    }

    // ---- lookups ------------------------------------------------------------------------------

    public Portfolio portfolio(String portfolioId) {
        Portfolio p = portfolios.get(portfolioId);
        if (p == null) throw new IllegalArgumentException("unknown portfolio " + portfolioId);
        return p;
    }

    public InvestorProfile investor(String investorId) {
        InvestorProfile p = investors.get(investorId);
        if (p == null) throw new IllegalArgumentException("unknown investor " + investorId);
        return p;
    }

    public MarketDataService market() { return market; }

    private static TargetAllocation requireTarget(Portfolio portfolio) {
        return portfolio.targetAllocation().orElseThrow(
                () -> new IllegalStateException("portfolio " + portfolio.id() + " has no target allocation"));
    }
}
