package com.portfolio.advisor.advice;

public record AdviceItem(AdviceCategory category, AdvicePriority priority, String title, String detail) { }
