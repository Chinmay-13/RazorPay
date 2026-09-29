package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.model.RiskTolerance;

import java.util.List;
import java.util.Map;

/** Single-name risk: no one asset should dominate, tighter limit for conservative investors. */
public final class ConcentrationRule implements AdviceRule {

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        double limit = ctx.profile().riskTolerance() == RiskTolerance.CONSERVATIVE ? 0.15 : 0.25;
        return ctx.snapshot().assetValues().entrySet().stream()
                .filter(e -> ctx.snapshot().assetWeight(e.getKey()) > limit)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> new AdviceItem(AdviceCategory.DIVERSIFICATION, AdvicePriority.MEDIUM,
                        "Concentrated position in " + e.getKey(),
                        String.format("%s is %.1f%% of the portfolio (limit %.0f%% for %s investors).", e.getKey(),
                                ctx.snapshot().assetWeight(e.getKey()) * 100, limit * 100, ctx.profile().riskTolerance())))
                .toList();
    }
}
