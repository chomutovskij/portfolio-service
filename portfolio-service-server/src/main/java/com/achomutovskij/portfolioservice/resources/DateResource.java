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

import com.achomutovskij.portfolioservice.api.UndertowDateService;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.palantir.logsafe.Preconditions;
import java.time.OffsetDateTime;
import java.util.List;

public final class DateResource implements UndertowDateService {

    private final MarketDataProvider marketDataProvider;

    public DateResource(MarketDataProvider marketDataProvider) {
        this.marketDataProvider =
                Preconditions.checkNotNull(marketDataProvider, "Market data provider must be non-null");
    }

    @Override
    public List<OffsetDateTime> getAvailableDates(String symbol) {
        return marketDataProvider.getAvailableDates(symbol);
    }
}
