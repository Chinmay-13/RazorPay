package com.portfolio.advisor.market;

import java.time.LocalDate;
import java.util.Map;

/** Observer notified after each simulated trading day. */
@FunctionalInterface
public interface MarketListener {

    void onMarketClose(LocalDate date, Map<String, Double> closingPrices);
}
