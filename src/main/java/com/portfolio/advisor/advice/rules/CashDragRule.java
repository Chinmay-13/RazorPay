package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.model.AssetClass;

import java.util.List;

public final class CashDragRule implements AdviceRule {

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        double excess = ctx.snapshot().weight(AssetClass.CASH) - ctx.target().weight(AssetClass.CASH);
        if (excess < 0.03) return List.of();
        return List.of(new AdviceItem(AdviceCategory.CASH, AdvicePriority.LOW, "Idle cash",
                String.format("$%,.0f (%.1f%% of the portfolio) is above your cash target and earning little.",
                        excess * ctx.snapshot().totalValue(), excess * 100)));
    }
}
