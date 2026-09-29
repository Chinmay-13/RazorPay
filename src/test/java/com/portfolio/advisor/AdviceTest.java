package com.portfolio.advisor;

import com.portfolio.advisor.advice.GoalAssessment;
import com.portfolio.advisor.advice.GoalPlanner;
import com.portfolio.advisor.advice.RiskProfiler;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.FinancialGoal;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.TargetAllocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdviceTest {

    @Test
    void riskierProfilesGetMoreEquity() {
        RiskProfiler profiler = new RiskProfiler();
        double conservative = profiler.recommend(TestFixtures.profile(RiskTolerance.CONSERVATIVE)).weight(AssetClass.STOCK);
        double moderate = profiler.recommend(TestFixtures.profile(RiskTolerance.MODERATE)).weight(AssetClass.STOCK);
        double aggressive = profiler.recommend(TestFixtures.profile(RiskTolerance.AGGRESSIVE)).weight(AssetClass.STOCK);
        assertTrue(conservative < moderate && moderate < aggressive);
    }

    @Test
    void shortHorizonDeRisks() {
        RiskProfiler profiler = new RiskProfiler();
        InvestorProfile longHorizon = TestFixtures.profile(RiskTolerance.MODERATE);
        InvestorProfile shortHorizon = new InvestorProfile("I2", "T", 35, RiskTolerance.MODERATE, 2, 0, 0.3, 0.15, List.of());
        assertTrue(profiler.recommend(shortHorizon).weight(AssetClass.STOCK)
                < profiler.recommend(longHorizon).weight(AssetClass.STOCK));
    }

    @Test
    void goalPlannerFlagsShortfallAndComputesRequiredReturn() {
        InvestorProfile p = new InvestorProfile("I", "T", 30, RiskTolerance.MODERATE, 10, 0, 0.3, 0.15,
                List.of(new FinancialGoal("Big", 1_000_000, 10, FinancialGoal.Priority.HIGH),
                        new FinancialGoal("Small", 10_000, 1, FinancialGoal.Priority.LOW)));
        List<GoalAssessment> result = new GoalPlanner().assess(p, 100_000, TargetAllocation.of(0.6, 0.35, 0.05));
        assertEquals("Small", result.get(0).goal().name(), "goals are checked in date order");
        assertTrue(result.get(0).onTrack());
        GoalAssessment big = result.get(1);
        assertFalse(big.onTrack());
        // ~90k left after year 1 must grow 11x in 9 years -> roughly 30%/yr
        assertTrue(big.requiredReturn() > 0.25 && big.requiredReturn() < 0.35, "was " + big.requiredReturn());
        assertTrue(big.extraAnnualContribution() > 0);
    }
}
