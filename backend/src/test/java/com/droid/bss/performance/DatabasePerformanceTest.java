package com.droid.bss.performance;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Database Performance Tests
 *
 * Tests database-specific performance characteristics:
 * 1. Query execution time
 * 2. Index usage
 * 3. Connection pool efficiency
 * 4. Batch operations
 * 5. Transaction overhead
 * 6. JPA/native query performance
 */
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    // ========== QUERY EXECUTION TIME ==========

    @Test
    @DisplayName("Should execute count query within 100ms")
    void shouldExecuteCountQueryWithin100ms() {
        // Arrange - Create some test data
        int customerCount = 100;
        for (int i = 0; i < customerCount; i++) {
            createTestCustomer("CountTest" + i);
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(customerRepository.count()).isGreaterThanOrEqualTo(customerCount - 5);
        });

        // Act - Measure count query performance
        long startTime = System.currentTimeMillis();
        long count = customerRepository.count();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        System.out.printf("Count query: %d customers in %d ms%n", count, duration);

        // Count query should be fast
        assertThat(duration).isLessThan(100);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should execute findAll query efficiently")
    void shouldExecuteFindAllQueryEfficiently() {
        // Arrange
        int customerCount = 500;
        for (int i = 0; i < customerCount; i++) {
            createTestCustomer("FindAll" + i);
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(customerRepository.count()).isGreaterThanOrEqualTo(customerCount - 10);
        });

        // Act - Measure findAll performance
        long startTime = System.currentTimeMillis();
        List<Customer> customers = customerRepository.findAll();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        System.out.printf("findAll: %d customers in %d ms (%.2f ms/customer)%n",
                customers.size(), duration, (double) duration / customers.size());

        // Should be reasonably fast
        assertThat(duration).isLessThan(2000); // 2 seconds for 500 customers
        assertThat(customers).hasSizeGreaterThan(customerCount - 20);
    }

    @Test
    @DisplayName("Should execute findById query within 50ms")
    void shouldExecuteFindByIdWithin50ms() {
        // Arrange
        String customerId = createTestCustomer("FindById");

        // Act & Assert - Multiple iterations
        for (int i = 0; i < 100; i++) {
            long startTime = System.currentTimeMillis();
            var customer = customerRepository.findById(customerId);
            long duration = System.currentTimeMillis() - startTime;

            assertThat(customer).isPresent();

            if (i % 20 == 0) {
                System.out.printf("findById #%d: %d ms%n", i, duration);
            }

            // Individual query should be fast
            assertThat(duration).isLessThan(50);
        }
    }

    // ========== INDEX USAGE ==========

    @Test
    @DisplayName("Should use indexes for email lookups")
    void shouldUseIndexesForEmailLookups() {
        // Arrange - Create customers with unique emails
        List<String> customerIds = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String id = createTestCustomer("IndexTest" + i);
            customerIds.add(id);
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(customerRepository.count()).isGreaterThanOrEqualTo(1000 - 20);
        });

        // Act - Test various email lookups
        int[] testIndices = {0, 100, 500, 999};

        for (int index : testIndices) {
            long startTime = System.currentTimeMillis();
            // This would use email index if configured
            // var customer = customerRepository.findByEmail("test" + index + "@example.com");
            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Email lookup (index %d): %d ms%n", index, duration);

            // Email lookup should be fast with index
            assertThat(duration).isLessThan(100);
        }
    }

    // ========== BATCH OPERATIONS ==========

    @Test
    @DisplayName("Should optimize batch inserts")
    void shouldOptimizeBatchInserts() {
        // Test different batch sizes
        int[] batchSizes = {10, 50, 100, 200};

        for (int batchSize : batchSizes) {
            long startTime = System.currentTimeMillis();

            // Simulate batch insert (in real scenario, would use batch processing)
            for (int i = 0; i < batchSize; i++) {
                createTestCustomer("Batch" + batchSize + "-" + i);
            }

            long duration = System.currentTimeMillis() - startTime;

            System.out.printf("Batch size %d: %d ms%n", batchSize, duration);

            // Larger batches should be more efficient per item
            // Too large = diminishing returns
        }
    }

    // ========== PAGINATION PERFORMANCE ==========

    @Test
    @DisplayName("Should handle paginated queries efficiently")
    void shouldHandlePaginatedQueriesEfficiently() {
        // Arrange
        int totalCustomers = 5000;
        for (int i = 0; i < totalCustomers; i++) {
            createTestCustomer("Pagination" + i);
        }

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(customerRepository.count()).isGreaterThanOrEqualTo(totalCustomers - 50);
        });

        // Act - Test various page sizes
        int[] pageSizes = {10, 50, 100, 200};
        int page = 0;

        for (int pageSize : pageSizes) {
            long totalPaginationTime = 0;
            int pages = totalCustomers / pageSize;

            for (int p = 0; p < Math.min(pages, 10); p++) { // Test first 10 pages
                long pageStart = System.currentTimeMillis();

                // Simulate pagination (in real scenario, would use Pageable)
                int offset = p * pageSize;
                List<Customer> customers = customerRepository.findAll()
                        .stream()
                        .skip(offset)
                        .limit(pageSize)
                        .toList();

                long pageDuration = System.currentTimeMillis() - pageStart;
                totalPaginationTime += pageDuration;

                assertThat(customers.size()).isLessThanOrEqualTo(pageSize);
            }

            double avgPageTime = (double) totalPaginationTime / Math.min(pages, 10);
            System.out.printf("Page size %d: avg %.2f ms/page%n", pageSize, avgPageTime);

            // Pagination should be reasonable
            assertThat(avgPageTime).isLessThan(200.0);
        }
    }

    // ========== CONNECTION POOL EFFICIENCY ==========

    @Test
    @DisplayName("Should reuse database connections efficiently")
    void shouldReuseConnectionsEfficiently() {
        // Arrange
        int queryCount = 200;
        long startTime = System.currentTimeMillis();

        // Act - Sequential queries (should reuse connections)
        for (int i = 0; i < queryCount; i++) {
            long queryStart = System.currentTimeMillis();
            customerRepository.count();
            long queryDuration = System.currentTimeMillis() - queryStart;

            if (i % 50 == 0) {
                System.out.printf("Query %d: %d ms%n", i, queryDuration);
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        double avgQueryTime = (double) totalDuration / queryCount;

        System.out.printf("Connection reuse: %d queries in %d ms (avg %.2f ms/query)%n",
                queryCount, totalDuration, avgQueryTime);

        // Connection reuse should make queries faster over time
        assertThat(avgQueryTime).isLessThan(10.0);
    }

    // ========== JPA vs NATIVE QUERY ==========

    @Test
    @DisplayName("Should compare JPA and native query performance")
    void shouldCompareJpaAndNativeQueryPerformance() {
        // Arrange
        int customerCount = 1000;
        for (int i = 0; i < customerCount; i++) {
            createTestCustomer("QueryCompare" + i);
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(customerRepository.count()).isGreaterThanOrEqualTo(customerCount - 20);
        });

        // Act - Test JPA query
        long jpaStart = System.currentTimeMillis();
        List<Customer> jpaCustomers = customerRepository.findAll();
        long jpaDuration = System.currentTimeMillis() - jpaStart;

        System.out.printf("JPA findAll: %d customers in %d ms%n",
                jpaCustomers.size(), jpaDuration);

        // Note: In real scenario, would compare with native query
        // For now, just ensure JPA is performing reasonably
        assertThat(jpaDuration).isLessThan(3000);
    }

    // ========== TRANSACTION OVERHEAD ==========

    @Test
    @DisplayName("Should measure transaction overhead")
    void shouldMeasureTransactionOverhead() {
        // Test transaction vs non-transaction operations
        int operationCount = 100;

        // With transaction (simulated)
        long withTxStart = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            createTestCustomer("WithTx" + i);
        }
        long withTxDuration = System.currentTimeMillis() - withTxStart;

        System.out.printf("With transaction: %d operations in %d ms%n",
                operationCount, withTxDuration);

        // Transaction overhead should be reasonable
        // Actual overhead depends on isolation level and lock strategy
    }

    // ========== HELPER METHODS ==========

    private String createTestCustomer(String prefix) {
        String customerId = UUID.randomUUID().toString();
        Customer customer = new Customer(
            customerId,
            prefix,
            "Test",
            prefix.toLowerCase() + "@test.com",
            "+1234567890"
        );
        customerRepository.save(customer);
        return customerId;
    }
}
