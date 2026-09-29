package com.portfolio.advisor.market;

import com.portfolio.advisor.model.Asset;

import java.util.Collection;
import java.util.Map;

/** Strategy for generating the next trading day's prices for every listed asset. */
public interface PriceSimulator {

    Map<String, Double> nextPrices(Collection<Asset> assets, Map<String, Double> currentPrices);
}
