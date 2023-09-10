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

import com.achomutovskij.portfolioservice.api.BucketErrors;
import com.achomutovskij.portfolioservice.api.BucketPosition;
import com.achomutovskij.portfolioservice.api.BucketsUpdateRequest;
import com.achomutovskij.portfolioservice.api.HoldingErrors;
import com.achomutovskij.portfolioservice.api.OrderErrors;
import com.achomutovskij.portfolioservice.api.OrderRequest;
import com.achomutovskij.portfolioservice.api.ProfitLossAmountAndPercent;
import com.achomutovskij.portfolioservice.api.StockPosition;
import com.achomutovskij.portfolioservice.api.UndertowPositionService;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.achomutovskij.portfolioservice.position.SymbolPosition;
import com.achomutovskij.portfolioservice.util.OffsetDateTimeUtils;
import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.logsafe.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.decimal4j.immutable.Decimal2f;
import org.decimal4j.immutable.Decimal4f;
import org.decimal4j.mutable.MutableDecimal2f;

public final class PositionResource implements UndertowPositionService {

    private static final String NO_SUCH_HOLDING = "User does not hold specified symbol";
    private static final String BUCKET_SET_EMPTY = "The bucket set must be non-empty";
    private static final String QUANTITY_MUST_BE_POSITIVE = "Quantity must be positive";

    private final MarketDataProvider marketDataProvider;
    private final BucketManagementResource bucketManager;
    private final Map<String, SymbolPosition> symbolPositions;

    public PositionResource(MarketDataProvider marketDataProvider, BucketManagementResource bucketManager) {
        this.marketDataProvider =
                Preconditions.checkNotNull(marketDataProvider, "Market Data Provider must be non-null");
        this.bucketManager = Preconditions.checkNotNull(bucketManager, "Bucket manager must be non-null");
        this.symbolPositions = new ConcurrentHashMap<>();
    }

    @Override
    public void addOrder(OrderRequest orderRequest) {
        if (orderRequest.getQuantity() <= 0) {
            throw OrderErrors.invalidQuantityAmount(QUANTITY_MUST_BE_POSITIVE);
        }

        String symbol = orderRequest.getSymbol();

        double priceOnSpecifiedDate =
                marketDataProvider.getPrice(symbol, OffsetDateTimeUtils.utcStartOfDay(orderRequest.getDate()));

        if (!symbolPositions.containsKey(symbol)) {
            symbolPositions.put(
                    symbol,
                    SymbolPosition.of(
                            orderRequest.getType(), symbol, orderRequest.getQuantity(), priceOnSpecifiedDate));

            bucketManager.insertSymbolIntoBuckets(symbol, orderRequest.getBuckets());
        } else {
            Optional<SymbolPosition> mergedPositionOptional = symbolPositions
                    .get(symbol)
                    .mergeWithNewOrder(orderRequest.getType(), orderRequest.getQuantity(), priceOnSpecifiedDate);

            // we will delete the position from the buckets below in case merged position is an empty optional
            bucketManager.insertSymbolIntoBuckets(symbol, orderRequest.getBuckets());

            mergedPositionOptional.ifPresentOrElse(merged -> symbolPositions.put(symbol, merged), () -> {
                symbolPositions.remove(symbol);
                bucketManager.removeSymbolFromAllBuckets(symbol);
            });
        }
    }

    @Override
    public void addSymbolToBuckets(BucketsUpdateRequest bucketUpdateRequest) {
        String symbol = bucketUpdateRequest.getSymbol();
        if (!symbolPositions.containsKey(symbol)) {
            throw HoldingErrors.noSuchHolding(symbol, NO_SUCH_HOLDING);
        }

        if (bucketUpdateRequest.getBuckets().isEmpty()) {
            throw BucketErrors.bucketSetEmpty(BUCKET_SET_EMPTY);
        }

        bucketManager.insertSymbolIntoBuckets(symbol, bucketUpdateRequest.getBuckets());
    }

    @Override
    public void removeSymbolFromBuckets(BucketsUpdateRequest bucketUpdateRequest) {
        String symbol = bucketUpdateRequest.getSymbol();
        if (!symbolPositions.containsKey(symbol)) {
            throw HoldingErrors.noSuchHolding(symbol, NO_SUCH_HOLDING);
        }

        if (bucketUpdateRequest.getBuckets().isEmpty()) {
            throw BucketErrors.bucketSetEmpty(BUCKET_SET_EMPTY);
        }

        bucketUpdateRequest.getBuckets().forEach(bucket -> bucketManager.removeSymbolFromBucket(bucket, symbol));
    }

    @Override
    public StockPosition getStockPosition(String symbol) {
        if (!symbolPositions.containsKey(symbol)) {
            throw HoldingErrors.noSuchHolding(symbol, NO_SUCH_HOLDING);
        }

        SymbolPosition symbolPosition = symbolPositions.get(symbol);
        double latestPrice = marketDataProvider.getLatestPrice(symbol);

        Pair<Double, Double> profitLossAmountAndPercent =
                symbolPosition.computeProfitLossAmountAndPercentage(latestPrice);

        return StockPosition.builder()
                .tradeType(symbolPosition.tradeType())
                .quantity(symbolPosition.totalShares())
                .totalPurchaseCost(symbolPosition.totalPurchaseCostAsDouble())
                .totalMarketValue(symbolPosition.computeMarketValue(latestPrice))
                .avgCostPerShare(symbolPosition.averageCostPerShareAsDouble())
                .position(1)
                .profitLossAmount(profitLossAmountAndPercent.getLeft())
                .profitLossPercent(profitLossAmountAndPercent.getRight())
                .buckets(bucketManager.getBucketsForSymbol(symbol))
                .build();
    }

