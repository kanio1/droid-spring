package com.droid.bss.performance.jmh;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * JMH Benchmarks for Domain Operations
 *
 * Measures performance of core domain operations:
 * 1. Entity creation
 * 2. Business logic execution
 * 3. Validation operations
 * 4. State transitions
 * 5. Object creation/GC impact
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class DomainOperationBenchmark {

    private Customer customer;
    private String email;
    private String customerId;

    @Setup
    public void setup() {
        customerId = UUID.randomUUID().toString();
        email = "test" + System.currentTimeMillis() + "@example.com";
        customer = CustomerFactory.createCustomer(
            customerId,
            "John",
            "Doe",
            email,
            "+1234567890"
        );
    }

    // ========== ENTITY CREATION BENCHMARKS ==========

    @Benchmark
    public void benchmarkCustomerCreation() {
        Customer newCustomer = CustomerFactory.createCustomer(
            UUID.randomUUID().toString(),
            "Jane",
            "Smith",
            "jane" + System.currentTimeMillis() + "@example.com",
            "+0987654321"
        );
    }

    @Benchmark
    public void benchmarkCustomerCreationWithoutFactory() {
        Customer newCustomer = new Customer(
            UUID.randomUUID().toString(),
            "Jane",
            "Smith",
            "jane" + System.currentTimeMillis() + "@example.com",
            "+0987654321"
        );
    }

    // ========== BUSINESS LOGIC BENCHMARKS ==========

    @Benchmark
    public void benchmarkCustomerValidation() {
        customer.validate();
    }

    @Benchmark
    public void benchmarkEmailValidation() {
        if (email != null && email.contains("@")) {
            // Simple email validation
            String[] parts = email.split("@");
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                // Valid
            }
        }
    }

    @Benchmark
    public void benchmarkPhoneValidation() {
        String phone = customer.getPhone();
        if (phone != null && phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            // Valid phone
        }
    }

    // ========== STATE TRANSITION BENCHMARKS ==========

    @Benchmark
    public void benchmarkCustomerStateUpdate() {
        customer.updateEmail("updated" + System.currentTimeMillis() + "@example.com");
    }

    @Benchmark
    public void benchmarkCustomerPropertiesUpdate() {
        customer.updateFirstName("UpdatedFirst");
        customer.updateLastName("UpdatedLast");
    }

    @Benchmark
    public void benchmarkStringOperations() {
        // Common string operations in domain logic
        String firstName = customer.getFirstName();
        String lastName = customer.getLastName();
        String fullName = firstName + " " + lastName;
        String upperName = fullName.toUpperCase();
        String trimmedName = upperName.trim();
    }

    // ========== OBJECT CREATION BENCHMARKS ==========

    @Benchmark
    public void benchmarkStringConcatenation() {
        String result = "Customer: " + customer.getFirstName() + " " + customer.getLastName();
    }

    @Benchmark
    public void benchmarkStringBuilder() {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer: ");
        sb.append(customer.getFirstName());
        sb.append(" ");
        sb.append(customer.getLastName());
        String result = sb.toString();
    }

    @Benchmark
    public void benchmarkStringFormat() {
        String result = String.format("Customer: %s %s",
            customer.getFirstName(), customer.getLastName());
    }

    // ========== COLLECTION OPERATIONS ==========

    @Benchmark
    public void benchmarkStringListCreation() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add(customer.getFirstName());
        list.add(customer.getLastName());
        list.add(customer.getEmail());
        list.add(customer.getPhone());
    }

    @Benchmark
    public void benchmarkStringListIteration() {
        java.util.List<String> list = java.util.Arrays.asList(
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone()
        );

        for (String item : list) {
            if (item != null && !item.isEmpty()) {
                // Process
            }
        }
    }

    // ========== UUID OPERATIONS ==========

    @Benchmark
    public void benchmarkUuidGeneration() {
        UUID.randomUUID();
    }

    @Benchmark
    public void benchmarkUuidToString() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
    }

    @Benchmark
    public void benchmarkUuidFromString() {
        String uuidString = UUID.randomUUID().toString();
        UUID uuid = UUID.fromString(uuidString);
    }

    // ========== COMPARISON BENCHMARKS ==========

    @Benchmark
    public void benchmarkStringEquals() {
        customer.getEmail().equals("test@example.com");
    }

    @Benchmark
    public void benchmarkStringEqualsIgnoreCase() {
        customer.getEmail().equalsIgnoreCase("TEST@EXAMPLE.COM");
    }

    @Benchmark
    public void benchmarkStringContains() {
        customer.getEmail().contains("@");
    }

    // ========== MATHEMATICAL OPERATIONS ==========

    @Benchmark
    public void benchmarkBigDecimalOperations() {
        java.math.BigDecimal amount1 = new java.math.BigDecimal("99.99");
        java.math.BigDecimal amount2 = new java.math.BigDecimal("49.99");
        java.math.BigDecimal total = amount1.add(amount2);
        java.math.BigDecimal tax = total.multiply(new java.math.BigDecimal("0.23"));
        java.math.BigDecimal finalTotal = total.add(tax);
    }

    @Benchmark
    public void benchmarkDoubleMathOperations() {
        double amount1 = 99.99;
        double amount2 = 49.99;
        double total = amount1 + amount2;
        double tax = total * 0.23;
        double finalTotal = total + tax;
    }

    // ========== GARBAGE COLLECTION IMPACT ==========

    @Benchmark
    public void benchmarkMemoryAllocation() {
        // Test memory allocation patterns
        Object[] objects = new Object[100];
        for (int i = 0; i < 100; i++) {
            objects[i] = new Object();
        }
    }

    @Benchmark
    public void benchmarkObjectReuse() {
        // Test object reuse vs recreation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.setLength(0);
            sb.append("Item ").append(i);
            String result = sb.toString();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(DomainOperationBenchmark.class.getSimpleName())
            .result("jmh-domain-results.json")
            .resultFormat(ResultFormatType.JSON)
            .build();

        new Runner(opt).run();
    }
}
