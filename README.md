# Financial Portfolio Rebalancing Advisor (Java)

A dependency-free Java 17+ implementation of a portfolio rebalancing advisor. It includes simulated markets, an ML trend model, automated drift detection, tax- and cost-aware rebalancing, and personalised advice.

**Read the design:** [`docs/LLD.md`](docs/LLD.md). It covers the class diagram, design patterns, algorithms, sequence flow and extension points.

## Build, test and run

```bash
mvn test                  # 26 unit + integration tests (JUnit 5)
mvn -q compile exec:java  # run the end-to-end demo
mvn -q compile exec:java -Dexec.args="42"   # a different market seed
```

Without Maven:

```bash
javac -d out $(find src/main/java -name '*.java')
java -cp out com.portfolio.advisor.App
```

## What the demo shows
1. An investor profile, the allocation the risk profiler recommends, and the user's chosen target.
2. About 1.5 years of simulated trading, with **automated drift alerts** firing live.
3. Holdings, tax lots and the current allocation.
4. A drift report using the 5/25 tolerance bands.
5. The **AI market outlook**: a ridge regression and momentum ensemble with out-of-sample confidence.
6. Two rebalancing plans side by side, **naive FIFO vs tax-aware + AI tilt**, with fees, tax and total friction.
7. The portfolio after executing the better plan.
8. **Personalised advice**: goal funding, risk fit, tax-loss harvesting, concentration and outlook.

## Code map

| Package | Responsibility |
|---|---|
| `model` | `Asset`/`Stock`/`Bond`, `Portfolio`, `Holding`, `TaxLot`, `TargetAllocation`, `Trade`, `InvestorProfile` |
| `market` | `MarketDataService` (clock, prices, observers), `RegimeSwitchingPriceSimulator` |
| `prediction` | `FeatureExtractor`, `RidgeRegressionPredictor`, `MomentumPredictor`, `EnsemblePredictor`, `TrendAnalysisService` |
| `allocation` | `AllocationCalculator`, `AllocationSnapshot` |
| `drift` | `ToleranceBandDriftDetector`, `DriftMonitor`, `DriftReport` |
| `cost` / `tax` | `TieredTransactionCostModel`, `SimulatedTaxCalculator`, `FifoLotSelection`, `TaxEfficientLotSelection` |
| `rebalancing` | `AbstractRebalancer` (template), `ProportionalRebalancer`, `TaxAwareTrendRebalancer`, `RebalancePlan` |
| `advice` | `RiskProfiler`, `GoalPlanner`, `AdviceEngine`, `rules/*` |
| `service` | `PortfolioAdvisorService` (facade), `AdvisorFactory` (wiring) |
