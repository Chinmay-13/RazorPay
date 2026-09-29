package com.portfolio.advisor.model;

public record FinancialGoal(String name, double targetAmount, int yearsToGoal, Priority priority) {

    public enum Priority { HIGH, MEDIUM, LOW }

    public FinancialGoal {
        if (targetAmount <= 0) throw new IllegalArgumentException("target amount must be positive");
        if (yearsToGoal <= 0) throw new IllegalArgumentException("years to goal must be positive");
    }
}
