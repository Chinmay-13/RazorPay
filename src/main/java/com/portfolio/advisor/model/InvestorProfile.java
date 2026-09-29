package com.portfolio.advisor.model;

import java.util.List;

/**
 * Who the investor is: drives the recommended allocation, the tax model and personalised advice.
 * Tax rates are simulated flat rates for short-term (&lt;= 1 year) and long-term gains.
 */
public record InvestorProfile(String investorId, String name, int age, RiskTolerance riskTolerance,
                              int investmentHorizonYears, double annualContribution,
                              double shortTermTaxRate, double longTermTaxRate,
                              List<FinancialGoal> goals) {

    public InvestorProfile {
        if (age < 18) throw new IllegalArgumentException("investor must be an adult");
        if (investmentHorizonYears <= 0) throw new IllegalArgumentException("horizon must be positive");
        if (shortTermTaxRate < 0 || shortTermTaxRate > 1 || longTermTaxRate < 0 || longTermTaxRate > 1) {
            throw new IllegalArgumentException("tax rates must be in [0,1]");
        }
        goals = List.copyOf(goals);
    }
}
