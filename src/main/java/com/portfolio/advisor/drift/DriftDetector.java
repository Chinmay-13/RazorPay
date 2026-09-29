package com.portfolio.advisor.drift;

import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.model.TargetAllocation;

import java.time.LocalDate;

public interface DriftDetector {

    DriftReport detect(String portfolioId, AllocationSnapshot snapshot, TargetAllocation target, LocalDate asOf);
}
