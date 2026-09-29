package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.drift.AssetClassDrift;
import com.portfolio.advisor.drift.DriftSeverity;

import java.util.List;
import java.util.stream.Collectors;

public final class DriftRule implements AdviceRule {

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        DriftSeverity severity = ctx.drift().severity();
        if (severity == DriftSeverity.NONE) return List.of();
        String detail = ctx.drift().drifts().stream()
                .filter(d -> d.severity() != DriftSeverity.NONE)
                .map(DriftRule::describe)
                .collect(Collectors.joining("; "));
        if (ctx.drift().rebalanceRequired()) {
            return List.of(new AdviceItem(AdviceCategory.REBALANCING, AdvicePriority.HIGH,
                    "Rebalance now (" + severity + " drift)", detail));
        }
        return List.of(new AdviceItem(AdviceCategory.REBALANCING, AdvicePriority.LOW,
                "Allocation is drifting; no action yet", detail + ". Direct new contributions to underweight classes."));
    }

    private static String describe(AssetClassDrift d) {
        return String.format("%s %.1f%% vs target %.1f%% (%+.1f pts, band %.1f pts)", d.assetClass(),
                d.currentWeight() * 100, d.targetWeight() * 100, d.absoluteDrift() * 100, d.threshold() * 100);
    }
}
