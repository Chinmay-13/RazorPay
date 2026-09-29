package com.portfolio.advisor.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Portfolio {

    private final String id;
    private final String ownerId;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();
    private final List<Trade> tradeHistory = new ArrayList<>();
    private double cash;
    private TargetAllocation targetAllocation;

    public Portfolio(String id, String ownerId, double initialCash) {
        if (initialCash < 0) throw new IllegalArgumentException("initial cash must be >= 0");
        this.id = id;
        this.ownerId = ownerId;
        this.cash = initialCash;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("deposit must be positive");
        cash += amount;
    }

    public void buy(Asset asset, double quantity, double price, double cost, LocalDate date) {
        double outflow = quantity * price + cost;
        if (outflow > cash + 1e-6) {
            throw new IllegalStateException(String.format("insufficient cash for %s: need %.2f, have %.2f",
                    asset.symbol(), outflow, cash));
        }
        holdings.computeIfAbsent(asset.symbol(), s -> new Holding(asset)).addLot(quantity, price, date);
        cash -= outflow;
    }

    public void sell(Asset asset, List<LotSale> lotSales, double price, double cost) {
        Holding holding = holdings.get(asset.symbol());
        if (holding == null) throw new IllegalStateException("no holding in " + asset.symbol());
        holding.applySale(lotSales);
        if (holding.isEmpty()) holdings.remove(asset.symbol());
        cash += lotSales.stream().mapToDouble(LotSale::quantity).sum() * price - cost;
    }

    /** Applies a planned trade and appends it to the history. */
    public void execute(Trade trade, LocalDate date) {
        if (trade.side() == TradeSide.BUY) {
            buy(trade.asset(), trade.quantity(), trade.price(), trade.transactionCost(), date);
        } else {
            sell(trade.asset(), trade.lotSales(), trade.price(), trade.transactionCost());
        }
        tradeHistory.add(trade);
    }

    public Optional<Holding> holding(String symbol) { return Optional.ofNullable(holdings.get(symbol)); }
    public Collection<Holding> holdings() { return Collections.unmodifiableCollection(holdings.values()); }
    public List<Trade> tradeHistory() { return Collections.unmodifiableList(tradeHistory); }
    public double cash() { return cash; }
    public String id() { return id; }
    public String ownerId() { return ownerId; }

    public Optional<TargetAllocation> targetAllocation() { return Optional.ofNullable(targetAllocation); }
    public void setTargetAllocation(TargetAllocation target) { this.targetAllocation = target; }
}
