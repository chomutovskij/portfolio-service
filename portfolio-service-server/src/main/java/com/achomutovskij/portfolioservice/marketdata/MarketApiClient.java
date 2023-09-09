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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import java.io.IOException;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class MarketApiClient {

    private static final SafeLogger log = SafeLoggerFactory.get(MarketApiClient.class);

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public MarketApiClient(OkHttpClient okHttpClient) {
        this.okHttpClient = Preconditions.checkNotNull(okHttpClient, "OkHttpClient needs to be non-null");

        objectMapper = new ObjectMapper();
        objectMapper.registerModules(new GuavaModule()); // for ImmutableList
        objectMapper.registerModule(new JavaTimeModule()); // for OffsetDateTime
    }

    public Optional<MarketApiResponse> getApiResponse(String symbol) {

        String url = String.format(
                "https://api.dev.app.getbaraka.com/v1/finance_market/quotes/%s/historical?range=month&interval=day",
                symbol);

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.code() != 200 || response.body() == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(response.body().string(), MarketApiResponse.class));
        } catch (IOException | RuntimeException e) {
            log.error("Failed to get or parse the response from Market Data API", e);
            return Optional.empty();
        }
    }
}
