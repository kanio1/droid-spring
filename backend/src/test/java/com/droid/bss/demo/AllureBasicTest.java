package com.droid.bss.demo;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;

/**
 * Simple Allure test with working API
 */
@Feature("Allure Demo Tests")
public class AllureBasicTest {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Story("Basic test with steps")
    public void testBasicFunctionality() {
        step("Step 1: Perform action");
        performAction();

        step("Step 2: Verify result");
        verifyResult();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Test with attachments")
    public void testWithAttachment() {
        step("Creating test data");
        String testData = "Test data for Allure report";

        // Add text attachment
        Allure.addAttachment("Test Data", "text/plain", testData);

        step("Test completed");
    }

    @Test
    @Severity(SeverityLevel.MINOR)
    @Story("Another demo test")
    public void testAnotherScenario() {
        step("Setup test");
        setup();

        step("Execute test");
        execute();

        step("Clean up");
        cleanup();
    }

    private void performAction() {
        // Simple action
    }

    private void verifyResult() {
        // Simple verification
    }

    private void setup() {
        // Setup
    }

    private void execute() {
        // Execute
    }

    private void cleanup() {
        // Clean up
    }
}
