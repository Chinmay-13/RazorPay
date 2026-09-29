package com.portfolio.advisor.drift;

import com.portfolio.advisor.allocation.AllocationCalculator;
import com.portfolio.advisor.market.MarketListener;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Automated drift detection. Subscribes to market closes (Observer) and re-evaluates every
 * watched portfolio; listeners are alerted only when severity reaches SIGNIFICANT and
 * escalates beyond what was last alerted, so a portfolio that stays drifted does not spam.
 * The alert state resets once the portfolio is back inside its bands.
 */
public final class DriftMonitor implements MarketListener {

    private final AllocationCalculator calculator;
    private final DriftDetector detector;
    private final Map<String, Portfolio> watched = new ConcurrentHashMap<>();
    private final Map<String, DriftSeverity> lastAlerted = new ConcurrentHashMap<>();
    private final List<DriftListener> listeners = new CopyOnWriteArrayList<>();

    public DriftMonitor(AllocationCalculator calculator, DriftDetector detector) {
        this.calculator = calculator;
        this.detector = detector;
    }

    public void watch(Portfolio portfolio) { watched.put(portfolio.id(), portfolio); }

    public void unwatch(String portfolioId) {
        watched.remove(portfolioId);
        lastAlerted.remove(portfolioId);
    }

    public void addListener(DriftListener listener) { listeners.add(listener); }

    /** Call after a rebalance so the next breach alerts again. */
    public void reset(String portfolioId) { lastAlerted.remove(portfolioId); }

    @Override
    public void onMarketClose(LocalDate date, Map<String, Double> closingPrices) {
        for (Portfolio portfolio : watched.values()) {
            TargetAllocation target = portfolio.targetAllocation().orElse(null);
            if (target == null) continue;
            DriftReport report = detector.detect(portfolio.id(),
                    calculator.calculate(portfolio, closingPrices), target, date);
            DriftSeverity severity = report.severity();
            DriftSeverity previous = lastAlerted.getOrDefault(portfolio.id(), DriftSeverity.NONE);
            if (report.rebalanceRequired() && severity.compareTo(previous) > 0) {
                lastAlerted.put(portfolio.id(), severity);
                listeners.forEach(l -> l.onSignificantDrift(portfolio, report));
            } else if (!report.rebalanceRequired()) {
                lastAlerted.remove(portfolio.id());
            }
        }
    }
}
