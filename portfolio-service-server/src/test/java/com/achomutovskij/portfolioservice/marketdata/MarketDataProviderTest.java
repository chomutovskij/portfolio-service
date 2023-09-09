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

import static org.assertj.core.api.Assertions.assertThat;

import com.achomutovskij.portfolioservice.util.OffsetDateTimeUtils;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.testing.Assertions;
import java.time.Duration;
import java.time.OffsetDateTime;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketDataProviderTest {

    private static final OffsetDateTime VALID_DATE = OffsetDateTimeUtils.getValidTradingDateStartOfDayInUtc();

    private MarketDataProvider marketDataProvider;

    @BeforeEach
    public void beforeEach() {
        marketDataProvider = new MarketDataProvider(new MarketApiClient(new OkHttpClient()), Duration.ofMinutes(15L));
    }

    @Test
    public void priceValidSymbolValidDate() {
        marketDataProvider.getPrice("NVDA", VALID_DATE);
    }

    @Test
    public void latestPriceValidSymbolValidDate() {
        marketDataProvider.getLatestPrice("NVDA");
    }

    @Test
    public void getAvailableDates() {
        assertThat(marketDataProvider.getAvailableDates("NVDA")).isNotEmpty();
    }

    @Test
    public void priceInvalidSymbol() {
        Assertions.assertThatServiceExceptionThrownBy(() -> marketDataProvider.getPrice("$BADSYMBOL", VALID_DATE))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Data:SymbolNotFound"));
    }

    @Test
    public void latestPriceInvalidSymbol() {
        Assertions.assertThatServiceExceptionThrownBy(() -> marketDataProvider.getLatestPrice("$BADSYMBOL"))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Data:SymbolNotFound"));
    }
}
