package com.portfolio.advisor.tax;

import com.portfolio.advisor.model.InvestorProfile;
import com.portfolio.advisor.model.LotSale;

import java.util.List;

public interface TaxCalculator {

    TaxImpact estimate(List<LotSale> sales, InvestorProfile profile);

    /** Tax per unit if one unit of {@code costPerUnit} bought on a lot of that term is sold at {@code price}. */
    double taxPerUnit(double price, double costPerUnit, boolean longTerm, InvestorProfile profile);
}
