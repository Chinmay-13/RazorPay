package com.portfolio.advisor.advice;

import com.portfolio.advisor.allocation.AllocationSnapshot;
import com.portfolio.advisor.drift.DriftReport;
import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.prediction.MarketOutlook;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdviceContext(Portfolio portfolio, InvestorProfile profile, AllocationSnapshot snapshot,
                            TargetAllocation target, TargetAllocation recommended, DriftReport drift,
                            MarketOutlook outlook, List<GoalAssessment> goals, Map<String, Double> prices,
                            LocalDate asOf) { }
