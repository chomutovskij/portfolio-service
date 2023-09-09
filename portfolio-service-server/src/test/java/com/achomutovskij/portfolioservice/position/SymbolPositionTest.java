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

import static org.assertj.core.api.Assertions.assertThat;

import com.achomutovskij.portfolioservice.api.TradeType;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class SymbolPositionTest {
    @Test
    public void simpleLong() {
        SymbolPosition position = SymbolPosition.of(TradeType.BUY, "NVDA", 100, 470.61);
        assertThat(position.tradeType()).isEqualTo(TradeType.BUY);
        assertThat(position.symbol()).isEqualTo("NVDA");
        assertThat(position.totalShares()).isEqualTo(100);
        assertThat(position.averageCostPerShareAsDouble()).isEqualTo(470.61);
        assertThat(position.totalPurchaseCostAsDouble()).isEqualTo(47061);
    }

    @Test
    public void simpleShort() {
        SymbolPosition position = SymbolPosition.of(TradeType.SELL, "NVDA", 100, 470.61);
        assertThat(position.tradeType()).isEqualTo(TradeType.SELL);
        assertThat(position.symbol()).isEqualTo("NVDA");
        assertThat(position.totalShares()).isEqualTo(-100);
        assertThat(position.averageCostPerShareAsDouble()).isEqualTo(470.61);
        assertThat(position.totalPurchaseCostAsDouble()).isEqualTo(47061);
    }

    @Test
    public void mergeTwoBuyOrders() {
        SymbolPosition position = SymbolPosition.of(TradeType.BUY, "NVDA", 2, 328.98);
        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.BUY, 2, 333.18);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.BUY);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(4);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(331.08);
            assertThat(updatedOne.totalPurchaseCostAsDouble()).isEqualTo(1324.32);
        });
    }

    @Test
    public void mergeTwoSellOrders() {
        SymbolPosition position = SymbolPosition.of(TradeType.SELL, "NVDA", 2, 328.98);
        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.SELL, 2, 333.18);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.SELL);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(-4);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(331.08);
            assertThat(updatedOne.totalPurchaseCostAsDouble()).isEqualTo(1324.32);
        });
    }

    @Test
    public void mergeDifferentTypesOfOrdersStartWithBuy() {
        SymbolPosition position = SymbolPosition.of(TradeType.BUY, "NVDA", 2, 700.00);

        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.BUY, 2, 300.0);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.BUY);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(4);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(500.00);
        });

        Optional<SymbolPosition> updatedTwoOpt =
                updatedOneOpt.flatMap(updatedOne -> updatedOne.mergeWithNewOrder(TradeType.SELL, 2, 796.45));
        assertThat(updatedTwoOpt).isPresent().get().satisfies(updatedTwo -> {
            assertThat(updatedTwo.tradeType()).isEqualTo(TradeType.BUY);
            assertThat(updatedTwo.symbol()).isEqualTo("NVDA");
            assertThat(updatedTwo.totalShares()).isEqualTo(2);

            // average should not change since last order
            assertThat(updatedTwo.averageCostPerShareAsDouble()).isEqualTo(500.00);
        });

        Optional<SymbolPosition> updatedThreeOpt =
                updatedTwoOpt.flatMap(updatedTwo -> updatedTwo.mergeWithNewOrder(TradeType.BUY, 2, 700.00));
        assertThat(updatedThreeOpt).isPresent().get().satisfies(updatedThree -> {
            assertThat(updatedThree.tradeType()).isEqualTo(TradeType.BUY);
            assertThat(updatedThree.symbol()).isEqualTo("NVDA");
            assertThat(updatedThree.totalShares()).isEqualTo(4);
            assertThat(updatedThree.averageCostPerShareAsDouble()).isEqualTo(600.00);
        });
    }

    @Test
    public void mergeDifferentTypesOfOrdersStartWithSell() {
        SymbolPosition position = SymbolPosition.of(TradeType.SELL, "NVDA", 2, 700.00);

        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.SELL, 2, 300.0);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.SELL);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(-4);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(500.00);
        });

        Optional<SymbolPosition> updatedTwoOpt =
                updatedOneOpt.flatMap(updatedOne -> updatedOne.mergeWithNewOrder(TradeType.BUY, 2, 796.45));
        assertThat(updatedTwoOpt).isPresent().get().satisfies(updatedTwo -> {
            assertThat(updatedTwo.tradeType()).isEqualTo(TradeType.SELL);
            assertThat(updatedTwo.symbol()).isEqualTo("NVDA");
            assertThat(updatedTwo.totalShares()).isEqualTo(-2);

            // average should not change since last order
            assertThat(updatedTwo.averageCostPerShareAsDouble()).isEqualTo(500.00);
        });

        Optional<SymbolPosition> updatedThreeOpt =
                updatedTwoOpt.flatMap(updatedTwo -> updatedTwo.mergeWithNewOrder(TradeType.SELL, 2, 700.00));
        assertThat(updatedThreeOpt).isPresent().get().satisfies(updatedThree -> {
            assertThat(updatedThree.tradeType()).isEqualTo(TradeType.SELL);
            assertThat(updatedThree.symbol()).isEqualTo("NVDA");
            assertThat(updatedThree.totalShares()).isEqualTo(-4);
            assertThat(updatedThree.averageCostPerShareAsDouble()).isEqualTo(600.00);
        });
    }

    @Test
    public void switchFromLongToShort() {
        SymbolPosition position = SymbolPosition.of(TradeType.BUY, "NVDA", 2, 328.98);
        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.SELL, 4, 333.18);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.SELL);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(-2);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(333.18);
            assertThat(updatedOne.totalPurchaseCostAsDouble()).isEqualTo(666.36);
        });
    }

    @Test
    public void switchFromShortToLong() {
        SymbolPosition position = SymbolPosition.of(TradeType.SELL, "NVDA", 2, 328.98);
        Optional<SymbolPosition> updatedOneOpt = position.mergeWithNewOrder(TradeType.BUY, 4, 333.18);
        assertThat(updatedOneOpt).isPresent().get().satisfies(updatedOne -> {
            assertThat(updatedOne.tradeType()).isEqualTo(TradeType.BUY);
            assertThat(updatedOne.symbol()).isEqualTo("NVDA");
            assertThat(updatedOne.totalShares()).isEqualTo(2);
            assertThat(updatedOne.averageCostPerShareAsDouble()).isEqualTo(333.18);
            assertThat(updatedOne.totalPurchaseCostAsDouble()).isEqualTo(666.36);
        });
    }

    @Test
    public void longPositionProfit() {
        SymbolPosition longPosition = SymbolPosition.of(TradeType.BUY, "NVDA", 10, 328.98);
        Pair<Double, Double> profitPair = longPosition.computeProfitLossAmountAndPercentage(330.98);
        assertThat(profitPair.getLeft()).isEqualTo(20.00);
        assertThat(profitPair.getRight()).isEqualTo(0.61);
    }

    @Test
    public void shortPositionProfit() {
        SymbolPosition shortPosition = SymbolPosition.of(TradeType.SELL, "NVDA", 10, 330.98);
        Pair<Double, Double> profitPair = shortPosition.computeProfitLossAmountAndPercentage(328.98);
        assertThat(profitPair.getLeft()).isEqualTo(20.00);
        assertThat(profitPair.getRight()).isEqualTo(0.6);
    }

    @Test
    public void longPositionLoss() {
        SymbolPosition longPosition = SymbolPosition.of(TradeType.BUY, "NVDA", 10, 330.98);
        Pair<Double, Double> lossPair = longPosition.computeProfitLossAmountAndPercentage(328.98);
        assertThat(lossPair.getLeft()).isEqualTo(-20.00);
        assertThat(lossPair.getRight()).isEqualTo(-0.6);
    }

    @Test
    public void shortPositionLoss() {
        SymbolPosition shortPosition = SymbolPosition.of(TradeType.SELL, "NVDA", 10, 328.98);
        Pair<Double, Double> lossPair = shortPosition.computeProfitLossAmountAndPercentage(330.98);
        assertThat(lossPair.getLeft()).isEqualTo(-20.00);
        assertThat(lossPair.getRight()).isEqualTo(-0.61);
    }
}
