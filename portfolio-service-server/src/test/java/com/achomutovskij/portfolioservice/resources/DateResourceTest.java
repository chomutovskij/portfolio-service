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

import com.achomutovskij.portfolioservice.marketdata.MarketApiClient;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.testing.Assertions;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateResourceTest {

    private DateResource dateResource;

    @BeforeEach
    public void beforeEach() {
        dateResource = new DateResource(
                new MarketDataProvider(new MarketApiClient(new OkHttpClient()), Duration.ofMinutes(15L)));
    }

    @Test
    public void getDatesValidSymbol() {
        assertThat(dateResource.getAvailableDates("NVDA")).isNotEmpty();
    }

    @Test
    public void getDatesInvalidSymbol() {
        Assertions.assertThatServiceExceptionThrownBy(() -> dateResource.getAvailableDates("$BADSYMBOL"))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Data:SymbolNotFound"));
    }
}
