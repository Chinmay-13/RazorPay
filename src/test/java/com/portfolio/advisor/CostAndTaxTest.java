package com.portfolio.advisor;

import com.portfolio.advisor.cost.TieredTransactionCostModel;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.model.TradeSide;
import com.portfolio.advisor.tax.FifoLotSelection;
import com.portfolio.advisor.tax.SimulatedTaxCalculator;
import com.portfolio.advisor.tax.TaxEfficientLotSelection;
import com.portfolio.advisor.tax.TaxImpact;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostAndTaxTest {

    private final SimulatedTaxCalculator tax = new SimulatedTaxCalculator();
    private final InvestorProfile profile = TestFixtures.profile(RiskTolerance.MODERATE);   // ST 30%, LT 15%
    private final LocalDate today = LocalDate.of(2025, 6, 2);

    @Test
    void costModelChargesFlatFeePlusBps() {
        TieredTransactionCostModel model = new TieredTransactionCostModel();
        // stock: 5 bps commission + 2 bps half-spread on $10,000 = $7, plus $1 ticket
        assertEquals(8.0, model.estimate(TestFixtures.STOCK_A, TradeSide.BUY, 100, 100), 1e-9);
        // bond: 10 + 15 bps on $10,000 = $25, plus $1
        assertEquals(26.0, model.estimate(TestFixtures.BOND_A, TradeSide.SELL, 100, 100), 1e-9);
    }

    @Test
    void shortAndLongTermGainsAreTaxedAtTheirRates() {
        Holding h = new Holding(TestFixtures.STOCK_A);
        h.addLot(10, 100, LocalDate.of(2023, 1, 2));   // long-term
        h.addLot(10, 100, LocalDate.of(2025, 3, 3));   // short-term
        List<LotSale> sales = new FifoLotSelection().select(h, 20, 150, today, profile);
        TaxImpact impact = tax.estimate(sales, profile);
        assertEquals(500, impact.longTermGain(), 1e-9);
        assertEquals(500, impact.shortTermGain(), 1e-9);
        assertEquals(500 * 0.15 + 500 * 0.30, impact.estimatedTax(), 1e-9);
    }

    @Test
    void lossesOffsetGainsAndNetLossIsASaving() {
        Holding h = new Holding(TestFixtures.STOCK_A);
        h.addLot(10, 200, LocalDate.of(2025, 3, 3));   // short-term loss of 500 at 150
        h.addLot(10, 120, LocalDate.of(2023, 1, 2));   // long-term gain of 300 at 150
        List<LotSale> sales = new FifoLotSelection().select(h, 20, 150, today, profile);
        TaxImpact impact = tax.estimate(sales, profile);
        assertEquals(-200 * 0.15, impact.estimatedTax(), 1e-9);
    }

    @Test
    void taxEfficientSelectionSellsLossesFirstAndShortTermGainsLast() {
        Holding h = new Holding(TestFixtures.STOCK_A);
        h.addLot(10, 50, LocalDate.of(2025, 3, 3));    // short-term big gain  -> last
        h.addLot(10, 60, LocalDate.of(2023, 1, 2));    // long-term gain       -> second
        h.addLot(10, 150, LocalDate.of(2025, 2, 3));   // short-term loss      -> first
        TaxEfficientLotSelection selection = new TaxEfficientLotSelection(tax);
        List<LotSale> sales = selection.select(h, 15, 100, today, profile);
        assertEquals(150, sales.get(0).lot().costPerUnit());
        assertEquals(60, sales.get(1).lot().costPerUnit());
        assertEquals(5, sales.get(1).quantity(), 1e-9);

        double fifoTax = tax.estimate(new FifoLotSelection().select(h, 15, 100, today, profile), profile).estimatedTax();
        double smartTax = tax.estimate(sales, profile).estimatedTax();
        assertTrue(smartTax < fifoTax);
    }
}