    @Override
    public BucketPosition getBucketPosition(String bucketName) {
        Set<String> symbols = bucketManager.getPositionsInBucket(bucketName);
        if (symbols.isEmpty()) {
            return BucketPosition.builder()
                    .name(bucketName)
                    .totalNumberOfSharesLong(SafeLong.of(0))
                    .totalNumberOfSharesShort(SafeLong.of(0))
                    .totalPurchaseCost(0)
                    .totalMarketValue(0)
                    .numberOfPositions(0)
                    .profitLossAmount(0)
                    .profitLossPercent(0)
                    .bucketBreakdown(Collections.emptyMap())
                    .build();
        }

        List<SymbolPosition> positions =
                symbols.stream().map(symbolPositions::get).collect(Collectors.toList());

        Map<String, Double> latestPriceForEachSymbol =
                symbols.stream().collect(Collectors.toMap(symbol -> symbol, marketDataProvider::getLatestPrice));

        Pair<Double, Double> profitLossAmountAndPercent =
                getBucketProfitLossAmountAndPercentage(positions, latestPriceForEachSymbol);

        return BucketPosition.builder()
                .name(bucketName)
                .totalNumberOfSharesLong(SafeLong.of(getTotalNumberOfSharesLong(positions)))
                .totalNumberOfSharesShort(SafeLong.of(getTotalNumberOfSharesShort(positions)))
                .totalPurchaseCost(getBucketTotalPurchaseCost(positions).doubleValue())
                .totalMarketValue(getBucketMarketValue(positions, latestPriceForEachSymbol))
                .numberOfPositions(positions.size())
                .profitLossAmount(profitLossAmountAndPercent.getLeft())
                .profitLossPercent(profitLossAmountAndPercent.getRight())
                .bucketBreakdown(computeBucketBreakDown(positions, latestPriceForEachSymbol))
                .build();
    }

    private static Pair<Double, Double> getBucketProfitLossAmountAndPercentage(
            List<SymbolPosition> positions, Map<String, Double> latestPriceForEachSymbol) {

        MutableDecimal2f profitLossSum = MutableDecimal2f.zero();
        positions.forEach(position -> {
            double latestPrice = latestPriceForEachSymbol.get(position.symbol());
            profitLossSum.add(position.getProfitLossAmount(latestPrice));
        });

        Decimal2f profitLossPercentage = Decimal2f.valueOf(
                // when computing, switch to higher precision, since we will be multiplying the result by 100
                Decimal4f.valueOf(profitLossSum)
                        .divide(Decimal4f.valueOf(getBucketTotalPurchaseCost(positions)))
                        .multiply(100));

        return Pair.of(profitLossSum.doubleValue(), profitLossPercentage.doubleValue());
    }

    private static long getTotalNumberOfSharesLong(List<SymbolPosition> positions) {
        return positions.stream()
                .map(SymbolPosition::totalShares)
                .filter(numberOfShares -> numberOfShares > 0)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private static long getTotalNumberOfSharesShort(List<SymbolPosition> positions) {
        return positions.stream()
                .map(SymbolPosition::totalShares)
                .filter(numberOfShares -> numberOfShares < 0)
                .mapToLong(Integer::longValue)
                .sum();
    }

    private static Decimal2f getBucketTotalPurchaseCost(List<SymbolPosition> positions) {
        MutableDecimal2f costSum = MutableDecimal2f.zero();
        positions.forEach(position -> costSum.add(position.totalPurchaseCost()));
        return Decimal2f.valueOf(costSum);
    }

    private static double getBucketMarketValue(
            List<SymbolPosition> positions, Map<String, Double> latestPriceForEachSymbol) {

        MutableDecimal2f valueSum = MutableDecimal2f.zero();
        positions.forEach(position -> {
            double latestPrice = latestPriceForEachSymbol.get(position.symbol());
            valueSum.add(position.computeMarketValue(latestPrice));
        });
        return valueSum.doubleValue();
    }

    private static NavigableMap<String, ProfitLossAmountAndPercent> computeBucketBreakDown(
            List<SymbolPosition> positions, Map<String, Double> latestPriceForEachSymbol) {

        return positions.stream()
                .collect(Collectors.toMap(
                        SymbolPosition::symbol,
                        position -> {
                            double latestPrice = latestPriceForEachSymbol.get(position.symbol());
                            Pair<Double, Double> profitLossAmountAndPercent =
                                    position.computeProfitLossAmountAndPercentage(latestPrice);
                            return ProfitLossAmountAndPercent.of(
                                    profitLossAmountAndPercent.getLeft(), profitLossAmountAndPercent.getRight());
                        },
                        (first, _second) -> first,
                        TreeMap::new));
    }
}
