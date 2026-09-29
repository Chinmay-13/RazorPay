package com.portfolio.advisor.drift;

public enum DriftSeverity {
    NONE,
    MINOR,
    SIGNIFICANT,
    CRITICAL;

    public boolean atLeast(DriftSeverity other) { return compareTo(other) >= 0; }
}
