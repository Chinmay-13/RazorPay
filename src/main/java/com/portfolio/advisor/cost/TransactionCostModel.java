package com.portfolio.advisor.cost;

import com.portfolio.advisor.model.Asset;
import com.portfolio.advisor.model.TradeSide;

public interface TransactionCostModel {

    double estimate(Asset asset, TradeSide side, double quantity, double price);
}
