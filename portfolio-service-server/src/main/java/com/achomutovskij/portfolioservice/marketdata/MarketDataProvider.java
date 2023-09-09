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

package com.achomutovskij.portfolioservice.marketdata;

import com.achomutovskij.portfolioservice.api.DataErrors;
import com.achomutovskij.portfolioservice.api.DateErrors;
import com.palantir.logsafe.Preconditions;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class MarketDataProvider {

    private static final String NO_DATA = "No market data exists for the specified symbol";

    private final MarketApiClient apiClient;
    private final Duration refreshPeriod;

    private final Map<String, Map<OffsetDateTime, Double>> symbolToDatePrices; // symbol -> date -> price
    private final Map<String, Double> latestPrices; // symbol -> latest price
    private final Map<String, OffsetDateTime> lastTimeSymbolDataUpdated; // symbol -> timestamp when it was last updated

    public MarketDataProvider(MarketApiClient apiClient, Duration refreshPeriod) {
        this.apiClient = Preconditions.checkNotNull(apiClient, "API Client must be non-null");
        this.refreshPeriod = Preconditions.checkNotNull(refreshPeriod, "Refresh period must be non-null");
        this.symbolToDatePrices = new ConcurrentHashMap<>();
        this.latestPrices = new ConcurrentHashMap<>();
        this.lastTimeSymbolDataUpdated = new ConcurrentHashMap<>();
    }

    public double getPrice(@Nonnull String symbol, @Nonnull OffsetDateTime date) {
        if (!symbolToDatePrices.containsKey(symbol)
                || !symbolToDatePrices.get(symbol).containsKey(date)) {
            updateState(symbol);
        }

        if (!symbolToDatePrices.containsKey(symbol)) {
            throw DataErrors.symbolNotFound(symbol, NO_DATA);
        }

        if (!symbolToDatePrices.get(symbol).containsKey(date)) {
            throw DateErrors.dateNotFound(date);
        }

        return symbolToDatePrices.get(symbol).get(date);
    }

    public double getLatestPrice(@Nonnull String symbol) {
        if (!latestPrices.containsKey(symbol)) {
            updateState(symbol);
        }

        if (!latestPrices.containsKey(symbol)) {
            throw DataErrors.symbolNotFound(symbol, NO_DATA);
        }

        return latestPrices.get(symbol);
    }

    public List<OffsetDateTime> getAvailableDates(String symbol) {
        if (!symbolToDatePrices.containsKey(symbol)) {
            updateState(symbol);
        }

        if (!symbolToDatePrices.containsKey(symbol)) {
            throw DataErrors.symbolNotFound(symbol, NO_DATA);
        }

        return symbolToDatePrices.get(symbol).keySet().stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    private void updateState(@Nonnull String symbol) {
        if (lastTimeSymbolDataUpdated.containsKey(symbol) && !shouldRefresh(lastTimeSymbolDataUpdated.get(symbol))) {
            return;
        }

        apiClient.getApiResponse(symbol).ifPresent(marketApiResponse -> {
            List<MarketApiResponse.DataEntry> data = marketApiResponse.data();
            if (!data.isEmpty()) {
                latestPrices.put(symbol, data.get(data.size() - 1).close());
            }

            Map<OffsetDateTime, Double> pricesForSymbol = data.stream()
                    .collect(Collectors.toMap(MarketApiResponse.DataEntry::date, MarketApiResponse.DataEntry::close));

            symbolToDatePrices.computeIfAbsent(symbol, _key -> new HashMap<>()).putAll(pricesForSymbol);

            lastTimeSymbolDataUpdated.put(symbol, OffsetDateTime.now(ZoneOffset.UTC));
        });
    }

    private boolean shouldRefresh(OffsetDateTime lastTimeRefreshed) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Duration.between(lastTimeRefreshed, now).compareTo(refreshPeriod) > 0;
    }
}
