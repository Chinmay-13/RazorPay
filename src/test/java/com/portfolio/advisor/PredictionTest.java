package com.portfolio.advisor;

import com.portfolio.advisor.market.PricePoint;
import com.portfolio.advisor.prediction.EnsemblePredictor;
import com.portfolio.advisor.prediction.MomentumPredictor;
import com.portfolio.advisor.prediction.RidgeRegressionPredictor;
import com.portfolio.advisor.prediction.TrendDirection;
import com.portfolio.advisor.prediction.TrendForecast;
import com.portfolio.advisor.util.LinearAlgebra;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredictionTest {

    /** Noisy exponential series: steady trend with daily noise. */
    private static List<PricePoint> series(int days, double dailyDrift, long seed) {
        Random r = new Random(seed);
        List<PricePoint> out = new ArrayList<>();
        double price = 100;
        LocalDate d = LocalDate.of(2020, 1, 1);
        for (int i = 0; i < days; i++) {
            out.add(new PricePoint(d.plusDays(i), price));
            price *= Math.exp(dailyDrift + 0.004 * r.nextGaussian());
        }
        return out;
    }

    @Test
    void solverSolvesLinearSystem() {
        double[][] a = {{2, 1, -1}, {-3, -1, 2}, {-2, 1, 2}};
        double[] b = {8, -11, -3};
        assertArrayEquals(new double[] {2, 3, -1}, LinearAlgebra.solve(a, b), 1e-9);
    }

    @Test
    void ridgeModelLearnsAnUptrend() {
        TrendForecast f = new RidgeRegressionPredictor().predict("X", series(400, 0.002, 1), 21);
        assertEquals(TrendDirection.BULLISH, f.direction());
        assertTrue(f.confidence() > 0.5, "out-of-sample hit rate should be high on a clean trend");
    }

    @Test
    void ridgeModelLearnsADowntrend() {
        TrendForecast f = new RidgeRegressionPredictor().predict("X", series(400, -0.002, 2), 21);
        assertEquals(TrendDirection.BEARISH, f.direction());
    }

    @Test
    void momentumAndEnsembleAgreeOnClearTrend() {
        List<PricePoint> up = series(300, 0.002, 3);
        assertEquals(TrendDirection.BULLISH, new MomentumPredictor().predict("X", up, 21).direction());
        EnsemblePredictor ensemble = new EnsemblePredictor(List.of(new RidgeRegressionPredictor(), new MomentumPredictor()));
        TrendForecast f = ensemble.predict("X", up, 21);
        assertEquals(TrendDirection.BULLISH, f.direction());
        assertTrue(f.confidence() > 0);
    }

    @Test
    void tooLittleHistoryGivesNeutralZeroConfidence() {
        TrendForecast f = new RidgeRegressionPredictor().predict("X", series(50, 0.002, 4), 21);
        assertEquals(TrendDirection.NEUTRAL, f.direction());
        assertEquals(0, f.confidence());
    }
}
