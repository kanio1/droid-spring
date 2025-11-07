package com.droid.bss.infrastructure;

import com.droid.bss.domain.product.*;
import com.droid.bss.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scaffolding for ProductRepository
 * Tests all custom query methods and CRUD operations
 */
@DataJpaTest
@Testcontainers
@DisplayName("ProductRepository JPA Layer - Test Scaffolding")
class ProductRepositoryDataJpaTest {

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
    }

    @Autowired
    private ProductRepository productRepository;

    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        testProduct = new ProductEntity(
            "PROD-001",
            "Test Product",
            "Test product description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            new BigDecimal("99.99"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(365)
        );
    }

    // CRUD Operations

    @Test
    @DisplayName("should save and retrieve product by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldSaveAndRetrieveProductById() {
        // Given
        ProductEntity savedProduct = productRepository.save(testProduct);

        // When
        var retrieved = productRepository.findById(savedProduct.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductCode()).isEqualTo("PROD-001");
        assertThat(retrieved.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("should save multiple products and retrieve all")
    @Disabled("Test scaffolding - implementation required")
    void shouldSaveMultipleProductsAndRetrieveAll() {
        // Given
        ProductEntity product1 = testProduct;
        ProductEntity product2 = new ProductEntity(
            "PROD-002",
            "Product 2",
            "Description 2",
            ProductType.TARIFF,
            ProductCategory.TV,
            new BigDecimal("199.99"),
            "PLN",
            "YEARLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(product1, product2));

        // When
        List<ProductEntity> allProducts = productRepository.findAll();

        // Then
        assertThat(allProducts).hasSize(2);
    }

    @Test
    @DisplayName("should delete product by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldDeleteProductById() {
        // Given
        ProductEntity savedProduct = productRepository.save(testProduct);

        // When
        productRepository.deleteById(savedProduct.getId());

        // Then
        assertThat(productRepository.findById(savedProduct.getId())).isEmpty();
    }

    // Custom Query Methods

    @Test
    @DisplayName("should find product by product code")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductByProductCode() {
        // Given
        productRepository.save(testProduct);

        // When
        var found = productRepository.findByProductCode("PROD-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("should return empty when product code not found")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturnEmptyWhenProductCodeNotFound() {
        // When
        var found = productRepository.findByProductCode("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find products by status with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByStatusWithPagination() {
        // Given
        ProductEntity activeProduct1 = testProduct;
        ProductEntity activeProduct2 = new ProductEntity(
            "PROD-002",
            "Active Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        ProductEntity inactiveProduct = new ProductEntity(
            "PROD-003",
            "Inactive Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.INACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(activeProduct1, activeProduct2, inactiveProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);

        // Then
        assertThat(activeProducts.getContent()).hasSize(2);
        assertThat(activeProducts.getContent())
            .extracting(ProductEntity::getProductCode)
            .containsExactlyInAnyOrder("PROD-001", "PROD-002");
    }

    @Test
    @DisplayName("should find products by product type with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByProductTypeWithPagination() {
        // Given
        productRepository.save(testProduct);
        ProductEntity product2 = new ProductEntity(
            "PROD-002",
            "Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.save(product2);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> serviceProducts = productRepository.findByProductType(ProductType.SERVICE, pageable);

        // Then
        assertThat(serviceProducts.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find products by product category with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByProductCategoryWithPagination() {
        // Given
        ProductEntity basicProduct1 = testProduct;
        ProductEntity basicProduct2 = new ProductEntity(
            "PROD-002",
            "Basic Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        ProductEntity premiumProduct = new ProductEntity(
            "PROD-003",
            "Premium Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.TV,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(basicProduct1, basicProduct2, premiumProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> basicProducts = productRepository.findByCategory(ProductCategory.BASIC, pageable);

        // Then
        assertThat(basicProducts.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find active products within validity period")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindActiveProductsWithinValidityPeriod() {
        // Given
        ProductEntity validActiveProduct = testProduct;
        ProductEntity futureProduct = new ProductEntity(
            "PROD-002",
            "Future Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().plusDays(30),
            LocalDate.now().plusYears(1)
        );
        ProductEntity expiredProduct = new ProductEntity(
            "PROD-003",
            "Expired Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusYears(2),
            LocalDate.now().minusDays(30)
        );
        productRepository.saveAll(List.of(validActiveProduct, futureProduct, expiredProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> activeProducts = productRepository.findActiveProducts(ProductStatus.ACTIVE, pageable);

        // Then
        assertThat(activeProducts.getContent()).hasSize(1);
        assertThat(activeProducts.getContent().get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should search products by name or description")
    @Disabled("Test scaffolding - implementation required")
    void shouldSearchProductsByNameOrDescription() {
        // Given
        ProductEntity product1 = testProduct;
        ProductEntity product2 = new ProductEntity(
            "PROD-002",
            "Premium Service",
            "Premium service description",
            ProductType.SERVICE,
            ProductCategory.TV,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(product1, product2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> searchResults = productRepository.searchProducts("Test", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should find products by price range")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByPriceRange() {
        // Given
        ProductEntity cheapProduct = new ProductEntity(
            "PROD-001",
            "Cheap Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            new BigDecimal("50.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        ProductEntity expensiveProduct = new ProductEntity(
            "PROD-002",
            "Expensive Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.TV,
            new BigDecimal("200.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(cheapProduct, expensiveProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> inRangeProducts = productRepository.findByPriceRange(40.00, 150.00, pageable);

        // Then
        assertThat(inRangeProducts.getContent()).hasSize(1);
        assertThat(inRangeProducts.getContent().get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should find products expiring within given days")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsExpiringWithinGivenDays() {
        // Given
        ProductEntity expiringSoonProduct = new ProductEntity(
            "PROD-001",
            "Expiring Soon Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(15)
        );
        ProductEntity expiringLaterProduct = new ProductEntity(
            "PROD-002",
            "Expiring Later Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(100)
        );
        ProductEntity expiredProduct = new ProductEntity(
            "PROD-003",
            "Expired Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(10)
        );
        productRepository.saveAll(List.of(expiringSoonProduct, expiringLaterProduct, expiredProduct));

        // When
        List<ProductEntity> expiringProducts = productRepository.findExpiringProducts(LocalDate.now().plusDays(30));

        // Then
        assertThat(expiringProducts).hasSize(1);
        assertThat(expiringProducts.get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should check if product code exists")
    @Disabled("Test scaffolding - implementation required")
    void shouldCheckIfProductCodeExists() {
        // Given
        productRepository.save(testProduct);

        // When & Then
        assertThat(productRepository.existsByProductCode("PROD-001")).isTrue();
        assertThat(productRepository.existsByProductCode("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("should find products by feature key")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByFeatureKey() {
        // Given
        ProductEntity product1 = testProduct;
        ProductFeatureEntity feature1 = new ProductFeatureEntity(
            product1,
            "data_limit",
            "100GB",
            FeatureDataType.STRING,
            true,
            1
        );
        product1.addFeature(feature1);

        ProductEntity product2 = new ProductEntity(
            "PROD-002",
            "Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(product1, product2));

        // When
        List<ProductEntity> productsWithDataLimit = productRepository.findByFeatureKey("data_limit");

        // Then
        assertThat(productsWithDataLimit).hasSize(1);
        assertThat(productsWithDataLimit.get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should count products by status")
    @Disabled("Test scaffolding - implementation required")
    void shouldCountProductsByStatus() {
        // Given
        ProductEntity activeProduct1 = testProduct;
        ProductEntity activeProduct2 = new ProductEntity(
            "PROD-002",
            "Active Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        ProductEntity inactiveProduct = new ProductEntity(
            "PROD-003",
            "Inactive Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.INACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(activeProduct1, activeProduct2, inactiveProduct));

        // When
        long activeCount = productRepository.countByStatus(ProductStatus.ACTIVE);
        long inactiveCount = productRepository.countByStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should find products by multiple product types")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByMultipleProductTypes() {
        // Given
        ProductEntity serviceProduct = testProduct;
        ProductEntity tariffProduct = new ProductEntity(
            "PROD-002",
            "Tariff Product",
            "Description",
            ProductType.TARIFF,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        ProductEntity addonProduct = new ProductEntity(
            "PROD-003",
            "Addon Product",
            "Description",
            ProductType.ADDON,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(serviceProduct, tariffProduct, addonProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> serviceAndTariffProducts = productRepository.findByProductTypeIn(
            List.of(ProductType.SERVICE, ProductType.TARIFF), pageable);

        // Then
        assertThat(serviceAndTariffProducts.getContent()).hasSize(2);
        assertThat(serviceAndTariffProducts.getContent())
            .extracting(ProductEntity::getProductType)
            .containsExactlyInAnyOrder(ProductType.SERVICE, ProductType.TARIFF);
    }

    @Test
    @DisplayName("should find products by billing period")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindProductsByBillingPeriod() {
        // Given
        ProductEntity monthlyProduct = testProduct;
        ProductEntity yearlyProduct = new ProductEntity(
            "PROD-002",
            "Yearly Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "YEARLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(monthlyProduct, yearlyProduct));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> monthlyProducts = productRepository.findByBillingPeriod("MONTHLY", pageable);

        // Then
        assertThat(monthlyProducts.getContent()).hasSize(1);
        assertThat(monthlyProducts.getContent().get(0).getProductCode()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("should return empty page when no products match search criteria")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturnEmptyPageWhenNoProductsMatchSearchCriteria() {
        // Given
        productRepository.save(testProduct);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductEntity> nonExistentProducts = productRepository.findByStatus(ProductStatus.SUSPENDED, pageable);

        // Then
        assertThat(nonExistentProducts.getContent()).isEmpty();
        assertThat(nonExistentProducts.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle pagination with zero offset")
    @Disabled("Test scaffolding - implementation required")
    void shouldHandlePaginationWithZeroOffset() {
        // Given
        ProductEntity product1 = testProduct;
        ProductEntity product2 = new ProductEntity(
            "PROD-002",
            "Product 2",
            "Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.ONE,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );
        productRepository.saveAll(List.of(product1, product2));
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<ProductEntity> firstPage = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);

        // Then
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
    }
}
