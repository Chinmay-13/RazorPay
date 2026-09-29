package com.portfolio.advisor.advice;

import com.portfolio.advisor.model.TargetAllocation;

import java.time.LocalDate;
import java.util.List;

public record InvestmentAdvice(String portfolioId, String investorId, LocalDate asOf,
                               TargetAllocation recommendedAllocation, double expectedAnnualReturn,
                               double expectedVolatility, List<GoalAssessment> goals, List<AdviceItem> items) {

    public InvestmentAdvice {
        goals = List.copyOf(goals);
        items = List.copyOf(items);
    }
}
