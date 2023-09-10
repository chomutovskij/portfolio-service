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

package com.achomutovskij.portfolioservice;

import com.achomutovskij.portfolioservice.api.BucketManagementServiceEndpoints;
import com.achomutovskij.portfolioservice.api.DateServiceEndpoints;
import com.achomutovskij.portfolioservice.api.PositionServiceEndpoints;
import com.achomutovskij.portfolioservice.marketdata.MarketApiClient;
import com.achomutovskij.portfolioservice.marketdata.MarketDataProvider;
import com.achomutovskij.portfolioservice.resources.BucketManagementResource;
import com.achomutovskij.portfolioservice.resources.DateResource;
import com.achomutovskij.portfolioservice.resources.PositionResource;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.errors.ServiceException;
import com.palantir.conjure.java.config.ssl.SslSocketFactories;
import com.palantir.conjure.java.undertow.runtime.ConjureHandler;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import okhttp3.OkHttpClient;

@SuppressWarnings("StrictUnusedVariable")
public final class PortfolioServiceApplication {

    private static final SafeLogger log = SafeLoggerFactory.get(PortfolioServiceApplication.class);

    private static final String KEY_STORE_PATH = "var/certs/keystore.jks";
    private static final String TRUSTSTORE_PATH = "var/certs/truststore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private PortfolioServiceApplication() {}

    public static void main(String[] _args) {
        Configuration conf;
        try {
            conf = ConfigurationLoader.load();
        } catch (IOException e) {
            throw new ServiceException(ErrorType.INTERNAL, e, SafeArg.of("reason", "Failed to load the config"));
        }

        startServer(conf);
    }

    public static Undertow startServer(Configuration conf) {
        SslConfiguration sslConfig =
                SslConfiguration.of(Paths.get(TRUSTSTORE_PATH), Paths.get(KEY_STORE_PATH), KEYSTORE_PASSWORD);
        SSLContext sslContext = SslSocketFactories.createSslContext(sslConfig);

        MarketDataProvider marketDataProvider =
                new MarketDataProvider(new MarketApiClient(new OkHttpClient()), Duration.ofMinutes(15L));

        BucketManagementResource bucketManagementResource = new BucketManagementResource();

        Undertow server = Undertow.builder()
                .addHttpsListener(conf.getPort(), conf.getHost(), sslContext)
                .addHttpListener(conf.getPort() + 1, conf.getHost())
                .setHandler(Handlers.path()
                        .addPrefixPath(
                                "api/",
                                ConjureHandler.builder()
                                        .services(DateServiceEndpoints.of(new DateResource(marketDataProvider)))
                                        .services(BucketManagementServiceEndpoints.of(bucketManagementResource))
                                        .services(PositionServiceEndpoints.of(
                                                new PositionResource(marketDataProvider, bucketManagementResource)))
                                        .build()))
                .build();

        server.start();

        return server;
    }
}
