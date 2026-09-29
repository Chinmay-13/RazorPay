package com.portfolio.advisor.advice.rules;

import com.portfolio.advisor.advice.AdviceCategory;
import com.portfolio.advisor.advice.AdviceContext;
import com.portfolio.advisor.advice.AdviceItem;
import com.portfolio.advisor.advice.AdvicePriority;
import com.portfolio.advisor.advice.AdviceRule;
import com.portfolio.advisor.model.Holding;
import com.portfolio.advisor.model.LotSale;
import com.portfolio.advisor.model.TaxLot;
import com.portfolio.advisor.tax.TaxCalculator;

import java.util.ArrayList;
import java.util.List;

/** Finds lots sitting on a meaningful loss that could be sold to offset gains. */
public final class TaxLossHarvestingRule implements AdviceRule {

    private static final double MIN_LOSS = 500;
    private static final double MIN_LOSS_PCT = 0.05;

    private final TaxCalculator taxCalculator;

    public TaxLossHarvestingRule(TaxCalculator taxCalculator) {
        this.taxCalculator = taxCalculator;
    }

    @Override
    public List<AdviceItem> evaluate(AdviceContext ctx) {
        List<AdviceItem> items = new ArrayList<>();
        for (Holding h : ctx.portfolio().holdings()) {
            double price = ctx.prices().get(h.asset().symbol());
            List<LotSale> losers = new ArrayList<>();
            for (TaxLot lot : h.lots()) {
                if (price < lot.costPerUnit() * (1 - MIN_LOSS_PCT)) {
                    losers.add(new LotSale(lot, lot.quantity(), price, lot.isLongTerm(ctx.asOf())));
                }
            }
            double loss = -losers.stream().mapToDouble(LotSale::gain).sum();
            if (loss < MIN_LOSS) continue;
            double saving = -taxCalculator.estimate(losers, ctx.profile()).estimatedTax();
            items.add(new AdviceItem(AdviceCategory.TAX, AdvicePriority.MEDIUM,
                    "Tax-loss harvesting opportunity in " + h.asset().symbol(),
                    String.format("%d lot(s) are down $%,.0f; realising the loss saves ~$%,.0f in tax. Reinvest in a "
                            + "similar (not identical) asset and avoid buying %s back for 30 days (wash-sale rule).",
                            losers.size(), loss, saving, h.asset().symbol())));
        }
        return items;
    }
}
