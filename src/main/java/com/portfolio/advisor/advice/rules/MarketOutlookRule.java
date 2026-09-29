package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.RiskTolerance;
import com.portfolio.advisor.prediction.TrendDirection;
import com.portfolio.advisor.prediction.TrendForecast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Surfaces confident AI forecasts on assets the investor owns. Bearish calls matter more
 * for conservative investors, so their priority is raised.
 */
public final class MarketOutlookRule implements AdviceRule {

    private static final double MIN_CONFIDENCE = 0.20;

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        List<AdviceItem> items = new ArrayList<>();
        for (Holding h : ctx.portfolio().holdings()) {
            Optional<TrendForecast> f = ctx.outlook().forecast(h.asset().symbol());
            if (f.isEmpty() || f.get().confidence() < MIN_CONFIDENCE || f.get().direction() == TrendDirection.NEUTRAL) {
                continue;
            }
            TrendForecast forecast = f.get();
            boolean bearish = forecast.direction() == TrendDirection.BEARISH;
            AdvicePriority priority = bearish && ctx.profile().riskTolerance() == RiskTolerance.CONSERVATIVE
                    ? AdvicePriority.MEDIUM : AdvicePriority.LOW;
            items.add(new AdviceItem(AdviceCategory.MARKET_OUTLOOK, priority,
                    String.format("%s outlook: %s", h.asset().symbol(), forecast.direction()),
                    String.format("Model expects %+.1f%% over %d trading days (confidence %.0f%%). %s",
                            forecast.expectedReturn() * 100, forecast.horizonDays(), forecast.confidence() * 100,
                            bearish ? "Prefer trimming this name first when rebalancing."
                                    : "Prefer adding here when the class is underweight.")));
        }
        return items;
    }
}
