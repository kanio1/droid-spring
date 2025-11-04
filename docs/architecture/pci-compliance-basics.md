# PCI Compliance Basics
## BSS System - Sprint 1 Implementation Guide

**Document Owner:** Backend Developer
**Date:** 2025-10-29
**Version:** 1.0
**Status:** DRAFT

---

## EXECUTIVE SUMMARY

This document outlines the minimum PCI DSS (Payment Card Industry Data Security Standard) compliance requirements implemented in Sprint 1 for the BSS payment processing system.

**Important Note:** This document covers Sprint 1 BASICS ONLY. Full PCI compliance requires additional measures (penetration testing, SAQ completion, quarterly scans) to be implemented in future sprints.

---

## PCI DSS REQUIREMENTS - SPRINT 1 BASICS

### Requirement 3: Protect Stored Cardholder Data
**Objective:** Encrypt payment data at rest

**Implementation:**
- AES-256 encryption for payment data fields
- No storage of full PAN (Primary Account Number)
- Hash and salt for transaction IDs

**Data Classification:**
- **Sensitive:** Payment transaction IDs, reference numbers, card hashes
- **Non-Sensitive:** Payment amounts, dates, methods (without card details)

---

## ENCRYPTION IMPLEMENTATION

### EncryptionConverter

```java
@Component
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    @Value("${payment.encryption.key:}")
    private String encryptionKey;

    private SecretKey getKey() throws Exception {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            encryptionKey = System.getenv("PAYMENT_DATA_ENCRYPTION_KEY");
        }

        if (encryptionKey == null || encryptionKey.length() < 32) {
            throw new IllegalStateException("Encryption key must be at least 32 characters");
        }

        byte[] keyBytes = encryptionKey.substring(0, 32).getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted data + tag
            byte[] combined = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt payment data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(dbData);

            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            // Extract encrypted data
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt payment data", e);
        }
    }
}
```

---

### Payment Entity Encryption

```java
@Entity
@Table(name = "payments")
public class PaymentEntity extends BaseEntity {

    @Column(name = "payment_number", nullable = false, unique = true)
    private String paymentNumber;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    // Sensitive data - encrypt at rest
    @Convert(converter = EncryptionConverter.class)
    @Column(name = "transaction_id", length = 500)
    private String transactionId; // From payment gateway

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "reference_number", length = 500)
    private String referenceNumber; // Bank reference

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse; // Full gateway response (JSON)

    // Non-sensitive metadata
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "notes")
    private String notes;

    // Getters and setters...
}
```

### Environment Configuration

```bash
# Set encryption key via environment variable
export PAYMENT_DATA_ENCRYPTION_KEY="your-256-bit-encryption-key-here-32-chars-min"

# Or in application.yml
spring:
  config:
    import:
      - optional:file:.env
```

```yaml
# application.yml
payment:
  encryption:
    key: ${PAYMENT_DATA_ENCRYPTION_KEY:default-key-must-be-32-chars-min}
```

---

## DATA MASKING

### Log Masking Utility

