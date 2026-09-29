package com.portfolio.advisor.market;

import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.AssetClass;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Owns the simulated clock, the asset universe and the full price history. Subject for {@link MarketListener}s. */
public final class MarketDataService {

    private final PriceSimulator simulator;
    private final Map<String, Asset> assets = new LinkedHashMap<>();
    private final Map<String, List<PricePoint>> history = new HashMap<>();
    private final List<MarketListener> listeners = new CopyOnWriteArrayList<>();
    private LocalDate currentDate;

    public MarketDataService(PriceSimulator simulator, LocalDate startDate) {
        this.simulator = simulator;
        this.currentDate = startDate;
    }

    public void listAsset(Asset asset, double initialPrice) {
        if (initialPrice <= 0) throw new IllegalArgumentException("initial price must be positive");
        if (assets.putIfAbsent(asset.symbol(), asset) != null) {
            throw new IllegalArgumentException("asset already listed: " + asset.symbol());
        }
        List<PricePoint> series = new ArrayList<>();
        series.add(new PricePoint(currentDate, initialPrice));
        history.put(asset.symbol(), series);
    }

    /** Simulates {@code tradingDays} business days, notifying listeners after each close. */
    public void advance(int tradingDays) {
        for (int i = 0; i < tradingDays; i++) {
            currentDate = nextBusinessDay(currentDate);
            Map<String, Double> next = simulator.nextPrices(assets.values(), currentPrices());
            next.forEach((symbol, price) -> history.get(symbol).add(new PricePoint(currentDate, price)));
            Map<String, Double> closing = Collections.unmodifiableMap(next);
            listeners.forEach(l -> l.onMarketClose(currentDate, closing));
        }
    }

    private static LocalDate nextBusinessDay(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (next.getDayOfWeek() == DayOfWeek.SATURDAY || next.getDayOfWeek() == DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return next;
    }

    public double price(String symbol) {
        List<PricePoint> series = history.get(symbol);
        if (series == null) throw new IllegalArgumentException("unknown symbol " + symbol);
        return series.get(series.size() - 1).price();
    }

    public Map<String, Double> currentPrices() {
        Map<String, Double> prices = new HashMap<>();
        history.forEach((symbol, series) -> prices.put(symbol, series.get(series.size() - 1).price()));
        return prices;
    }

    public List<PricePoint> history(String symbol) {
        List<PricePoint> series = history.get(symbol);
        if (series == null) throw new IllegalArgumentException("unknown symbol " + symbol);
        return Collections.unmodifiableList(series);
    }

    public Asset asset(String symbol) {
        Asset asset = assets.get(symbol);
        if (asset == null) throw new IllegalArgumentException("unknown symbol " + symbol);
        return asset;
    }

    public Collection<Asset> assets() { return Collections.unmodifiableCollection(assets.values()); }

    public List<Asset> assetsOf(AssetClass assetClass) {
        return assets.values().stream().filter(a -> a.assetClass() == assetClass).toList();
    }

    public LocalDate currentDate() { return currentDate; }

    public void addListener(MarketListener listener) { listeners.add(listener); }
    public void removeListener(MarketListener listener) { listeners.remove(listener); }
}
