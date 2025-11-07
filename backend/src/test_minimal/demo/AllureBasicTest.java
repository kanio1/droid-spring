package com.droid.bss.demo;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static io.qameta.allure.Allure.step;

/**
 * Basic Allure test to demonstrate reporting capabilities
 */
@Feature("Allure Demo Tests")
@Owner("BSS Team")
public class AllureBasicTest {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Basic test execution")
    public void testSuccessfulScenario() {
        step("Step 1: Setup test data");
        String testData = "Test data for Allure";
        Allure.addAttachment("Test Attachment", "text/plain", testData, ".txt");

        step("Step 2: Execute test logic");
        int result = 2 + 2;
        Allure.parameter("result", result);

        step("Step 3: Verify results");
        assert result == 4 : "Calculation failed";

        step("Step 4: Create report artifact");
        createTempFile("allure-demo.txt", "Allure Report Demo\nTest completed successfully!");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Test with parameters")
    public void testWithParameters() {
        step("Testing with different parameters");

        String[] testCases = {"Case 1", "Case 2", "Case 3"};
        for (String testCase : testCases) {
            step("Execute: " + testCase);
            Allure.parameter("testCase", testCase);
            // Simulate test execution
        }

        step("All test cases completed");
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Story("Test with environment info")
    public void testWithEnvironmentInfo() {
        step("Adding environment information");
        Allure.environment("Java Version", System.getProperty("java.version"));
        Allure.environment("OS", System.getProperty("os.name"));
        Allure.environment("User", System.getProperty("user.name"));

        step("Test completed");
    }

    private void createTempFile(String filename, String content) {
        try {
            File tempFile = new File(System.getProperty("java.io.tmpdir"), filename);
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(content);
            }
            Allure.addAttachment("Generated File", "text/plain", tempFile);
        } catch (IOException e) {
            Allure.addAttachment("Error", "text/plain", "Failed to create file: " + e.getMessage());
        }
    }
}
