package com.portfolio.advisor.advice;

import com.portfolio.advisor.model.FinancialGoal;

/**
 * @param requiredReturn annual return needed to hit the goal exactly (infinite if unreachable)
 * @param extraAnnualContribution extra yearly saving needed at the expected return (0 if on track)
 */
public record GoalAssessment(FinancialGoal goal, double projectedValue, double fundedRatio,
                             double expectedReturn, double requiredReturn, double extraAnnualContribution) {

    public boolean onTrack() { return fundedRatio >= 1.0; }
}
