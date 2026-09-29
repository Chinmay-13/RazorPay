package com.portfolio.advisor.service;

import com.portfolio.advisor.advice.AdviceEngine;
import com.portfolio.advisor.advice.GoalPlanner;
import com.portfolio.advisor.advice.RiskProfiler;
import com.portfolio.advisor.allocation.AllocationCalculator;
import com.portfolio.advisor.cost.TransactionCostModel;
import com.portfolio.advisor.drift.DriftDetector;
import com.portfolio.advisor.drift.ToleranceBandDriftDetector;
import com.portfolio.advisor.market.MarketDataService;
import com.portfolio.advisor.prediction.EnsemblePredictor;
import com.portfolio.advisor.prediction.MomentumPredictor;
import com.portfolio.advisor.prediction.RidgeRegressionPredictor;
import com.portfolio.advisor.prediction.TrendAnalysisService;
import com.portfolio.advisor.tax.TaxCalculator;

import java.util.List;

/** Composition root: the one place that picks concrete implementations. */
public final class AdvisorFactory {

    public static final int FORECAST_HORIZON_DAYS = 21;

    private AdvisorFactory() { }

    public static PortfolioAdvisorService create(MarketDataService market, TransactionCostModel costModel,
                                                 TaxCalculator taxCalculator) {
        DriftDetector detector = new ToleranceBandDriftDetector(0.05, 0.25);
        EnsemblePredictor predictor = new EnsemblePredictor(List.of(new RidgeRegressionPredictor(), new MomentumPredictor()));
        return new PortfolioAdvisorService(market, new AllocationCalculator(), detector,
                new TrendAnalysisService(market, predictor, FORECAST_HORIZON_DAYS), costModel,
                new RiskProfiler(), new GoalPlanner(), AdviceEngine.withDefaultRules(taxCalculator));
    }
}
