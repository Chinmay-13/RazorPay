package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.model.AssetClass;

import java.util.List;

/** Flags a user-chosen target that does not match their risk tolerance, age and horizon. */
public final class RiskAlignmentRule implements AdviceRule {

    private static final double MISMATCH = 0.10;

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        double chosen = ctx.target().weight(AssetClass.STOCK);
        double suggested = ctx.recommended().weight(AssetClass.STOCK);
        double gap = chosen - suggested;
        if (Math.abs(gap) < MISMATCH) return List.of();
        String direction = gap > 0 ? "more aggressive" : "more conservative";
        return List.of(new AdviceItem(AdviceCategory.RISK, AdvicePriority.MEDIUM,
                "Target is " + direction + " than your profile",
                String.format("You target %.0f%% stocks; for a %d-year-old %s investor with a %d-year horizon we "
                                + "suggest %s.", chosen * 100, ctx.profile().age(), ctx.profile().riskTolerance(),
                        ctx.profile().investmentHorizonYears(), ctx.recommended())));
    }
}
