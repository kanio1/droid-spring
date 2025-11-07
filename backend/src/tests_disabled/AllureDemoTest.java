import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.qameta.allure.Allure.*;

import java.util.UUID;

@Epic("Demo Tests")
@Feature("Allure Reporting")
public class AllureDemoTest {

    @Test
    @Story("Basic Test")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This test demonstrates basic Allure reporting")
    void testBasicFunctionality() {
        parameter("Test Parameter", "value123");
        description("Testing basic functionality with Allure annotations");
        
        // Simulate test execution
        String result = perform(() -> {
            return "Test Result: " + UUID.randomUUID().toString();
        });
        
        System.out.println(result);
        Assertions.assertNotNull(result);
    }

    @Test
    @Story("Attachment Test")
    @Severity(SeverityLevel.MINOR)
    @Description("This test demonstrates Allure attachments")
    void testWithAttachment() {
        String data = "Important test data: " + System.currentTimeMillis();
        
        // Add text attachment
        Allure.attachment("Test Data", data);
        
        // Add JSON attachment
        String jsonData = "{ \"key\": \"value\", \"timestamp\": " + System.currentTimeMillis() + " }";
        Allure.attachment("JSON Data", "application/json", jsonData.getBytes());
        
        System.out.println("Attachment test completed");
        Assertions.assertNotNull(jsonData);
    }

    @Test
    @Story("Step Test")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test demonstrates Allure steps")
    void testWithSteps() {
        step("Step 1: Initialize", () -> {
            System.out.println("Initialization step");
            Assertions.assertTrue(true, "Initialization successful");
        });
        
        step("Step 2: Process", () -> {
            String result = processData("test input");
            Assertions.assertNotNull(result);
        });
        
        step("Step 3: Validate", () -> {
            boolean validationResult = validateResult();
            Assertions.assertTrue(validationResult, "Validation successful");
        });
    }

    @Step("Process data: {input}")
    private String processData(String input) {
        System.out.println("Processing: " + input);
        return "Processed: " + input;
    }

    @Step("Validate result")
    private boolean validateResult() {
        System.out.println("Validating result...");
        return true;
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println("Test completed: " + testInfo.getDisplayName());
    }
}
