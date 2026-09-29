package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.advice.GoalAssessment;
import com.portfolio.advisor.model.FinancialGoal;

import java.util.ArrayList;
import java.util.List;

public final class GoalProgressRule implements AdviceRule {

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        List<AdviceItem> items = new ArrayList<>();
        for (GoalAssessment g : ctx.goals()) {
            FinancialGoal goal = g.goal();
            if (g.onTrack()) {
                items.add(new AdviceItem(AdviceCategory.GOALS, AdvicePriority.LOW, "On track: " + goal.name(),
                        String.format("Projected $%,.0f vs $%,.0f needed in %d years (%.0f%% funded).",
                                g.projectedValue(), goal.targetAmount(), goal.yearsToGoal(), g.fundedRatio() * 100)));
                continue;
            }
            AdvicePriority priority = goal.priority() == FinancialGoal.Priority.HIGH
                    ? AdvicePriority.HIGH : AdvicePriority.MEDIUM;
            String required = Double.isInfinite(g.requiredReturn()) ? "is not reachable by returns alone"
                    : String.format("needs %.1f%%/yr vs %.1f%% expected", g.requiredReturn() * 100, g.expectedReturn() * 100);
            items.add(new AdviceItem(AdviceCategory.GOALS, priority, "Shortfall: " + goal.name(),
                    String.format("Only %.0f%% funded; the goal %s. Save an extra $%,.0f/yr, extend the deadline, "
                            + "or lower the target.", g.fundedRatio() * 100, required, g.extraAnnualContribution())));
        }
        return items;
    }
}
