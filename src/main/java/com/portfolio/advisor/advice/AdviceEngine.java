package com.portfolio.advisor.advice;

import com.portfolio.advisor.advice.rules.CashDragRule;
import com.portfolio.advisor.advice.rules.ConcentrationRule;
import com.portfolio.advisor.advice.rules.DriftRule;
import com.portfolio.advisor.advice.rules.GoalProgressRule;
import com.portfolio.advisor.advice.rules.MarketOutlookRule;
import com.portfolio.advisor.advice.rules.RiskAlignmentRule;
import com.portfolio.advisor.advice.rules.TaxLossHarvestingRule;
import com.portfolio.advisor.tax.TaxCalculator;

import java.util.Comparator;
import java.util.List;

/** Runs a chain of {@link AdviceRule}s and returns their findings, most urgent first. */
public final class AdviceEngine {

    private final List<AdviceRule> rules;

    public AdviceEngine(List<AdviceRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static AdviceEngine withDefaultRules(TaxCalculator taxCalculator) {
        return new AdviceEngine(List.of(
                new DriftRule(),
                new RiskAlignmentRule(),
                new GoalProgressRule(),
                new ConcentrationRule(),
                new TaxLossHarvestingRule(taxCalculator),
                new CashDragRule(),
                new MarketOutlookRule()));
    }

    public InvestmentAdvice advise(AdviceContext ctx) {
        List<AdviceItem> items = rules.stream()
                .flatMap(rule -> rule.evaluate(ctx).stream())
                .sorted(Comparator.comparing(AdviceItem::priority).thenComparing(AdviceItem::category))
                .toList();
        return new InvestmentAdvice(ctx.portfolio().id(), ctx.profile().investorId(), ctx.asOf(), ctx.recommended(),
                CapitalMarketAssumptions.expectedReturn(ctx.target()), CapitalMarketAssumptions.volatility(ctx.target()),
                ctx.goals(), items);
    }
}