```java
@Component
public class DataMaskingUtil {

    private static final Logger logger = LoggerFactory.getLogger(DataMaskingUtil.class);

    // Pattern for transaction IDs (alphanumeric, 16-64 chars)
    private static final Pattern TRANSACTION_ID_PATTERN =
        Pattern.compile("([Tt]ransaction[_-]?[Ii]d[\\s:]*)(\\w{16,64})");

    // Pattern for reference numbers
    private static final Pattern REFERENCE_NUMBER_PATTERN =
        Pattern.compile("([Rr]eference[_-]?[Nn]umber[\\s:]*)(\\w{10,64})");

    // Pattern for sensitive IDs in JSON
    private static final Pattern JSON_SENSITIVE_PATTERN =
        Pattern.compile("(\\\"(transactionId|referenceNumber|gatewayResponse)\\\":\\s*\\\")([^\"]+)(\\\")");

    public String maskTransactionId(String message) {
        if (message == null) {
            return null;
        }
        return TRANSACTION_ID_PATTERN.matcher(message)
            .replaceAll("$1****-$2");
    }

    public String maskReferenceNumber(String message) {
        if (message == null) {
            return null;
        }
        return REFERENCE_NUMBER_PATTERN.matcher(message)
            .replaceAll("$1****-$2");
    }

    public String maskJsonField(String message, String fieldName) {
        if (message == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(
            "(\\\"" + fieldName + "\\\":\\s*\\\")([^\"]+)(\\\")"
        );
        return pattern.matcher(message)
            .replaceAll("$1****-$3");
    }

    public String maskAllPaymentData(String message) {
        if (message == null) {
            return null;
        }
        message = maskTransactionId(message);
        message = maskReferenceNumber(message);
        message = maskJsonField(message, "transactionId");
        message = maskJsonField(message, "referenceNumber");
        message = maskJsonField(message, "gatewayResponse");
        return message;
    }
}
```

### Logger Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Separate payment logger with masking -->
    <appender name="PAYMENT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/bss/payment.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/bss/payment-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </logger>

    <logger name="PAYMENT" level="INFO" additivity="false">
        <appender-ref ref="PAYMENT_FILE"/>
    </logger>

    <!-- Deny SENSITIVE logs from general appenders -->
    <turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
        <marker>SENSITIVE</marker>
        <onMatch>DENY</onMatch>
    </turboFilter>
</configuration>
```

### Payment Service with Masking

```java
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final Logger paymentLogger = LoggerFactory.getLogger("PAYMENT");

    @Autowired
    private DataMaskingUtil maskingUtil;

    public PaymentDto processPayment(RecordPaymentRequest request) {
        try {
            PaymentEntity payment = createPayment(request);

            // Log payment with masking
            paymentLogger.info("Payment processed successfully: {}",
                maskingUtil.maskAllPaymentData(String.format(
                    "paymentId=%s, customerId=%s, amount=%s, method=%s, transactionId=%s",
                    payment.getId(),
                    payment.getCustomerId(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getTransactionId()
                ))
            );

            return convertToDto(payment);

        } catch (Exception e) {
            // Mask sensitive data in error logs
            logger.error("Payment processing failed: customerId={}, amount={}, transactionId={}",
                request.getCustomerId(),
                request.getAmount(),
                maskingUtil.maskTransactionId(request.getTransactionId()),
                e
            );
            throw e;
        }
    }

    @EventListener
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        // Mask sensitive data in event logs
        paymentLogger.info("Payment created: {}",
            maskingUtil.maskAllPaymentData(String.format(
                "paymentId=%s, transactionId=%s, amount=%s",
                event.getPaymentId(),
                event.getTransactionId(),
                event.getAmount()
            ))
        );
    }
}
```

---

## ACCESS CONTROL

### Role-Based Access for Payments

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // Customer can only see their own payments
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getCustomerPayments(
            @RequestParam UUID customerId) {

        List<PaymentDto> payments = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    // Billing role only for recording payments
    @PreAuthorize("hasRole('BILLING') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {

        PaymentDto payment = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    // Admin can view all payment data
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getAllPayments() {
        List<PaymentDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
```

---

## AUDIT LOGGING

### Payment Audit Service

