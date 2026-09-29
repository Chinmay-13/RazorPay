package com.portfolio.advisor.drift;

import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.TargetAllocation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The "5/25 rule": a class has drifted significantly when it is off target by more than
 * {@code absoluteBand} percentage points OR by more than {@code relativeBand} of its own
 * target, whichever is tighter. The relative leg protects small allocations (a 4% cash
 * target should not be allowed to drift to 9%).
 * <ul>
 *   <li>MINOR       - beyond half the band (watch)</li>
 *   <li>SIGNIFICANT - beyond the band (rebalance)</li>
 *   <li>CRITICAL    - beyond twice the band (rebalance even if taxes are high)</li>
 * </ul>
 */
public final class ToleranceBandDriftDetector implements DriftDetector {

    private final double absoluteBand;
    private final double relativeBand;

    public ToleranceBandDriftDetector() {
        this(0.05, 0.25);
    }

    public ToleranceBandDriftDetector(double absoluteBand, double relativeBand) {
        if (absoluteBand <= 0 || relativeBand <= 0) throw new IllegalArgumentException("bands must be positive");
        this.absoluteBand = absoluteBand;
        this.relativeBand = relativeBand;
    }

    @Override
    public DriftReport detect(String portfolioId, AllocationSnapshot snapshot, TargetAllocation target, LocalDate asOf) {
        List<AssetClassDrift> drifts = new ArrayList<>();
        for (AssetClass ac : AssetClass.values()) {
            double current = snapshot.weight(ac);
            double desired = target.weight(ac);
            double abs = current - desired;
            double rel = desired > 0 ? abs / desired : (abs > 0 ? Double.POSITIVE_INFINITY : 0);
            double threshold = desired > 0 ? Math.min(absoluteBand, relativeBand * desired) : absoluteBand;
            drifts.add(new AssetClassDrift(ac, current, desired, abs, rel, threshold, classify(Math.abs(abs), threshold)));
        }
        return new DriftReport(portfolioId, asOf, drifts);
    }

    private static DriftSeverity classify(double drift, double threshold) {
        if (drift > 2 * threshold) return DriftSeverity.CRITICAL;
        if (drift > threshold) return DriftSeverity.SIGNIFICANT;
        if (drift > threshold / 2) return DriftSeverity.MINOR;
        return DriftSeverity.NONE;
    }

    public double absoluteBand() { return absoluteBand; }
}
