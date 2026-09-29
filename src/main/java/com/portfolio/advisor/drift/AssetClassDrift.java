package com.portfolio.advisor.drift;

import com.portfolio.advisor.model.AssetClass;

/**
 * @param absoluteDrift current - target, in weight units (0.05 = 5 percentage points)
 * @param relativeDrift absoluteDrift / target
 * @param threshold     the band that was applied to this class
 */
public record AssetClassDrift(AssetClass assetClass, double currentWeight, double targetWeight,
                              double absoluteDrift, double relativeDrift, double threshold,
                              DriftSeverity severity) { }
