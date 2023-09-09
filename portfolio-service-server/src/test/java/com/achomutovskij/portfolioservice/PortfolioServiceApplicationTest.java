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

import com.achomutovskij.portfolioservice.api.BucketManagementService;
import com.achomutovskij.portfolioservice.api.PositionService;
import com.google.common.collect.ImmutableList;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.client.config.ClientConfiguration;
import com.palantir.conjure.java.client.config.ClientConfigurations;
import com.palantir.conjure.java.client.jaxrs.JaxRsClient;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

@SuppressWarnings("StrictUnusedVariable")
public class PortfolioServiceApplicationTest {

    private static BucketManagementService bucketManagementService;
    private static PositionService positionService;

    private static Undertow server;

    @BeforeAll
    public static void before()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
                    KeyManagementException {

        server = PortfolioServiceApplication.startServer(
                Configuration.builder().port(8345).host("0.0.0.0").build());

        File crtFile = new File("src/test/resources/certs/ca-cert");
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

    @AfterEach
    void afterEach() {}
}