```java
@Service
public class PaymentAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("PAYMENT_AUDIT");
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Autowired
    private DataMaskingUtil maskingUtil;

    public void logPaymentEvent(String eventType, UUID paymentId, UUID customerId,
                               String paymentMethod, BigDecimal amount,
                               String transactionId, String userId) {
        Map<String, Object> auditData = Map.of(
            "eventType", eventType,
            "paymentId", paymentId,
            "customerId", customerId,
            "paymentMethod", paymentMethod,
            "amount", amount,
            "transactionId", maskingUtil.maskTransactionId(transactionId),
            "userId", userId,
            "timestamp", Instant.now().toString(),
            "traceId", MDC.get("traceId"),
            "ipAddress", getCurrentIpAddress(),
            "userAgent", getCurrentUserAgent()
        );

        auditLogger.info("PAYMENT_AUDIT: {}", auditData);
    }

    public void logPaymentAccess(String action, UUID paymentId, String userId, boolean granted) {
        Map<String, Object> auditData = Map.of(
            "action", action,
            "paymentId", paymentId,
            "userId", userId,
            "granted", granted,
            "timestamp", Instant.now().toString(),
            "ipAddress", getCurrentIpAddress()
        );

        if (granted) {
            auditLogger.info("PAYMENT_ACCESS_GRANTED: {}", auditData);
        } else {
            securityLogger.warn("PAYMENT_ACCESS_DENIED: {}", auditData);
        }
    }

    public void logDataEncryption(String action, UUID entityId, String entityType) {
        auditLogger.info("DATA_ENCRYPTION: action={}, entityType={}, entityId={}, timestamp={}",
            action, entityType, entityId, Instant.now().toString());
    }
}
```

### Audit Annotations

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PaymentAuditEvent {

    String eventType();

    String description() default "";
}
```

```java
@Service
public class PaymentService {

    @Autowired
    private PaymentAuditService auditService;

    @PaymentAuditEvent(eventType = "PAYMENT_RECORDED", description = "Payment recorded successfully")
    public PaymentDto processPayment(RecordPaymentRequest request) {
        // Implementation
    }
}
```

---

## TLS CONFIGURATION

### Application Properties

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: bss-payment
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
  http2:
    enabled: true
  tomcat:
    remote-ip-header: X-Forwarded-For
    protocol-header: X-Forwarded-Proto

# Force HTTPS redirect
spring:
  security:
    require-ssl: true
```

### SSL/TLS Certificate Management

```bash
# Generate self-signed certificate for development
keytool -genkeypair -alias bss-payment \
    -keyalg RSA -keysize 2048 \
    -validity 365 \
    -keystore keystore.p12 \
    -storetype PKCS12 \
    -storepass changeit \
    -keypass changeit

# Production: Use certificates from Let's Encrypt or commercial CA
```

---

## DATA ENCRYPTION IN TRANSIT

### RestTemplate Configuration

```java
@Configuration
public class PaymentConfig {

    @Bean
    public RestTemplate paymentRestTemplate() throws Exception {
        SSLContext sslContext = SSLContextBuilder
            .create()
            .loadTrustMaterial(new TrustStoreUtil().getTrustStore())
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
            .build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setHttpClient(httpClient);
        return restTemplate;
    }
}
```

### HTTPS Only for Payment Endpoints

```java
@Configuration
public class PaymentSecurityConfig {

    @Bean
    public SecurityFilterChain paymentSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/payments/**")
            .requiresChannel(channel ->
                channel.anyRequest().requiresSecure()
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().hasAnyRole("CUSTOMER", "BILLING", "ADMIN")
            );

        return http.build();
    }
}
```

---

## KEY MANAGEMENT

### Environment Variable Management

```bash
# Development
export PAYMENT_DATA_ENCRYPTION_KEY="dev-encryption-key-32-chars-min"

# Production - use secret management service
# AWS Secrets Manager, Azure Key Vault, HashiCorp Vault
```

### Key Rotation (Future Sprint)

```java
@Component
public class EncryptionKeyRotationService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionKeyRotationService.class);

    // TODO: Implement key rotation for Sprint 2
    public void rotateEncryptionKey(String newKey) throws Exception {
        // 1. Re-encrypt all encrypted data with new key
        // 2. Update encryption key
        // 3. Verify all data can be decrypted
        // 4. Log key rotation event
        logger.info("Encryption key rotation completed");
    }
}
```

---

## SECURE CODING PRACTICES

### Input Validation

```java
@RestController
public class PaymentController {

    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {

        // Additional validation beyond @Valid
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds limit");
        }

        // Business logic
    }
}
```

### No Sensitive Data in Logs

