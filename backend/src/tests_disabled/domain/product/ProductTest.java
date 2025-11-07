package com.droid.bss.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product - Aggregate Root Tests")
class ProductTest {

    @Test
    @DisplayName("should create product with valid data")
    void shouldCreateProductWithValidData() {
        // Given
        String productCode = "PROD-001";
        String name = "Test Product";
        String description = "Test Description";
        ProductType productType = ProductType.SERVICE;
        ProductCategory category = ProductCategory.MOBILE;
        BigDecimal price = new BigDecimal("99.99");
        String billingPeriod = "MONTHLY";

        // When
        Product product = Product.create(
            productCode,
            name,
            description,
            productType,
            category,
            price,
            billingPeriod
        );

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getProductCode()).isEqualTo(productCode);
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getProductType()).isEqualTo(productType);
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getPrice()).isEqualByComparingTo(price);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should create product with default currency")
    void shouldCreateProductWithDefaultCurrency() {
        // Given
        String productCode = "PROD-002";
        String name = "Test Product 2";
        ProductType productType = ProductType.SERVICE;
        BigDecimal price = new BigDecimal("50.00");
        String billingPeriod = "MONTHLY";

        // When
        Product product = Product.create(
            productCode,
            name,
            null,
            productType,
            null,
            price,
            billingPeriod
        );

        // Then
        assertThat(product.getCurrency()).isEqualTo("PLN");
    }

    @Test
    @DisplayName("should create product with validity period")
    void shouldCreateProductWithValidityPeriod() {
        // Given
        String productCode = "PROD-003";
        String name = "Test Product 3";
        ProductType productType = ProductType.SERVICE;
        BigDecimal price = new BigDecimal("100.00");
        String billingPeriod = "MONTHLY";
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        // When
        Product product = Product.create(
            productCode,
            name,
            null,
            productType,
            null,
            price,
            "PLN",
            billingPeriod,
            ProductStatus.ACTIVE,
            startDate,
            endDate
        );

        // Then
        assertThat(product.getValidityStart()).isEqualTo(startDate);
        assertThat(product.getValidityEnd()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("should update product information")
    void shouldUpdateProductInformation() {
        // Given
        Product product = Product.create(
            "PROD-004",
            "Old Name",
            "Old Description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        // When
        Product updated = product.updateInfo(
            "New Name",
            "New Description",
            ProductCategory.FIXED
        );

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New Description");
        assertThat(updated.getCategory()).isEqualTo(ProductCategory.FIXED);
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(product.getVersion()).isEqualTo(1); // Original unchanged
    }

    @Test
    @DisplayName("should update product price")
    void shouldUpdateProductPrice() {
        // Given
        Product product = Product.create(
            "PROD-005",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        // When
        Product updated = product.updatePrice(new BigDecimal("150.00"));

        // Then
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change product status")
    void shouldChangeProductStatus() {
        // Given
        Product product = Product.create(
            "PROD-006",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        // When
        Product updated = product.changeStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update validity period")
    void shouldUpdateValidityPeriod() {
        // Given
        Product product = Product.create(
            "PROD-007",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        // When
        LocalDate newStart = LocalDate.now();
        LocalDate newEnd = newStart.plusYears(2);
        Product updated = product.updateValidity(newStart, newEnd);

        // Then
        assertThat(updated.getValidityStart()).isEqualTo(newStart);
        assertThat(updated.getValidityEnd()).isEqualTo(newEnd);
    }

    @Test
    @DisplayName("should check if product is active")
    void shouldCheckIfProductIsActive() {
        // Given
        Product activeProduct = Product.create(
            "PROD-008",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        Product inactiveProduct = activeProduct.changeStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(activeProduct.isActive()).isTrue();
        assertThat(inactiveProduct.isActive()).isFalse();
    }

    @Test
    @DisplayName("should check if product is discontinued")
    void shouldCheckIfProductIsDiscontinued() {
        // Given
        Product product = Product.create(
            "PROD-009",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        Product discontinued = product.changeStatus(ProductStatus.DISCONTINUED);

        // Then
        assertThat(product.isDiscontinued()).isFalse();
        assertThat(discontinued.isDiscontinued()).isTrue();
    }

    @Test
    @DisplayName("should check if product is available")
    void shouldCheckIfProductIsAvailable() {
        // Given
        Product activeProduct = Product.create(
            "PROD-010",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        Product inactiveProduct = activeProduct.changeStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(activeProduct.isAvailable()).isTrue();
        assertThat(inactiveProduct.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("should check if product is within validity period")
    void shouldCheckIfProductIsWithinValidityPeriod() {
        // Given
        LocalDate now = LocalDate.now();
        Product validProduct = Product.create(
            "PROD-011",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            now.minusDays(1),
            now.plusDays(1)
        );

        Product notYetValid = Product.create(
            "PROD-012",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            now.plusDays(1),
            now.plusDays(10)
        );

        Product expired = Product.create(
            "PROD-013",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            now.minusDays(10),
            now.minusDays(1)
        );

        // Then
        assertThat(validProduct.isWithinValidityPeriod()).isTrue();
        assertThat(notYetValid.isWithinValidityPeriod()).isFalse();
        assertThat(expired.isWithinValidityPeriod()).isFalse();
    }

    @Test
    @DisplayName("should check if product can be modified")
    void shouldCheckIfProductCanBeModified() {
        // Given
        Product activeProduct = Product.create(
            "PROD-014",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        );

        Product inactiveProduct = activeProduct.changeStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(activeProduct.canBeModified()).isTrue();
        assertThat(inactiveProduct.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("should throw exception for negative price")
    void shouldThrowExceptionForNegativePrice() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-015",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("-100.00"),
            "MONTHLY"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price cannot be negative");
    }

    @Test
    @DisplayName("should throw exception for invalid validity period")
    void shouldThrowExceptionForInvalidValidityPeriod() {
        // Given
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = LocalDate.now(); // End before start

        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-016",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            start,
            end
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Validity start date cannot be after validity end date");
    }

    @Test
    @DisplayName("should throw exception for null product code")
    void shouldThrowExceptionForNullProductCode() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            null,
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Product code cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null name")
    void shouldThrowExceptionForNullName() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-017",
            null,
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Name cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null product type")
    void shouldThrowExceptionForNullProductType() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-018",
            "Test Product",
            null,
            null,
            null,
            new BigDecimal("100.00"),
            "MONTHLY"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Product type cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null price")
    void shouldThrowExceptionForNullPrice() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-019",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            null,
            "MONTHLY"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Price cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null billing period")
    void shouldThrowExceptionForNullBillingPeriod() {
        // When & Then
        assertThatThrownBy(() -> Product.create(
            "PROD-020",
            "Test Product",
            null,
            ProductType.SERVICE,
            null,
            new BigDecimal("100.00"),
            null
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Billing period cannot be null");
    }
}
