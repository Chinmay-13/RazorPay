package com.portfolio.advisor.advice;

import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.util.LinearAlgebra;

/**
 * Maps an investor profile to a recommended strategic allocation by blending a
 * risk-tolerance model portfolio with an age-based glide path ("110 - age"),
 * then de-risking for short horizons.
 */
public final class RiskProfiler {

    public TargetAllocation recommend(InvestorProfile profile) {
        double toleranceStock = switch (profile.riskTolerance()) {
            case CONSERVATIVE -> 0.35;
            case MODERATE -> 0.60;
            case AGGRESSIVE -> 0.80;
        };
        double ageStock = LinearAlgebra.clamp((110 - profile.age()) / 100.0, 0.20, 0.90);
        double stock = 0.6 * toleranceStock + 0.4 * ageStock;

        int horizon = profile.investmentHorizonYears();
        if (horizon < 3) stock -= 0.20;
        else if (horizon < 7) stock -= 0.10;
        else if (horizon >= 15) stock += 0.05;
        stock = LinearAlgebra.clamp(stock, 0.10, 0.90);

        double cash = switch (profile.riskTolerance()) {
            case CONSERVATIVE -> 0.08;
            case MODERATE -> 0.05;
            case AGGRESSIVE -> 0.03;
        };
        if (horizon < 3) cash += 0.05;

        stock = Math.round(stock * 100) / 100.0;
        cash = Math.round(cash * 100) / 100.0;
        return TargetAllocation.of(stock, 1.0 - stock - cash, cash);
    }
}
