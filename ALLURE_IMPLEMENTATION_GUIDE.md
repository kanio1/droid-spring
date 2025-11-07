# Allure Test Reporting - Implementation Guide

## Overview

This guide demonstrates how to use Allure for test reporting in the BSS Backend project. Allure is a flexible, lightweight multi-language test reporting tool that provides clear visual reports.

## Current Configuration

### 1. Allure Dependencies (pom.xml)

The project already has Allure configured with the following dependencies:

```xml
<!-- Allure Test Reporting -->
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <version>2.24.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-rest-assured</artifactId>
    <version>2.24.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-attachments</artifactId>
    <version>2.24.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Allure Maven Plugin

```xml
<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.12.0</version>
    <configuration>
        <reportVersion>2.24.0</reportVersion>
        <resultsDirectory>${project.build.directory}/allure-results</resultsDirectory>
        <reportDirectory>${project.build.directory}/allure-report</reportDirectory>
        <propertiesFilePath>allure.properties</propertiesFilePath>
        <checkLinks>false</checkLinks>
    </configuration>
</plugin>
```

### 3. Allure Configuration (allure.properties)

```properties
# Allure Reporting Configuration
allure.results.pattern=**/allure-results/*.json
allure.attachments.enabled=true
allure.encoding=UTF-8
allure.metrics.enabled=true
allure.environment.name=BSS Backend Test Environment
allure.environment.url=http://localhost:8080
allure.project.name=BSS Backend
allure.report.title=BSS Backend - Test Report
```

## Allure Features Demonstrated

### 1. Test Annotations

Allure provides rich annotations for documenting tests:

```java
import io.qameta.allure.*;

@Epic("Customer Management")
@Feature("Customer CRUD Operations")
class CustomerTest {

    @Test
    @Story("Create Customer")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Should create a customer with valid data")
    @Link(name = "Documentation", url = "https://docs.example.com/customer")
    @Issue("BSS-123")
    void shouldCreateCustomer() {
        // Test implementation
    }

    @Test
    @Story("Update Customer")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TMS-456")
    void shouldUpdateCustomer() {
        // Test implementation
    }
}
```

### 2. Steps

Break down tests into meaningful steps:

```java
@Test
void complexOperation() {
    step("Step 1: Initialize", () -> {
        // Initialization logic
    });
    
    step("Step 2: Process data", () -> {
        String result = processData("test input");
        Assertions.assertNotNull(result);
    });
    
    step("Step 3: Validate results", () -> {
        boolean isValid = validateResults();
        Assertions.assertTrue(isValid);
    });
}

@Step("Process data: {input}")
private String processData(String input) {
    // Processing logic
    return "Processed: " + input;
}
```

### 3. Attachments

Add files, screenshots, and data to test reports:

```java
@Test
void testWithAttachments() {
    // Add text attachment
    String data = "Important test data";
    Allure.attachment("Test Data", data);
    
    // Add JSON attachment
    String json = "{ \"key\": \"value\" }";
    Allure.attachment("Response JSON", "application/json", json.getBytes());
    
    // Add file attachment
    byte[] fileContent = Files.readAllBytes(path);
    Allure.attachment("Downloaded File", "application/octet-stream", fileContent);
}
```

### 4. Parameters

Pass parameters to tests:

```java
@Test
void parameterizedTest(String username, String role) {
    parameter("Username", username);
    parameter("User Role", role);
    
    // Test implementation
}
```

### 5. Labels and Categories

Organize tests with labels:

```java
@Test
@Owner("Development Team")
@Tag("smoke")
@Tag("customer")
void smokeTest() {
    // Test implementation
}
```

Define categories in `allure.properties`:

```properties
allure.categories.condition.failed.name=Failed Tests
allure.categories.condition.failed.matchesRegex=.*

allure.categories.broken.name=Broken Tests
allure.categories.broken.matchesRegex=.*
```

### 6. Environment Information

Record environment details:

```java
@BeforeAll
static void setupEnvironment() {
    AllureConfig.recordEnvironmentInfo();
    AllureConfig.addTestCategory("Integration Tests");
}
```

### 7. AllureConfig Utilities

The project includes `AllureConfig.java` with useful utilities:

```java
import com.droid.bss.config.AllureConfig;

@Test
void testWithAllureConfig() {
    AllureConfig.setSeverity("critical");
    AllureConfig.addFeature("Customer Management");
    AllureConfig.addStory("Create Customer");
    AllureConfig.addTag("smoke");
    
    // Test implementation
}
```

## Running Tests with Allure

### 1. Run Tests and Generate Results

```bash
# Run tests with Allure
mvn clean test

# Results will be generated in: target/allure-results/
```

### 2. Generate Allure Report

```bash
# Generate report from results
mvn allure:serve

# Or generate static report
mvn allure:report

# Report will be generated in: target/allure-report/
```

### 3. Using the Generate Script

The project includes a convenience script:

```bash
# Run the script
./backend/generate-allure-report.sh

# Or with serve option to keep server running
./backend/generate-allure-report.sh --serve
```

This script will:
- Run all tests
- Generate Allure results
- Start a local server (default port 5050)
- Open the report in your browser

### 4. Open Generated Report

After generation, the report will be available at:
- Local server: http://localhost:5050
- Static files: `target/allure-report/index.html`

## Allure Report Features

The generated Allure report provides:

1. **Overview Dashboard**
   - Total tests count
   - Passed/Failed/Broken/Skipped tests
   - Test duration graphs
   - Environment information

2. **Test Categories**
   - Tests grouped by severity (Critical, Normal, Minor, Trivial)
   - Tests grouped by feature/epic
   - Flaky tests identification

3. **Detailed Test View**
   - Test description
   - Step-by-step execution
   - Attachments (logs, files, screenshots)
   - Environment variables
   - Links (issues, docs, TMS)

4. **Trends**
   - Test execution trends over time
   - Success rate trends
   - Duration trends

## Example Test with Allure

Here's a complete example:

```java
package com.droid.bss.example;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("Payment Processing")
@Feature("Payment Operations")
class PaymentTest {

    @Test
    @Story("Process Valid Payment")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Should process a valid payment successfully")
    @Owner("Payment Team")
    @Tag("payment")
    @Tag("smoke")
    void shouldProcessValidPayment() {
        // Given
        String paymentId = "PAY-123";
        double amount = 99.99;
        
        parameter("Payment ID", paymentId);
        parameter("Amount", String.valueOf(amount));
        
        step("Step 1: Create payment request", () -> {
            PaymentRequest request = createPaymentRequest(paymentId, amount);
            assertNotNull(request);
        });
        
        step("Step 2: Process payment", () -> {
            PaymentResult result = processPayment(paymentId, amount);
            assertNotNull(result);
            assertEquals("SUCCESS", result.getStatus());
        });
        
        step("Step 3: Verify payment", () -> {
            PaymentStatus status = getPaymentStatus(paymentId);
            assertEquals(PaymentStatus.COMPLETED, status);
        });
        
        // Add attachment
        Allure.attachment("Payment Confirmation", 
            "Payment " + paymentId + " processed successfully for amount " + amount);
    }

    @Step("Create payment request: {paymentId}")
    private PaymentRequest createPaymentRequest(String paymentId, double amount) {
        System.out.println("Creating payment request...");
        return new PaymentRequest(paymentId, amount);
    }

    @Step("Process payment: {paymentId}")
    private PaymentResult processPayment(String paymentId, double amount) {
        System.out.println("Processing payment...");
        return new PaymentResult("SUCCESS", paymentId);
    }

    @Step("Get payment status: {paymentId}")
    private PaymentStatus getPaymentStatus(String paymentId) {
        System.out.println("Getting payment status...");
        return PaymentStatus.COMPLETED;
    }
}
```

## Best Practices

1. **Use Descriptive Test Names**
   - Clear, human-readable test names
   - Include expected behavior

2. **Document with @Description**
   - Explain what the test validates
   - Include acceptance criteria

3. **Break Down Complex Tests**
   - Use @Step to break down complex operations
   - Make steps reusable

4. **Add Attachments**
   - Log files for debugging
   - Request/response data
   - Screenshots for UI tests

5. **Use Tags for Filtering**
   - @Tag("smoke") for smoke tests
   - @Tag("regression") for regression tests
   - @Tag("slow") for time-consuming tests

6. **Link to Documentation**
   - Use @Link for documentation URLs
   - Use @Issue for bug tracker links
   - Use @TmsLink for test management system

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Tests with Allure

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run tests
        run: mvn clean test
      
      - name: Generate Allure Report
        run: mvn allure:report
      
      - name: Upload Allure Report
        uses: actions/upload-artifact@v3
        with:
          name: allure-report
          path: target/allure-report/
```

## Troubleshooting

### Issue: Allure results not generated

**Solution:** Ensure allure-junit5 is in test classpath:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

### Issue: Report shows no tests

**Solution:** Check results directory:

```bash
ls target/allure-results/
# Should contain .json files
```

### Issue: Server won't start

**Solution:** Check if port is available:

```bash
# Change port in generate-allure-report.sh
PORT=5051
```

## Resources

- [Allure Documentation](https://docs.qameta.io/allure/)
- [Allure JUnit5](https://docs.qameta.io/allure/#_junit_5)
- [Allure Maven Plugin](https://docs.qameta.io/allure/#_maven)
- [Allure Reports](https://demo.qameta.io/allure/#/)

## Summary

Allure provides powerful test reporting capabilities that help teams:
- Visualize test execution
- Debug failed tests quickly
- Track test trends over time
- Organize tests by features and severities
- Document test results for stakeholders

The BSS Backend project is already configured with Allure. Simply add Allure annotations to your tests and run the generate script to get beautiful, informative test reports.
