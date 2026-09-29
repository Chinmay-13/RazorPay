package com.portfolio.advisor.rebalancing;

public interface RebalancingStrategy {

    String name();

    RebalancePlan plan(RebalanceContext context);
}
