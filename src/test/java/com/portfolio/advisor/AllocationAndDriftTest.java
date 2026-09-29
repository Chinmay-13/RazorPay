package com.portfolio.advisor;

import com.portfolio.advisor.allocation.AllocationCalculator;
import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.drift.DriftMonitor;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.drift.DriftSeverity;
import com.portfolio.advisor.drift.ToleranceBandDriftDetector;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationAndDriftTest {

    private static final LocalDate D = LocalDate.of(2024, 6, 3);
    private final AllocationCalculator calculator = new AllocationCalculator();
    private final ToleranceBandDriftDetector detector = new ToleranceBandDriftDetector(0.05, 0.25);

    private Portfolio portfolio(double stockUnits, double bondUnits, double cash) {
        Portfolio p = new Portfolio("P", "I", stockUnits * 100 + bondUnits * 100 + cash);
        p.buy(TestFixtures.STOCK_A, stockUnits, 100, 0, D);
        p.buy(TestFixtures.BOND_A, bondUnits, 100, 0, D);
        return p;
    }

    @Test
    void calculatesWeightsPerClass() {
        AllocationSnapshot s = calculator.calculate(portfolio(60, 30, 1_000),
                Map.of("AAA", 100.0, "BND", 100.0));
        assertEquals(10_000, s.totalValue(), 1e-9);
        assertEquals(0.60, s.weight(AssetClass.STOCK), 1e-9);
        assertEquals(0.30, s.weight(AssetClass.BOND), 1e-9);
        assertEquals(0.10, s.weight(AssetClass.CASH), 1e-9);
    }

    @Test
    void appliesAbsoluteAndRelativeBands() {
        TargetAllocation target = TargetAllocation.of(0.60, 0.35, 0.05);
        // stock rallies 30%: 78/(78+35+5) = 66.1% -> +6.1 pts, beyond the 5 pt band
        AllocationSnapshot s = calculator.calculate(portfolio(60, 35, 500), Map.of("AAA", 130.0, "BND", 100.0));
        DriftReport report = detector.detect("P", s, target, D);
        assertEquals(DriftSeverity.SIGNIFICANT, report.severity());
        assertTrue(report.rebalanceRequired());

        // cash at 7% vs 5% target is only 2 pts but 40% relative -> significant under the 25% leg
        AllocationSnapshot cashHeavy = calculator.calculate(portfolio(58, 35, 700), Map.of("AAA", 100.0, "BND", 100.0));
        DriftReport cashReport = detector.detect("P", cashHeavy, target, D);
        assertEquals(DriftSeverity.SIGNIFICANT, cashReport.drifts().stream()
                .filter(d -> d.assetClass() == AssetClass.CASH).findFirst().orElseThrow().severity());
    }

    @Test
    void onTargetPortfolioHasNoDrift() {
        AllocationSnapshot s = calculator.calculate(portfolio(60, 35, 500), Map.of("AAA", 100.0, "BND", 100.0));
        DriftReport report = detector.detect("P", s, TargetAllocation.of(0.60, 0.35, 0.05), D);
        assertEquals(DriftSeverity.NONE, report.severity());
        assertFalse(report.rebalanceRequired());
    }

    @Test
    void monitorAlertsOnceOnEscalationAndRearmsAfterRecovery() {
        Portfolio p = portfolio(60, 35, 500);
        p.setTargetAllocation(TargetAllocation.of(0.60, 0.35, 0.05));
        DriftMonitor monitor = new DriftMonitor(calculator, detector);
        monitor.watch(p);
        List<DriftSeverity> alerts = new ArrayList<>();
        monitor.addListener((portfolio, report) -> alerts.add(report.severity()));

        monitor.onMarketClose(D, Map.of("AAA", 130.0, "BND", 100.0));   // significant -> alert
        monitor.onMarketClose(D, Map.of("AAA", 131.0, "BND", 100.0));   // still significant -> silent
        monitor.onMarketClose(D, Map.of("AAA", 200.0, "BND", 100.0));   // critical -> alert
        monitor.onMarketClose(D, Map.of("AAA", 100.0, "BND", 100.0));   // back in band -> re-arm
        monitor.onMarketClose(D, Map.of("AAA", 130.0, "BND", 100.0));   // significant again -> alert

        assertEquals(List.of(DriftSeverity.SIGNIFICANT, DriftSeverity.CRITICAL, DriftSeverity.SIGNIFICANT), alerts);
    }
}
