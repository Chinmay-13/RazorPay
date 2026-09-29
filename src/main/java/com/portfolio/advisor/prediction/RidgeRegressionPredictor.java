package com.portfolio.advisor.prediction;

import com.portfolio.advisor.market.PricePoint;
import com.portfolio.advisor.util.LinearAlgebra;

import java.util.List;

/**
 * Machine-learning model: ridge regression from technical features to the forward
 * {@code horizonDays} return, trained per symbol on that symbol's own history.
 * <p>
 * Confidence is measured honestly with a walk-forward split: the model is first fit on the
 * older 75% of samples and its directional hit-rate on the newest 25% becomes the confidence.
 * The model is then refit on all samples to produce the live forecast.
 */
public final class RidgeRegressionPredictor implements TrendPredictor {

    private static final int MIN_SAMPLES = 60;

    private final double lambda;
    private final int trainingWindow;

    public RidgeRegressionPredictor() {
        this(0.5, 500);
    }

    public RidgeRegressionPredictor(double lambda, int trainingWindow) {
        this.lambda = lambda;
        this.trainingWindow = trainingWindow;
    }

    @Override
    public String name() { return "RidgeRegression"; }

    @Override
    public TrendForecast predict(String symbol, List<PricePoint> history, int horizonDays) {
        double[] p = FeatureExtractor.prices(history);
        int last = p.length - 1;
        int firstSample = Math.max(FeatureExtractor.LOOKBACK, last - horizonDays - trainingWindow);
        int lastSample = last - horizonDays;          // the label must be fully observed
        int n = lastSample - firstSample + 1;
        if (n < MIN_SAMPLES) return TrendForecast.neutral(symbol, horizonDays, name());

        double[][] x = new double[n][];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            int t = firstSample + i;
            x[i] = FeatureExtractor.features(p, t);
            y[i] = p[t + horizonDays] / p[t] - 1;
        }

        int split = (int) (n * 0.75);
        double[] validationWeights = fit(x, y, 0, split);
        int hits = 0, evaluated = 0;
        for (int i = split; i < n; i++) {
            double predicted = LinearAlgebra.dot(validationWeights, x[i]);
            if (Math.abs(y[i]) < 1e-9) continue;
            evaluated++;
            if (Math.signum(predicted) == Math.signum(y[i])) hits++;
        }
        double hitRate = evaluated == 0 ? 0.5 : (double) hits / evaluated;
        double confidence = LinearAlgebra.clamp((hitRate - 0.5) * 2, 0, 1);

        double[] weights = fit(x, y, 0, n);
        double expected = LinearAlgebra.dot(weights, FeatureExtractor.features(p, last));
        return new TrendForecast(symbol, TrendDirection.fromExpectedReturn(expected), expected,
                confidence, horizonDays, name());
    }

    /** Closed-form ridge: w = (X'X + lambda*I)^-1 X'y over rows [from, to). Bias is not penalised. */
    double[] fit(double[][] x, double[] y, int from, int to) {
        int k = FeatureExtractor.FEATURE_COUNT;
        double[][] xtx = new double[k][k];
        double[] xty = new double[k];
        for (int r = from; r < to; r++) {
            for (int i = 0; i < k; i++) {
                xty[i] += x[r][i] * y[r];
                for (int j = 0; j < k; j++) xtx[i][j] += x[r][i] * x[r][j];
            }
        }
        for (int i = 1; i < k; i++) xtx[i][i] += lambda;
        return LinearAlgebra.solve(xtx, xty);
    }
}
