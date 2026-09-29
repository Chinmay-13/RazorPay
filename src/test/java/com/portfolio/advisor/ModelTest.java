package com.portfolio.advisor;

import com.portfolio.advisor.model.AssetClass;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.Portfolio;
import com.portfolio.advisor.model.TargetAllocation;
import com.portfolio.advisor.model.TaxLot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    @Test
    void targetAllocationMustSumToOne() {
        assertThrows(IllegalArgumentException.class, () -> TargetAllocation.of(0.6, 0.3, 0.05));
        assertThrows(IllegalArgumentException.class, () -> TargetAllocation.of(1.2, -0.2, 0.0));
        TargetAllocation ok = TargetAllocation.of(Map.of(AssetClass.STOCK, 0.7, AssetClass.BOND, 0.3));
        assertEquals(0.0, ok.weight(AssetClass.CASH));
    }

    @Test
    void buyingMoreThanCashIsRejected() {
        Portfolio p = new Portfolio("P", "I", 1_000);
        assertThrows(IllegalStateException.class,
                () -> p.buy(TestFixtures.STOCK_A, 10, 100, 1, LocalDate.of(2024, 1, 1)));
    }

    @Test
    void sellingReducesLotsAndCreditsCash() {
        Portfolio p = new Portfolio("P", "I", 10_000);
        p.buy(TestFixtures.STOCK_A, 50, 100, 0, LocalDate.of(2024, 1, 1));
        Holding h = p.holding("AAA").orElseThrow();
        TaxLot lot = h.lots().get(0);
        p.sell(TestFixtures.STOCK_A, List.of(new LotSale(lot, 20, 120, false)), 120, 5);
        assertEquals(30, h.quantity(), 1e-9);
        assertEquals(5_000 + 20 * 120 - 5, p.cash(), 1e-9);
    }

    @Test
    void lotIsLongTermOnlyAfterOneYear() {
        TaxLot lot = new TaxLot(1, 10, LocalDate.of(2024, 1, 1));
        assertFalse(lot.isLongTerm(LocalDate.of(2024, 12, 31)));
        assertTrue(lot.isLongTerm(LocalDate.of(2025, 1, 2)));
    }
}
