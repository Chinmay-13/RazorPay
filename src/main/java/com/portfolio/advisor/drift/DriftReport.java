package com.portfolio.advisor.drift;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record DriftReport(String portfolioId, LocalDate asOf, List<AssetClassDrift> drifts) {

    public DriftReport {
        drifts = List.copyOf(drifts);
    }

    public DriftSeverity severity() {
        return drifts.stream().map(AssetClassDrift::severity)
                .max(Comparator.naturalOrder()).orElse(DriftSeverity.NONE);
    }

    public boolean rebalanceRequired() { return severity().atLeast(DriftSeverity.SIGNIFICANT); }

    public double maxAbsoluteDrift() {
        return drifts.stream().mapToDouble(d -> Math.abs(d.absoluteDrift())).max().orElse(0);
    }
}
