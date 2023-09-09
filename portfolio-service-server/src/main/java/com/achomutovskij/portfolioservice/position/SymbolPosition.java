/*
 * (c) Copyright 2023 Andrej Chomutovskij. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.achomutovskij.portfolioservice.position;

import com.achomutovskij.portfolioservice.api.TradeType;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.decimal4j.immutable.Decimal2f;
import org.decimal4j.immutable.Decimal4f;
import org.immutables.value.Value;

@Value.Immutable
public interface SymbolPosition {

    TradeType tradeType();

    String symbol();

    int totalSharesAbsolute();

    // Fixed-point arithmetic
    Decimal2f averageCostPerShare();

    @Value.Derived
    default Decimal2f totalPurchaseCost() {
        return this.averageCostPerShare().multiply(this.totalSharesAbsolute());
    }

    @Value.Derived
    default double totalPurchaseCostAsDouble() {
        return this.totalPurchaseCost().doubleValue();
    }

    @Value.Derived
    default double averageCostPerShareAsDouble() {
        return this.averageCostPerShare().doubleValue();
    }

    @Value.Derived
    default int totalShares() {
        return this.totalSharesAbsolute() * (this.tradeType().equals(TradeType.BUY) ? 1 : -1);
    }

    static SymbolPosition of(TradeType tradeType, String symbol, int totalShares, double marketPricePerShare) {
        return ImmutableSymbolPosition.builder()
                .tradeType(tradeType)
                .symbol(symbol)
                .totalSharesAbsolute(totalShares)
                .averageCostPerShare(Decimal2f.valueOf(marketPricePerShare))
                .build();
    }

    default Optional<SymbolPosition> mergeWithNewOrder(
            TradeType orderType, int shareAmount, double pricePerShareOnOrderDate) {

        Decimal2f marketPricePerShare = Decimal2f.valueOf(pricePerShareOnOrderDate);

        // if types match, we *always* need to re-compute the average cost per share
        if (this.tradeType().equals(orderType)) {

            int newTotalShares = this.totalSharesAbsolute() + shareAmount;

            // the below is doing:
            // this.averageCostPerShare() * this.totalShares() + pricePerShareOnOrderDate * shareAmount
            Decimal2f newTotalPaidToAcquirePosition = this.averageCostPerShare()
                    .multiply(this.totalSharesAbsolute())
                    .add(marketPricePerShare.multiply(shareAmount));

            return Optional.of(ImmutableSymbolPosition.builder()
                    .tradeType(this.tradeType())
                    .symbol(this.symbol())
                    .totalSharesAbsolute(newTotalShares)
                    .averageCostPerShare(newTotalPaidToAcquirePosition.divide(newTotalShares))
                    .build());
        } else {
            int newTotalShares = this.totalSharesAbsolute() - shareAmount;

            // if newTotalShares is 0, then we no longer have a position
            if (newTotalShares == 0) {
                return Optional.empty();
            }

            Decimal2f averageCostPerShare = this.averageCostPerShare();
            TradeType tradeType = this.tradeType();
            if (newTotalShares < 0) {
                // flip the trade type
                tradeType = tradeType.equals(TradeType.BUY) ? TradeType.SELL : TradeType.BUY;

                // If the newTotalShares became negative - that means we are flipping from LONG to SHORT or vice
                // versa. This trade is equivalent to 2 trades: first close the existing position, then start the
                // opposing one (with the remaining shares left after first bringing the balance to 0).
                // In which case the new average is what the market price is.
                averageCostPerShare = marketPricePerShare;
            }

            return Optional.of(ImmutableSymbolPosition.builder()
                    .tradeType(tradeType)
                    .symbol(this.symbol())
                    .totalSharesAbsolute(Math.abs(newTotalShares))
                    .averageCostPerShare(averageCostPerShare)
                    .build());
        }
    }

    default Decimal2f getProfitLossAmount(double marketPricePerShare) {
        return Decimal2f.valueOf(marketPricePerShare)
                .subtract(averageCostPerShare())
                .multiply(totalSharesAbsolute())
                .multiply(this.tradeType().equals(TradeType.BUY) ? 1 : -1);
    }

    default Pair<Double, Double> computeProfitLossAmountAndPercentage(double marketPricePerShare) {
        Decimal2f profitLossAmount = this.getProfitLossAmount(marketPricePerShare);

        Decimal2f profitLossPercentage = Decimal2f.valueOf(
                // when computing, switch to higher precision, since we will be multiplying the result by 100
                Decimal4f.valueOf(profitLossAmount)
                        .divide(Decimal4f.valueOf(this.totalPurchaseCost()))
                        .multiply(100.0));

        return Pair.of(profitLossAmount.doubleValue(), profitLossPercentage.doubleValue());
    }

    default double computeMarketValue(double marketPricePerShare) {
        return Decimal2f.valueOf(this.totalSharesAbsolute())
                .multiply(marketPricePerShare)
                .doubleValue();
    }
}
