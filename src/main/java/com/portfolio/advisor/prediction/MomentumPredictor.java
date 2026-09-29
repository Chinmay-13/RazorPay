package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.PricePoint;
import com.portfolio.advisor.util.LinearAlgebra;

import java.util.List;

/**
 * Rule-based baseline: moving-average crossover confirmed by 20-day momentum,
 * damped when RSI signals the move is overextended.
 */
public final class MomentumPredictor implements TrendPredictor {

    @Override
    public String name() { return "Momentum"; }

    @Override
    public TrendForecast predict(String symbol, List<PricePoint> history, int horizonDays) {
        double[] p = FeatureExtractor.prices(history);
        int t = p.length - 1;
        if (t < FeatureExtractor.LOOKBACK) return TrendForecast.neutral(symbol, horizonDays, name());

        double[] f = FeatureExtractor.features(p, t);
        double ret20 = f[2];
        double maGap = f[4];
        double rsi = f[6] * 50 + 50;

        // Extrapolate half of recent momentum over the horizon.
        double expected = 0.5 * ret20 * horizonDays / 20.0;
        boolean signalsAgree = Math.signum(maGap) == Math.signum(ret20);
        if (!signalsAgree) expected *= 0.25;
        if ((rsi > 75 && expected > 0) || (rsi < 25 && expected < 0)) expected *= 0.5;

        double confidence = LinearAlgebra.clamp(Math.abs(maGap) * 8, 0, 0.8) * (signalsAgree ? 1 : 0.3);
        return new TrendForecast(symbol, TrendDirection.fromExpectedReturn(expected), expected,
                confidence, horizonDays, name());
    }
}
