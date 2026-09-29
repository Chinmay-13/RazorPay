package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.PricePoint;

import java.util.List;

/** Strategy interface: every forecasting model (rule-based, ML, ensemble) plugs in here. */
public interface TrendPredictor {

    String name();

    TrendForecast predict(String symbol, List<PricePoint> history, int horizonDays);
}
