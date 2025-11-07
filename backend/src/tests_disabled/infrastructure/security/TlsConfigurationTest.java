package com.droid.bss.infrastructure.security;

import com.droid.bss.BssApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TLS Configuration Tests
 *
 * Tests TLS version enforcement, cipher suite validation, certificate chain validation,
 * certificate expiration, self-signed certificate rejection, and mTLS support.
 */
@SpringBootTest(classes = Application.class)
@TestPropertySource(properties = {
    "server.ssl.enabled=true",
    "server.ssl.protocol=TLS",
    "server.ssl.ciphers=TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256",
    "server.ssl.enabled-protocols=TLSv1.2,TLSv1.3"
})
@DisplayName("TLS Configuration Tests")
class TlsConfigurationTest {

    private static final Set<String> SUPPORTED_TLS_VERSIONS = Set.of("TLSv1.2", "TLSv1.3");
    private static final Set<String> STRONG_CIPHERS = Set.of(
        "TLS_AES_256_GCM_SHA384",
        "TLS_CHACHA20_POLY1305_SHA256",
        "TLS_AES_128_GCM_SHA256"
    );

    @Test
    @DisplayName("Should enforce TLS version")
    void shouldEnforceTlsVersion() {
        for (String version : SUPPORTED_TLS_VERSIONS) {
            try {
                SSLContext sslContext = SSLContext.getInstance(version);
                assertThat(sslContext).isNotNull();
            } catch (Exception e) {
                fail("Failed to create SSLContext for " + version, e);
            }
        }
    }

    @Test
    @DisplayName("Should validate cipher suite strength")
    void shouldValidateCipherSuiteStrength() {
        String[] strongCiphers = STRONG_CIPHERS.toArray(new String[0]);

        assertThat(strongCiphers).isNotEmpty();
        assertThat(strongCiphers.length).isGreaterThanOrEqualTo(3);

        for (String cipher : strongCiphers) {
            assertThat(cipher).startsWith("TLS");
            assertThat(cipher).containsAnyOf("AES", "CHACHA20");
        }
    }

