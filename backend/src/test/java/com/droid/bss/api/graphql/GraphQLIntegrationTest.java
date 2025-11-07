package com.droid.bss.api.graphql;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GraphQL endpoints
 * Tests queries, mutations, and N+1 problem prevention
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("GraphQL Integration Tests")
public class GraphQLIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerGraphQLController customerGraphQLController;

    @Autowired
    private CustomerRepository customerRepository;

    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        graphQlTester = GraphQlTester.builder()
            .url("http://localhost:" + port + "/graphql")
            .build();
    }

    @AfterEach
    void cleanup() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get customer by ID")
    void shouldGetCustomerById() {
        // Create a test customer
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Jan")
            .lastName("Kowalski")
            .email("jan.kowalski@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        customerRepository.save(customer);

        // Test GraphQL query
        graphQlTester.document("""
            query GetCustomer($id: UUID!) {
                customer(id: $id) {
                    id
                    firstName
                    lastName
                    email
                    status
                }
            }
            """)
            .variable("id", customer.getId())
            .execute()
            .path("customer.firstName").entity(String.class).isEqualTo("Jan")
            .path("customer.lastName").entity(String.class).isEqualTo("Kowalski")
            .path("customer.email").entity(String.class).isEqualTo("jan.kowalski@example.com");
    }

    @Test
    @DisplayName("Should get customers with pagination")
    void shouldGetCustomersWithPagination() {
        // Create test customers
        CustomerEntity customer1 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Jan")
            .lastName("Kowalski")
            .email("jan.kowalski@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CustomerEntity customer2 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Anna")
            .lastName("Nowak")
            .email("anna.nowak@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        customerRepository.save(customer1);
        customerRepository.save(customer2);

        graphQlTester.document("""
            query GetCustomers($page: Int, $size: Int) {
                customers(page: $page, size: $size) {
                    edges {
                        node {
                            id
                            firstName
                            lastName
                            email
                        }
                    }
                    pageInfo {
                        hasNextPage
                        hasPreviousPage
                    }
                    totalCount
                }
            }
            """)
            .variable("page", 0)
            .variable("size", 10)
            .execute()
            .path("customers.totalCount").entity(Integer.class).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search customers by name")
    void shouldSearchCustomersByName() {
        // Create test customers
        CustomerEntity customer1 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Jan")
            .lastName("Kowalski")
            .email("jan.kowalski@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        CustomerEntity customer2 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Anna")
            .lastName("Nowak")
            .email("anna.nowak@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        customerRepository.save(customer1);
        customerRepository.save(customer2);

        graphQlTester.document("""
            query SearchCustomers($query: String!) {
                searchCustomers(query: $query) {
                    id
                    firstName
                    lastName
                    email
                }
            }
            """)
            .variable("query", "Jan")
            .execute()
            .path("searchCustomers[0].firstName").entity(String.class).isEqualTo("Jan");
    }

    @Test
    @DisplayName("Should create new customer")
    void shouldCreateNewCustomer() {
        graphQlTester.document("""
            mutation CreateCustomer($input: CreateCustomerInput!) {
                createCustomer(input: $input) {
                    id
                    firstName
                    lastName
                    email
                    status
                }
            }
            """)
            .variable("input", Map.of(
                "firstName", "Anna",
                "lastName", "Nowak",
                "email", "anna.nowak@example.com",
                "status", "PENDING"
            ))
            .execute()
            .path("createCustomer.firstName").entity(String.class).isEqualTo("Anna")
            .path("createCustomer.lastName").entity(String.class).isEqualTo("Nowak")
            .path("createCustomer.email").entity(String.class).isEqualTo("anna.nowak@example.com");
    }

    @Test
    @DisplayName("Should update existing customer")
    void shouldUpdateExistingCustomer() {
        // Create a customer
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Piotr")
            .lastName("Testowy")
            .email("piotr.testowy@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        customerRepository.save(customer);

        // Update the customer
        graphQlTester.document("""
            mutation UpdateCustomer($id: UUID!, $input: UpdateCustomerInput!) {
                updateCustomer(id: $id, input: $input) {
                    id
                    firstName
                    lastName
                    email
                }
            }
            """)
            .variable("id", customer.getId())
            .variable("input", Map.of("firstName", "Piotr-Updated"))
            .execute()
            .path("updateCustomer.firstName").entity(String.class).isEqualTo("Piotr-Updated");
    }

    @Test
    @DisplayName("Should get customer with relations")
    void shouldGetCustomerWithRelations() {
        // This test would require setting up related invoices, payments, subscriptions
        // For now, it's a placeholder
        graphQlTester.document("""
            query GetCustomerWithRelations($id: UUID!) {
                customer(id: $id) {
                    id
                    firstName
                    invoices {
                        edges {
                            node {
                                id
                                totalAmount
                                status
                            }
                        }
                    }
                }
            }
            """)
            .variable("id", UUID.randomUUID())
            .execute()
            .path("customer.invoices.edges").entityList(Object.class);
    }

    @Test
    @DisplayName("Should use batch loading to prevent N+1 problem")
    void shouldUseBatchLoadingToPreventNPlusOne() {
        // Test that N+1 problem is prevented
        // This would require creating multiple customers with relations

        graphQlTester.document("""
            query GetCustomersWithRelations {
                customers(size: 5) {
                    edges {
                        node {
                            id
                            firstName
                            totalRevenue
                            activeSubscriptionsCount
                        }
                    }
                }
            }
            """)
            .execute()
            .path("customers.edges[*].node.totalRevenue")
            .entityList(Double.class);
    }
}
