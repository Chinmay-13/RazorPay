package com.portfolio.advisor.tax;

/** A negative {@code estimatedTax} is a simulated tax saving from harvested losses. */
public record TaxImpact(double shortTermGain, double longTermGain, double estimatedTax) {

    public static final TaxImpact NONE = new TaxImpact(0, 0, 0);
}