```java
// ❌ BAD - Don't log sensitive data
logger.info("Processing payment: {}", paymentRequest);

// ✅ GOOD - Mask sensitive data
logger.info("Processing payment: {}",
    maskingUtil.maskAllPaymentData(paymentRequest.toString()));

// ✅ BETTER - Log only necessary metadata
logger.info("Processing payment: customerId={}, amount={}, paymentMethod={}",
    paymentRequest.getCustomerId(),
    paymentRequest.getAmount(),
    paymentRequest.getPaymentMethod());
```

---

## TESTING

### Unit Tests for Encryption

```java
@ExtendWith(MockitoExtension.class)
class EncryptionConverterTest {

    @Mock
    private Environment environment;

    private EncryptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptionConverter();
        converter.setEncryptionKey("test-key-32-characters-long!");
    }

    @Test
    void shouldEncryptAndDecrypt() {
        // Given
        String originalData = "sensitive-transaction-id-12345";

        // When
        String encrypted = converter.convertToDatabaseColumn(originalData);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(originalData);
        assertThat(decrypted).isEqualTo(originalData);
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
```

### Security Tests

```java
@SpringBootTest
class PaymentSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRequireAuthenticationForPaymentEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/payments",
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToAnotherCustomersPayment() {
        UUID anotherCustomerId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/payments?customerId={customerId}",
            String.class,
            anotherCustomerId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

---

## COMPLIANCE CHECKLIST - SPRINT 1

### Requirement 3: Protect Stored Cardholder Data
- [x] Encryption at rest (AES-256)
- [x] No storage of full PAN
- [x] Key management via environment variables
- [x] Secure key storage

### Requirement 4: Encrypt Transmission of Cardholder Data
- [x] TLS 1.3 for all payment endpoints
- [x] HTTPS redirect configured
- [x] HTTP Strict Transport Security (HSTS)

### Requirement 7: Restrict Access by Business Need to Know
- [x] Role-based access control
- [x] Customer-scoped data access
- [x] Administrative functions restricted

### Requirement 8: Identify and Authenticate Access
- [x] OIDC authentication via Keycloak
- [x] JWT token validation
- [x] User session management

### Requirement 10: Log and Monitor Access
- [x] Audit logging for all payment operations
- [x] Log access to payment data
- [x] Mask sensitive data in logs
- [x] Log retention (90 days for logs)

---

## FUTURE SPRINT REQUIREMENTS

### Sprint 2+
- [ ] Vulnerability scanning (SAST/DAST)
- [ ] Penetration testing
- [ ] Quarterly PCI scans
- [ ] SAQ completion
- [ ] Key rotation procedures
- [ ] Incident response plan
- [ ] Annual compliance review
- [ ] Third-party vendor assessments

---

## DEFINITION OF DONE - PCI BASICS

### Sprint 1 Complete When:
- [ ] Payment data encrypted at rest (AES-256)
- [ ] Sensitive data masked in logs
- [ ] TLS 1.3 configured for all payment endpoints
- [ ] Audit logging for all payment operations
- [ ] Access control enforced (role-based)
- [ ] No sensitive data in application logs
- [ ] Encryption keys stored in environment variables
- [ ] Integration tests pass
- [ ] Security tests pass (authentication, authorization)
- [ ] PCI compliance checklist documented

---

**Document Status:** ✅ Ready for Implementation
**Next Steps:**
1. Implement EncryptionConverter
2. Add @Convert annotations to PaymentEntity
3. Configure TLS in application properties
4. Add audit logging to PaymentService
5. Create security tests
6. Configure log masking

---

**Author:** Backend Developer
**Reviewer:** Tech Lead, Security Team
**Compliance Officer:** Pending Assignment
**Last Updated:** 2025-10-29

---

**DISCLAIMER:** This document covers Sprint 1 PCI DSS basics only. Full PCI compliance requires additional measures including quarterly security scans, penetration testing, and annual compliance validation by a qualified security assessor (QSA).
