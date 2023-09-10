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

package com.achomutovskij.portfolioservice.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.achomutovskij.portfolioservice.api.BucketPosition;
import com.achomutovskij.portfolioservice.api.BucketsUpdateRequest;
import com.achomutovskij.portfolioservice.api.OrderRequest;
import com.achomutovskij.portfolioservice.api.ProfitLossAmountAndPercent;
import com.achomutovskij.portfolioservice.api.StockPosition;
import com.achomutovskij.portfolioservice.api.TradeType;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.testing.Assertions;
import com.palantir.conjure.java.lib.SafeLong;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionResourceTest {

    private static final OffsetDateTime AUG_11 = OffsetDateTime.of(2023, 8, 11, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime SEPT_7 = OffsetDateTime.of(2023, 9, 7, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime SEPT_8 = OffsetDateTime.of(2023, 9, 8, 0, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private MarketDataProvider marketDataProviderMock;

    private BucketManagementResource bucketManagementResource;
    private PositionResource positionResource;

    @BeforeEach
    public void beforeEach() {
        bucketManagementResource = new BucketManagementResource();
        positionResource = new PositionResource(marketDataProviderMock, bucketManagementResource);
    }

    @Test
    public void addOrder() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(Collections.emptySet())
                .build());

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);

        StockPosition stockPosition = positionResource.getStockPosition("NVDA");

        assertThat(stockPosition)
                .isEqualTo(StockPosition.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(5)
                        .totalPurchaseCost(2312.05)
                        .totalMarketValue(2278.6)
                        .avgCostPerShare(462.41)
                        .position(1)
                        .profitLossAmount(-33.45)
                        .profitLossPercent(-1.45)
                        .buckets(Collections.emptyList())
                        .build());
    }

    @Test
    public void addOrderWithBucketsSpecified() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of("B", "A"))
                .build());

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);

        StockPosition stockPosition = positionResource.getStockPosition("NVDA");

        assertThat(stockPosition)
                .isEqualTo(StockPosition.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(5)
                        .totalPurchaseCost(2312.05)
                        .totalMarketValue(2278.6)
                        .avgCostPerShare(462.41)
                        .position(1)
                        .profitLossAmount(-33.45)
                        .profitLossPercent(-1.45)
                        .buckets(ImmutableList.of("A", "B"))
                        .build());
    }

    @Test
    public void addToBucketAfterOrder() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of())
                .build());

        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();

        positionResource.addSymbolToBuckets(BucketsUpdateRequest.of(nvidia, ImmutableSet.of("A", "B")));

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "A", ImmutableList.of("NVDA"),
                        "B", ImmutableList.of("NVDA")));

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);

        StockPosition stockPosition = positionResource.getStockPosition("NVDA");

        assertThat(stockPosition)
                .isEqualTo(StockPosition.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(5)
                        .totalPurchaseCost(2312.05)
                        .totalMarketValue(2278.6)
                        .avgCostPerShare(462.41)
                        .position(1)
                        .profitLossAmount(-33.45)
                        .profitLossPercent(-1.45)
                        .buckets(ImmutableList.of("A", "B"))
                        .build());
    }

    @Test
    public void removeFromBucket() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of("B", "A"))
                .build());

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "A", ImmutableList.of("NVDA"),
                        "B", ImmutableList.of("NVDA")));

        positionResource.removeSymbolFromBuckets(BucketsUpdateRequest.of(nvidia, ImmutableSet.of("B")));

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "A", ImmutableList.of("NVDA"),
                        "B", ImmutableList.of()));

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);

        StockPosition stockPosition = positionResource.getStockPosition("NVDA");

        assertThat(stockPosition)
                .isEqualTo(StockPosition.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(5)
                        .totalPurchaseCost(2312.05)
                        .totalMarketValue(2278.6)
                        .avgCostPerShare(462.41)
                        .position(1)
                        .profitLossAmount(-33.45)
                        .profitLossPercent(-1.45)
                        .buckets(ImmutableList.of("A"))
                        .build());
    }

    @Test
    public void multipleBuyOrders() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_8)).thenReturn(455.72);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_8)
                .quantity(5)
                .buckets(ImmutableSet.of("B"))
                .build());

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);

        StockPosition stockPosition = positionResource.getStockPosition("NVDA");

        assertThat(stockPosition)
                .isEqualTo(StockPosition.builder()
                        .tradeType(TradeType.BUY)
                        .quantity(10)
                        .totalPurchaseCost(4590.70)
                        .totalMarketValue(4557.20)
                        .avgCostPerShare(459.07)
                        .position(1)
                        .profitLossAmount(-33.5)
                        .profitLossPercent(-0.73)
                        .buckets(ImmutableList.of("A", "B"))
                        .build());

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "A", ImmutableList.of("NVDA"),
                        "B", ImmutableList.of("NVDA")));
    }

    @Test
    public void closePosition() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        Mockito.when(marketDataProviderMock.getPrice(nvidia, SEPT_7)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.SELL)
                .symbol(nvidia)
                .date(SEPT_7)
                .quantity(5)
                .buckets(ImmutableSet.of("A", "B"))
                .build());

        Assertions.assertThatServiceExceptionThrownBy(() -> positionResource.getStockPosition("NVDA"))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Holding:NoSuchHolding"));

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "A", ImmutableList.of(),
                        "B", ImmutableList.of()));
    }

    @Test
    public void bucketPositionPerformance() {
        String nvidia = "NVDA";
        String amazon = "AMZN";
        String tesla = "TSLA";

        Mockito.when(marketDataProviderMock.getPrice(nvidia, AUG_11)).thenReturn(408.55);
        Mockito.when(marketDataProviderMock.getPrice(amazon, AUG_11)).thenReturn(138.41);
        Mockito.when(marketDataProviderMock.getPrice(tesla, AUG_11)).thenReturn(242.65);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(AUG_11)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(amazon)
                .date(AUG_11)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(tesla)
                .date(AUG_11)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of("A", ImmutableList.of(amazon, nvidia, tesla)));

        Mockito.when(marketDataProviderMock.getLatestPrice(nvidia)).thenReturn(455.72);
        Mockito.when(marketDataProviderMock.getLatestPrice(amazon)).thenReturn(138.23);
        Mockito.when(marketDataProviderMock.getLatestPrice(tesla)).thenReturn(248.5);

        BucketPosition bucketPosition = positionResource.getBucketPosition("A");

        assertThat(bucketPosition)
                .isEqualTo(BucketPosition.builder()
                        .name("A")
                        .totalNumberOfShares(SafeLong.of(15))
                        .totalPurchaseCost(3948.05)
                        .totalMarketValue(4212.25)
                        .numberOfPositions(3)
                        .profitLossAmount(264.2)
                        .profitLossPercent(6.69)
                        .bucketBreakdown(ImmutableSortedMap.of(
                                "AMZN", ProfitLossAmountAndPercent.of(-0.9, -0.13),
                                "NVDA", ProfitLossAmountAndPercent.of(235.85, 11.55),
                                "TSLA", ProfitLossAmountAndPercent.of(29.25, 2.41)))
                        .build());
    }
}
