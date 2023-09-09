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

import com.achomutovskij.portfolioservice.api.BucketsUpdateRequest;
import com.achomutovskij.portfolioservice.api.OrderRequest;
import com.achomutovskij.portfolioservice.api.StockPosition;
import com.achomutovskij.portfolioservice.api.TradeType;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.testing.Assertions;
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

    private static final OffsetDateTime THU_7_SEPT = OffsetDateTime.of(2023, 9, 7, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime FRI_8_SEPT = OffsetDateTime.of(2023, 9, 8, 0, 0, 0, 0, ZoneOffset.UTC);

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
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
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
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
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
    public void addToBucket() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
                .quantity(5)
                .buckets(ImmutableSet.of())
                .build());

        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();

        positionResource.addSymbolToBuckets(BucketsUpdateRequest.of(nvidia, ImmutableSet.of("A", "B")));

        assertThat(bucketManagementResource.getPositionsInBucket("A")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getPositionsInBucket("B")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getBucketsForSymbol(nvidia)).isEqualTo(ImmutableList.of("A", "B"));
        assertThat(bucketManagementResource.getAllBuckets()).isEqualTo(ImmutableList.of("A", "B"));

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
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
                .quantity(5)
                .buckets(ImmutableSet.of("B", "A"))
                .build());

        assertThat(bucketManagementResource.getPositionsInBucket("A")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getPositionsInBucket("B")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getBucketsForSymbol(nvidia)).isEqualTo(ImmutableList.of("A", "B"));
        assertThat(bucketManagementResource.getAllBuckets()).isEqualTo(ImmutableList.of("A", "B"));

        positionResource.removeSymbolFromBuckets(BucketsUpdateRequest.of(nvidia, ImmutableSet.of("B")));

        assertThat(bucketManagementResource.getPositionsInBucket("A")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getPositionsInBucket("B")).isEmpty();
        assertThat(bucketManagementResource.getBucketsForSymbol(nvidia)).isEqualTo(ImmutableList.of("A"));
        assertThat(bucketManagementResource.getAllBuckets()).isEqualTo(ImmutableList.of("A", "B"));

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
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        Mockito.when(marketDataProviderMock.getPrice(nvidia, FRI_8_SEPT)).thenReturn(455.72);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(FRI_8_SEPT)
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

        assertThat(bucketManagementResource.getPositionsInBucket("A")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getPositionsInBucket("B")).isEqualTo(ImmutableSet.of(nvidia));
        assertThat(bucketManagementResource.getBucketsForSymbol(nvidia)).isEqualTo(ImmutableList.of("A", "B"));
        assertThat(bucketManagementResource.getAllBuckets()).isEqualTo(ImmutableList.of("A", "B"));
    }

    @Test
    public void closePosition() {
        String nvidia = "NVDA";
        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol(nvidia)
                .date(THU_7_SEPT)
                .quantity(5)
                .buckets(ImmutableSet.of("A"))
                .build());

        Mockito.when(marketDataProviderMock.getPrice(nvidia, THU_7_SEPT)).thenReturn(462.41);

        positionResource.addOrder(OrderRequest.builder()
                .type(TradeType.SELL)
                .symbol(nvidia)
                .date(THU_7_SEPT)
                .quantity(5)
                .buckets(ImmutableSet.of("A", "B"))
                .build());

        Assertions.assertThatServiceExceptionThrownBy(() -> positionResource.getStockPosition("NVDA"))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Holding:NoSuchHolding"));

        assertThat(bucketManagementResource.getPositionsInBucket("A")).isEmpty();
        assertThat(bucketManagementResource.getPositionsInBucket("B")).isEmpty();
        assertThat(bucketManagementResource.getAllBuckets()).isEqualTo(ImmutableList.of("A", "B"));
    }
}
