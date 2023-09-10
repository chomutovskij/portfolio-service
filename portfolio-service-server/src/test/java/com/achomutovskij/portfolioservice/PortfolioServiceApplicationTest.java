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

import static org.assertj.core.api.Assertions.assertThat;

import com.achomutovskij.portfolioservice.api.BucketManagementService;
import com.achomutovskij.portfolioservice.api.BucketPosition;
import com.achomutovskij.portfolioservice.api.BucketsUpdateRequest;
import com.achomutovskij.portfolioservice.api.OrderRequest;
import com.achomutovskij.portfolioservice.api.PositionService;
import com.achomutovskij.portfolioservice.api.StockPosition;
import com.achomutovskij.portfolioservice.api.TradeType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.client.config.ClientConfiguration;
import com.palantir.conjure.java.client.config.ClientConfigurations;
import com.palantir.conjure.java.client.jaxrs.JaxRsClient;
import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.conjure.java.okhttp.NoOpHostEventsSink;
import io.undertow.Undertow;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PortfolioServiceApplicationTest {

    private static final OffsetDateTime SEPT_5 = OffsetDateTime.of(2023, 9, 5, 0, 0, 0, 0, ZoneOffset.UTC);

    private static BucketManagementService bucketManagementService;
    private static PositionService positionService;

    private static Undertow server;

    @BeforeAll
    public static void before()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
                    KeyManagementException {

        server = PortfolioServiceApplication.startServer(Configuration.builder()
                .port(8345)
                .host("0.0.0.0")
                .externalApiResponseCacheDurationMinutes(15)
                .build());

        File crtFile = new File("var/certs/ca-cert");
        Certificate certificate =
                CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(crtFile));
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("server", certificate);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        TrustManager[] trustManager = trustManagerFactory.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManager, null);

        ClientConfiguration clientConfig = ClientConfigurations.of(
                ImmutableList.of("https://localhost:8345/api/"), sslContext.getSocketFactory(), (X509TrustManager)
                        trustManager[0]);

        bucketManagementService = JaxRsClient.create(
                BucketManagementService.class,
                UserAgent.of(UserAgent.Agent.of("test", "0.0.0")),
                NoOpHostEventsSink.INSTANCE,
                clientConfig);

        positionService = JaxRsClient.create(
                PositionService.class,
                UserAgent.of(UserAgent.Agent.of("test", "0.0.0")),
                NoOpHostEventsSink.INSTANCE,
                clientConfig);
    }

    @AfterAll
    public static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void compositeTest() {
        bucketManagementService.createBucket("BucketA");
        assertThat(bucketManagementService.getAllBuckets()).isEqualTo(ImmutableMap.of("BucketA", ImmutableList.of()));

        positionService.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol("TSLA")
                .date(SEPT_5)
                .quantity(11)
                .buckets(ImmutableSet.of("BucketB"))
                .build());

        positionService.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol("TSLA")
                .date(SEPT_5)
                .quantity(2)
                .buckets(ImmutableSet.of("BucketZ"))
                .build());

        positionService.addOrder(OrderRequest.builder()
                .type(TradeType.BUY)
                .symbol("NVDA")
                .date(SEPT_5)
                .quantity(5)
                .buckets(ImmutableSet.of("BucketB"))
                .build());

        positionService.addOrder(OrderRequest.builder()
                .type(TradeType.SELL)
                .symbol("AMZN")
                .date(SEPT_5)
                .quantity(2)
                .buckets(ImmutableSet.of("BucketA"))
                .build());

        positionService.addOrder(OrderRequest.builder()
                .type(TradeType.SELL)
                .symbol("GS")
                .date(SEPT_5)
                .quantity(1)
                .buckets(ImmutableSet.of())
                .build());

        assertThat(bucketManagementService.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("AMZN"),
                        "BucketB", ImmutableList.of("NVDA", "TSLA"),
                        "BucketZ", ImmutableList.of("TSLA")));

        StockPosition tslaStockPosition = positionService.getStockPosition("TSLA");
        assertThat(tslaStockPosition.getQuantity()).isEqualTo(13);
        assertThat(tslaStockPosition.getBuckets()).isEqualTo(ImmutableList.of("BucketB", "BucketZ"));

        StockPosition nvdaStockPosition = positionService.getStockPosition("NVDA");
        assertThat(nvdaStockPosition.getQuantity()).isEqualTo(5);
        assertThat(nvdaStockPosition.getBuckets()).isEqualTo(ImmutableList.of("BucketB"));

        StockPosition amznStockPosition = positionService.getStockPosition("AMZN");
        assertThat(amznStockPosition.getQuantity()).isEqualTo(-2);
        assertThat(amznStockPosition.getBuckets()).isEqualTo(ImmutableList.of("BucketA"));

        StockPosition gsStockPosition = positionService.getStockPosition("GS");
        assertThat(gsStockPosition.getQuantity()).isEqualTo(-1);
        assertThat(gsStockPosition.getBuckets()).isEqualTo(ImmutableList.of());

        BucketPosition bucketAPosition = positionService.getBucketPosition("BucketA");
        assertThat(bucketAPosition.getTotalNumberOfSharesLong()).isEqualTo(SafeLong.of(0));
        assertThat(bucketAPosition.getTotalNumberOfSharesShort()).isEqualTo(SafeLong.of(-2));

        BucketPosition bucketBPosition = positionService.getBucketPosition("BucketB");
        assertThat(bucketBPosition.getTotalNumberOfSharesLong()).isEqualTo(SafeLong.of(18));
        assertThat(bucketBPosition.getTotalNumberOfSharesShort()).isEqualTo(SafeLong.of(0));

        positionService.addSymbolToBuckets(BucketsUpdateRequest.of("GS", ImmutableSet.of("BucketZ")));
        assertThat(bucketManagementService.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("AMZN"),
                        "BucketB", ImmutableList.of("NVDA", "TSLA"),
                        "BucketZ", ImmutableList.of("GS", "TSLA")));

        positionService.removeSymbolFromBuckets(BucketsUpdateRequest.of("TSLA", ImmutableSet.of("BucketB")));
        assertThat(bucketManagementService.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("AMZN"),
                        "BucketB", ImmutableList.of("NVDA"),
                        "BucketZ", ImmutableList.of("GS", "TSLA")));

        bucketManagementService.deleteBucket("BucketZ");
        assertThat(bucketManagementService.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("AMZN"),
                        "BucketB", ImmutableList.of("NVDA")));
    }
}
