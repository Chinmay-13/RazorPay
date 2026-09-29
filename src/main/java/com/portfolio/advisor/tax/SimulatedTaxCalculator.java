package com.portfolio.advisor.tax;

import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.LotSale;

import java.util.List;

/**
 * Simplified capital-gains model. Short- and long-term results are netted within their own
 * bucket, then a net loss in one bucket offsets gains in the other before rates apply.
 * Any remaining net loss is credited at the long-term rate as a (simulated) tax saving.
 */
public final class SimulatedTaxCalculator implements TaxCalculator {

    @Override
    public TaxImpact estimate(List<LotSale> sales, InvestorProfile profile) {
        double st = 0, lt = 0;
        for (LotSale sale : sales) {
            if (sale.longTerm()) lt += sale.gain(); else st += sale.gain();
        }
        double stTaxable = st, ltTaxable = lt;
        if (st < 0 && lt > 0) {
            double offset = Math.min(-st, lt);
            stTaxable += offset;
            ltTaxable -= offset;
        } else if (lt < 0 && st > 0) {
            double offset = Math.min(-lt, st);
            ltTaxable += offset;
            stTaxable -= offset;
        }
        double tax = Math.max(0, stTaxable) * profile.shortTermTaxRate()
                + Math.max(0, ltTaxable) * profile.longTermTaxRate()
                + Math.min(0, stTaxable + ltTaxable) * profile.longTermTaxRate();
        return new TaxImpact(st, lt, tax);
    }

    @Override
    public double taxPerUnit(double price, double costPerUnit, boolean longTerm, InvestorProfile profile) {
        double rate = longTerm ? profile.longTermTaxRate() : profile.shortTermTaxRate();
        return (price - costPerUnit) * rate;
    }
}
