package com.portfolio.advisor;

import com.portfolio.advisor.advice.InvestmentAdvice;
import com.portfolio.advisor.cost.TieredTransactionCostModel;
import com.portfolio.advisor.market.MarketDataService;
import com.portfolio.advisor.market.RegimeSwitchingPriceSimulator;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.rebalancing.RebalancePlan;
import com.portfolio.advisor.rebalancing.TaxAwareTrendRebalancer;
import com.portfolio.advisor.service.AdvisorFactory;
import com.portfolio.advisor.service.PortfolioAdvisorService;
import com.portfolio.advisor.tax.SimulatedTaxCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvisorServiceIntegrationTest {

    @Test
    void endToEndFlow() {
        MarketDataService market = new MarketDataService(new RegimeSwitchingPriceSimulator(11), LocalDate.of(2023, 1, 2));
        market.listAsset(TestFixtures.STOCK_A, 100);
        market.listAsset(TestFixtures.STOCK_B, 50);
        market.listAsset(TestFixtures.BOND_A, 100);
        market.advance(150);

        TieredTransactionCostModel costs = new TieredTransactionCostModel();
        SimulatedTaxCalculator tax = new SimulatedTaxCalculator();
        PortfolioAdvisorService advisor = AdvisorFactory.create(market, costs, tax);
        advisor.registerInvestor(TestFixtures.profile(RiskTolerance.AGGRESSIVE));
        Portfolio p = advisor.createPortfolio("I1", 100_000);
        advisor.setTargetAllocation(p.id(), TargetAllocation.of(0.8, 0.15, 0.05));

        // start fully in cash: maximal drift, rebalancing must invest it
        assertTrue(advisor.checkDrift(p.id()).rebalanceRequired());
        RebalancePlan plan = advisor.proposeRebalance(p.id(), new TaxAwareTrendRebalancer(costs, tax));
        advisor.execute(plan);
        assertFalse(advisor.checkDrift(p.id()).rebalanceRequired());
        assertEquals(plan.trades().size(), p.tradeHistory().size());

        market.advance(1);
        assertThrows(IllegalStateException.class, () -> advisor.execute(plan), "stale plans are rejected");

        InvestmentAdvice advice = advisor.advise(p.id());
        assertEquals(1, advice.goals().size());
        assertTrue(advisor.marketOutlook().bySymbol().containsKey("AAA"));
    }
}
