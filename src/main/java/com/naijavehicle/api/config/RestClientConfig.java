package com.naijavehicle.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a RestClient.Builder pre-wired with a merged SSL truststore that
 * includes both the default JVM CA roots AND every *.crt bundled under
 * classpath:/certs/. This prevents PKIX failures on Railway (or any environment
 * whose JVM ships a minimal truststore) without disabling certificate validation.
 */
@Slf4j
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        try {
            SSLContext sslContext = buildMergedSslContext();
            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
            return RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            log.warn("Could not build custom SSL context, falling back to default. Reason: {}", e.getMessage());
            return RestClient.builder();
        }
    }

    /**
     * Builds an SSLContext whose truststore contains:
     *  1. Every X.509 certificate from the default JVM truststore (PKIX roots)
     *  2. Every certificate found in *.crt files under classpath:/certs/
     *
     * This means ALL normal HTTPS sites still work, plus our custom CAs.
     */
    private static SSLContext buildMergedSslContext() throws Exception {
        KeyStore mergedStore = KeyStore.getInstance(KeyStore.getDefaultType());
        mergedStore.load(null, null);

        // --- Step 1: copy default JVM trust anchors into our store ---
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null); // load default store

        AtomicInteger idx = new AtomicInteger(0);
        for (javax.net.ssl.TrustManager tm : defaultTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509) {
                for (X509Certificate ca : x509.getAcceptedIssuers()) {
                    mergedStore.setCertificateEntry("default-ca-" + idx.getAndIncrement(), ca);
                }
            }
        }
        log.info("SSL: loaded {} default JVM trust anchors", idx.get());

        // --- Step 2: load every *.crt from classpath:/certs/ ---
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] certResources = resolver.getResources("classpath:/certs/*.crt");

        int customCount = 0;
        for (Resource resource : certResources) {
            try (InputStream is = resource.getInputStream()) {
                Collection<? extends Certificate> certs = cf.generateCertificates(is);
                for (Certificate cert : certs) {
                    mergedStore.setCertificateEntry("custom-cert-" + idx.getAndIncrement(), cert);
                    customCount++;
                }
                log.info("SSL: loaded {} cert(s) from {}", certs.size(), resource.getFilename());
            } catch (Exception e) {
                log.warn("SSL: could not load cert from {}: {}", resource.getFilename(), e.getMessage());
            }
        }
        log.info("SSL: loaded {} custom certificate(s) from classpath:/certs/", customCount);

        // --- Step 3: init the final SSLContext ---
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(mergedStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
}
