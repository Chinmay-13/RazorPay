package com.portfolio.advisor.drift;

import com.portfolio.advisor.model.Portfolio;

@FunctionalInterface
public interface DriftListener {

    void onSignificantDrift(Portfolio portfolio, DriftReport report);
}
