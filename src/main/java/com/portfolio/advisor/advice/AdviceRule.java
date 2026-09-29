package com.portfolio.advisor.advice;

import java.util.List;

/** One independent piece of advisory logic. New advice = new rule; the engine never changes. */
@FunctionalInterface
public interface AdviceRule {

    List<AdviceItem> evaluate(AdviceContext context);
}
