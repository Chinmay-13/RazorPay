package com.portfolio.advisor;

import com.portfolio.advisor.model.Bond;
import com.portfolio.advisor.model.FinancialGoal;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.Stock;

import java.util.List;

final class TestFixtures {

    static final Stock STOCK_A = new Stock("AAA", "Alpha", "Tech", 1.0, 0.08, 0.2);
    static final Stock STOCK_B = new Stock("BBB", "Beta", "Health", 1.0, 0.08, 0.2);
    static final Bond BOND_A = new Bond("BND", "Bond", 0.04, 10, "AAA", 0.03, 0.05);

    private TestFixtures() { }

    static InvestorProfile profile(RiskTolerance tolerance) {
        return new InvestorProfile("I1", "Test", 35, tolerance, 20, 10_000, 0.30, 0.15,
                List.of(new FinancialGoal("Retire", 1_000_000, 25, FinancialGoal.Priority.HIGH)));
    }
}