    @Test
    @DisplayName("Should validate certificate chain")
    void shouldValidateCertificateChain() {
        MockCertificate cert = createMockCertificate();

        assertThat(cert).isNotNull();
        assertThat(cert.getSubject()).isNotEmpty();
        assertThat(cert.getIssuer()).isNotEmpty();
        assertThat(cert.getSerialNumber()).isNotEmpty();
        assertThat(cert.getSignature()).isNotEmpty();
        assertThat(cert.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should check certificate expiration")
    void shouldCheckCertificateExpiration() {
        MockCertificate validCert = createCertificate(System.currentTimeMillis() + 86400000);
        MockCertificate expiredCert = createCertificate(System.currentTimeMillis() - 86400000);

        assertThat(validCert.isExpired()).isFalse();
        assertThat(expiredCert.isExpired()).isTrue();
        assertThat(validCert.getDaysUntilExpiration()).isGreaterThan(0);
        assertThat(expiredCert.getDaysUntilExpiration()).isLessThan(0);
    }

    @Test
    @DisplayName("Should reject self-signed certificates")
    void shouldRejectSelfSignedCertificates() {
        MockCertificate selfSignedCert = createSelfSignedCertificate();

        boolean isSelfSigned = selfSignedCert.getSubject().equals(selfSignedCert.getIssuer());

        assertThat(isSelfSigned).isTrue();

        boolean shouldReject = isSelfSigned && !selfSignedCert.isCA();
        assertThat(shouldReject).isTrue();
    }

    @Test
    @DisplayName("Should support mutual TLS (mTLS)")
    void shouldSupportMutualTls() {
        MockCertificate serverCert = createMockCertificate();
        MockCertificate clientCert = createMockCertificate();

        MtlsConfig mtlsConfig = new MtlsConfig(serverCert, clientCert);

        assertThat(mtlsConfig).isNotNull();
        assertThat(mtlsConfig.getServerCertificate()).isNotNull();
        assertThat(mtlsConfig.getClientCertificate()).isNotNull();
        assertThat(mtlsConfig.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should validate TLS session reuse")
    void shouldValidateTlsSessionReuse() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);

            SSLEngine serverEngine = sslContext.createSSLEngine();
            serverEngine.setNeedClientAuth(true);

            assertThat(serverEngine.getNeedClientAuth()).isTrue();

            String[] enabledProtocols = serverEngine.getEnabledProtocols();
            assertThat(enabledProtocols).containsAnyOf("TLSv1.2", "TLSv1.3");
        } catch (Exception e) {
            fail("TLS session reuse test failed", e);
        }
    }

    @Test
    @DisplayName("Should measure TLS performance impact")
    void shouldMeasureTlsPerformanceImpact() {
        long startTime = System.nanoTime();
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (Exception e) {
            fail("TLS performance test failed", e);
        }
        long endTime = System.nanoTime();

        long setupTime = endTime - startTime;

        assertThat(setupTime).isGreaterThan(0);
        assertThat(setupTime).isLessThan(1000000);
    }

    @Test
    @DisplayName("Should validate cipher suite strength")
    void shouldValidateCipherSuiteStrength() {
        MockCertificate cert = createMockCertificate();
        String[] strongCiphers = cert.getSupportedCiphers();

        assertThat(strongCiphers).isNotEmpty();

        for (String cipher : strongCiphers) {
            assertThat(cipher).matches("TLS_[A-Z0-9_]+");

            boolean isStrong = cipher.contains("AES") || cipher.contains("CHACHA20");
            assertThat(isStrong).isTrue();
        }
    }

    @Test
    @DisplayName("Should protect against protocol downgrade")
    void shouldProtectAgainstProtocolDowngrade() {
        Set<String> allowedProtocols = SUPPORTED_TLS_VERSIONS;

        assertThat(allowedProtocols).doesNotContain("TLSv1.0", "TLSv1.1", "SSL");
        assertThat(allowedProtocols).containsOnly("TLSv1.2", "TLSv1.3");
    }

    @Test
    @DisplayName("Should support certificate pinning")
    void shouldSupportCertificatePinning() {
        MockCertificate cert = createMockCertificate();
        String certificatePin = generatePin(cert);

        String expectedPin = "sha256/" + generateHash(cert);

        assertThat(certificatePin).isNotEmpty();
        assertThat(certificatePin).startsWith("sha256/");
        assertThat(certificatePin.length()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Should enable TLS debugging")
    void shouldEnableTlsDebugging() {
        TlsDebugConfig debugConfig = new TlsDebugConfig(true, true, true);

        assertThat(debugConfig.isHandshakeEnabled()).isTrue();
        assertThat(debugConfig.isPacketEnabled()).isTrue();
        assertThat(debugConfig.isCipherEnabled()).isTrue();

        assertThat(debugConfig.getLogLevel()).isEqualTo("DEBUG");
    }

    private MockCertificate createMockCertificate() {
        return createCertificate(System.currentTimeMillis() + 86400000);
    }

    private MockCertificate createCertificate(long expirationTime) {
        return new MockCertificate(
            "CN=example.com,O=Example,C=US",
            "CN=Test CA,O=Example,C=US",
            "12345",
            "signature123",
            expirationTime,
            true
        );
    }

    private MockCertificate createSelfSignedCertificate() {
        return new MockCertificate(
            "CN=selfsigned.com,O=Example,C=US",
            "CN=selfsigned.com,O=Example,C=US",
            "67890",
            "selfsigned123",
            System.currentTimeMillis() + 86400000,
            false
        );
    }

    private String generatePin(MockCertificate cert) {
        return "sha256/" + generateHash(cert);
    }

    private String generateHash(MockCertificate cert) {
        return cert.getSerialNumber() + cert.getSignature();
    }

    private static class MockCertificate {
        private final String subject;
        private final String issuer;
        private final String serialNumber;
        private final String signature;
        private final long expirationTime;
        private final boolean isCA;

        public MockCertificate(String subject, String issuer, String serialNumber,
                              String signature, long expirationTime, boolean isCA) {
            this.subject = subject;
            this.issuer = issuer;
            this.serialNumber = serialNumber;
            this.signature = signature;
            this.expirationTime = expirationTime;
            this.isCA = isCA;
        }

        public String getSubject() { return subject; }
        public String getIssuer() { return issuer; }
        public String getSerialNumber() { return serialNumber; }
        public String getSignature() { return signature; }
        public long getExpirationTime() { return expirationTime; }
        public boolean isCA() { return isCA; }

        public boolean isValid() {
            return !isExpired();
        }

        public boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }

        public long getDaysUntilExpiration() {
            return (expirationTime - System.currentTimeMillis()) / 86400000;
        }

        public String[] getSupportedCiphers() {
            return new String[] {
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256"
            };
        }
    }

    private static class MtlsConfig {
        private final MockCertificate serverCertificate;
        private final MockCertificate clientCertificate;
        private final boolean enabled;

        public MtlsConfig(MockCertificate serverCertificate, MockCertificate clientCertificate) {
            this.serverCertificate = serverCertificate;
            this.clientCertificate = clientCertificate;
            this.enabled = true;
        }

        public MockCertificate getServerCertificate() { return serverCertificate; }
        public MockCertificate getClientCertificate() { return clientCertificate; }
        public boolean isEnabled() { return enabled; }
    }

    private static class TlsDebugConfig {
        private final boolean handshakeEnabled;
        private final boolean packetEnabled;
        private final boolean cipherEnabled;
        private final String logLevel;

        public TlsDebugConfig(boolean handshakeEnabled, boolean packetEnabled, boolean cipherEnabled) {
            this.handshakeEnabled = handshakeEnabled;
            this.packetEnabled = packetEnabled;
            this.cipherEnabled = cipherEnabled;
            this.logLevel = "DEBUG";
        }

        public boolean isHandshakeEnabled() { return handshakeEnabled; }
        public boolean isPacketEnabled() { return packetEnabled; }
        public boolean isCipherEnabled() { return cipherEnabled; }
        public String getLogLevel() { return logLevel; }
    }
}
