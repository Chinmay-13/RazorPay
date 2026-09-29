package com.portfolio.advisor.advice;

import com.portfolio.advisor.model.FinancialGoal;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.TargetAllocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Projects the portfolio forward and checks goals in date order: each goal is paid out of the
 * projected value, and whatever is left keeps compounding toward the next goal.
 */
public final class GoalPlanner {

    public List<GoalAssessment> assess(InvestorProfile profile, double portfolioValue, TargetAllocation allocation) {
        double r = CapitalMarketAssumptions.expectedReturn(allocation);
        double contribution = profile.annualContribution();
        List<FinancialGoal> goals = profile.goals().stream()
                .sorted(Comparator.comparingInt(FinancialGoal::yearsToGoal).thenComparing(FinancialGoal::priority))
                .toList();

        List<GoalAssessment> result = new ArrayList<>();
        double available = portfolioValue;
        int elapsed = 0;
        for (FinancialGoal goal : goals) {
            int years = Math.max(1, goal.yearsToGoal() - elapsed);
            double projected = futureValue(available, contribution, r, years);
            double required = requiredReturn(available, contribution, years, goal.targetAmount());
            double shortfall = Math.max(0, goal.targetAmount() - projected);
            double extra = shortfall / annuityFactor(r, years);
            result.add(new GoalAssessment(goal, projected, projected / goal.targetAmount(), r, required, extra));
            available = Math.max(0, projected - goal.targetAmount());
            elapsed = goal.yearsToGoal();
        }
        return result;
    }

    static double futureValue(double presentValue, double annualContribution, double r, int years) {
        return presentValue * Math.pow(1 + r, years) + annualContribution * annuityFactor(r, years);
    }

    static double annuityFactor(double r, int years) {
        return Math.abs(r) < 1e-9 ? years : (Math.pow(1 + r, years) - 1) / r;
    }

    /** Bisection for the rate at which the future value equals the target. */
    static double requiredReturn(double pv, double contribution, int years, double target) {
        double lo = -0.5, hi = 1.0;
        if (futureValue(pv, contribution, hi, years) < target) return Double.POSITIVE_INFINITY;
        if (futureValue(pv, contribution, lo, years) >= target) return lo;
        for (int i = 0; i < 100; i++) {
            double mid = (lo + hi) / 2;
            if (futureValue(pv, contribution, mid, years) >= target) hi = mid; else lo = mid;
        }
        return hi;
    }
}
