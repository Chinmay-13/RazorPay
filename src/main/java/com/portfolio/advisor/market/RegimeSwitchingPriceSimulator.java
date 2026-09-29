package com.portfolio.advisor.market;

import com.portfolio.advisor.model.Asset;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Geometric Brownian motion with a shared market factor and a Markov-switching regime.
 * <pre>
 *   r_i = (mu_i + beta_i * regimeDrift) * dt + beta_i * sigma_m * sqrt(dt) * Z_m + sigma_i * sqrt(dt) * Z_i
 *   P_i(t+1) = P_i(t) * exp(r_i - sigma_i^2 * dt / 2)
 * </pre>
 * Seeded, so every run of the demo and the tests is reproducible.
 */
public final class RegimeSwitchingPriceSimulator implements PriceSimulator {

    private static final double DT = 1.0 / 252;
    private static final double MARKET_VOLATILITY = 0.12;

    private final Random random;
    private final double regimeStayProbability;
    private MarketRegime regime;

    public RegimeSwitchingPriceSimulator(long seed) {
        this(seed, 0.985, MarketRegime.BULL);
    }

    public RegimeSwitchingPriceSimulator(long seed, double regimeStayProbability, MarketRegime initialRegime) {
        this.random = new Random(seed);
        this.regimeStayProbability = regimeStayProbability;
        this.regime = initialRegime;
    }

    @Override
    public Map<String, Double> nextPrices(Collection<Asset> assets, Map<String, Double> currentPrices) {
        maybeSwitchRegime();
        double marketShock = random.nextGaussian();
        double sqrtDt = Math.sqrt(DT);
        Map<String, Double> next = new HashMap<>();
        for (Asset asset : assets) {
            double beta = asset.regimeSensitivity();
            double sigma = asset.annualVolatility();
            double r = (asset.annualDrift() + beta * regime.annualDriftShift()) * DT
                    + beta * MARKET_VOLATILITY * sqrtDt * marketShock
                    + sigma * sqrtDt * random.nextGaussian();
            double price = currentPrices.get(asset.symbol()) * Math.exp(r - 0.5 * sigma * sigma * DT);
            next.put(asset.symbol(), Math.max(0.01, price));
        }
        return next;
    }

    private void maybeSwitchRegime() {
        if (random.nextDouble() < regimeStayProbability) return;
        MarketRegime[] all = MarketRegime.values();
        MarketRegime candidate;
        do {
            candidate = all[random.nextInt(all.length)];
        } while (candidate == regime);
        regime = candidate;
    }

    public MarketRegime currentRegime() { return regime; }
}
