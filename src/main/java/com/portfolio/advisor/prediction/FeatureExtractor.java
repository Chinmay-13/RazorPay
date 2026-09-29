package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.PricePoint;

import java.util.List;

/**
 * Turns a price series into the technical features used by the ML model.
 * Feature vector: [bias, ret5, ret20, ret60, maGap(10/50), vol20, rsi14].
 */
public final class FeatureExtractor {

    public static final int LOOKBACK = 60;
    public static final int FEATURE_COUNT = 7;
    public static final String[] FEATURE_NAMES =
            {"bias", "ret5", "ret20", "ret60", "maGap10_50", "vol20", "rsi14"};

    private FeatureExtractor() { }

    public static double[] prices(List<PricePoint> history) {
        return history.stream().mapToDouble(PricePoint::price).toArray();
    }

    /** Features observable at the close of day {@code t} (uses only data up to and including t). */
    public static double[] features(double[] p, int t) {
        if (t < LOOKBACK) throw new IllegalArgumentException("need " + LOOKBACK + " days of history");
        return new double[] {
                1.0,
                p[t] / p[t - 5] - 1,
                p[t] / p[t - 20] - 1,
                p[t] / p[t - 60] - 1,
                mean(p, t, 10) / mean(p, t, 50) - 1,
                volatility(p, t, 20),
                (rsi(p, t, 14) - 50) / 50
        };
    }

    static double mean(double[] p, int t, int window) {
        double s = 0;
        for (int i = t - window + 1; i <= t; i++) s += p[i];
        return s / window;
    }

    static double volatility(double[] p, int t, int window) {
        double[] r = new double[window];
        double mean = 0;
        for (int i = 0; i < window; i++) {
            r[i] = Math.log(p[t - i] / p[t - i - 1]);
            mean += r[i];
        }
        mean /= window;
        double var = 0;
        for (double x : r) var += (x - mean) * (x - mean);
        return Math.sqrt(var / (window - 1));
    }

    static double rsi(double[] p, int t, int window) {
        double gain = 0, loss = 0;
        for (int i = t - window + 1; i <= t; i++) {
            double change = p[i] - p[i - 1];
            if (change > 0) gain += change; else loss -= change;
        }
        if (loss == 0) return 100;
        double rs = gain / loss;
        return 100 - 100 / (1 + rs);
    }
}
