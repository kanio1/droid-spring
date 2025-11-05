package com.droid.bss.application.query.product;

import com.droid.bss.application.dto.product.GetProductByIdQuery;
import com.droid.bss.application.dto.product.ProductDto;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.product.ProductStatus;
import com.droid.bss.domain.product.ProductType;
import com.droid.bss.domain.product.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetProductByIdUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductByIdUseCase Query Side")
class GetProductByIdUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductByIdUseCase getProductByIdUseCase;

    @Test
    @DisplayName("Should return product by ID successfully")
    void shouldReturnProductById() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-001",
            "Premium Mobile Plan",
            "Premium mobile plan with unlimited data",
            ProductType.TARIFF,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(49.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId.toString());
        assertThat(result.getProductCode()).isEqualTo("PROD-001");
        assertThat(result.getName()).isEqualTo("Premium Mobile Plan");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getPrice()).isEqualByComparingTo("49.99");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with ACTIVE status")
    void shouldReturnProductWithActiveStatus() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-002",
            "Cloud Storage Service",
            "100GB cloud storage",
            ProductType.SERVICE,
            ProductCategory.CLOUD,
            BigDecimal.valueOf(19.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getProductType()).isEqualTo("SERVICE");
        assertThat(result.getCategory()).isEqualTo("CLOUD");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with INACTIVE status")
    void shouldReturnProductWithInactiveStatus() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-003",
            "Old Product",
            "Product no longer available",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(29.99),
            "PLN",
            "MONTHLY",
            ProductStatus.INACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("INACTIVE");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with DEPRECATED status")
    void shouldReturnProductWithDeprecatedStatus() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-004",
            "Deprecated Product",
            "Product being phased out",
            ProductType.ADDON,
            ProductCategory.TV,
            BigDecimal.valueOf(15.99),
            "PLN",
            "MONTHLY",
            ProductStatus.DEPRECATED,
            LocalDate.now().minusMonths(6),
            LocalDate.now().plusMonths(6)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DEPRECATED");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with SUSPENDED status")
    void shouldReturnProductWithSuspendedStatus() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-005",
            "Suspended Product",
            "Product temporarily suspended",
            ProductType.BUNDLE,
            ProductCategory.BROADBAND,
            BigDecimal.valueOf(99.99),
            "PLN",
            "MONTHLY",
            ProductStatus.SUSPENDED,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUSPENDED");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with all product types")
    void shouldReturnProductWithAllProductTypes() {
        // Arrange
        ProductType[] productTypes = {
            ProductType.SERVICE,
            ProductType.TARIFF,
            ProductType.BUNDLE,
            ProductType.ADDON
        };

        for (ProductType productType : productTypes) {
            UUID productId = UUID.randomUUID();

            GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

            ProductEntity product = createTestProduct(
                productId,
                "PROD-TEST-" + productType.ordinal(),
                "Test Product",
                "Test description",
                productType,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                "PLN",
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

            // Act
            ProductDto result = getProductByIdUseCase.handle(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProductType()).isEqualTo(productType.name());

            verify(productRepository).findById(eq(productId));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should return product with all categories")
    void shouldReturnProductWithAllCategories() {
        // Arrange
        ProductCategory[] categories = {
            ProductCategory.MOBILE,
            ProductCategory.BROADBAND,
            ProductCategory.TV,
            ProductCategory.CLOUD,
            ProductCategory.BASIC
        };

        for (ProductCategory category : categories) {
            UUID productId = UUID.randomUUID();

            GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

            ProductEntity product = createTestProduct(
                productId,
                "PROD-CAT-" + category.ordinal(),
                "Test Product",
                "Test description",
                ProductType.SERVICE,
                category,
                BigDecimal.valueOf(50.00),
                "PLN",
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

            // Act
            ProductDto result = getProductByIdUseCase.handle(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCategory()).isEqualTo(category.name());

            verify(productRepository).findById(eq(productId));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should return product with billing periods")
    void shouldReturnProductWithBillingPeriods() {
        // Arrange
        String[] billingPeriods = {"MONTHLY", "QUARTERLY", "YEARLY"};

        for (String billingPeriod : billingPeriods) {
            UUID productId = UUID.randomUUID();

            GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

            ProductEntity product = createTestProduct(
                productId,
                "PROD-BILL-" + billingPeriod,
                "Test Product",
                "Test description",
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                "PLN",
                billingPeriod,
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

            // Act
            ProductDto result = getProductByIdUseCase.handle(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBillingPeriod()).isEqualTo(billingPeriod);

            verify(productRepository).findById(eq(productId));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should return product with different currencies")
    void shouldReturnProductWithDifferentCurrencies() {
        // Arrange
        String[] currencies = {"PLN", "USD", "EUR"};

        for (String currency : currencies) {
            UUID productId = UUID.randomUUID();

            GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

            ProductEntity product = createTestProduct(
                productId,
                "PROD-CUR-" + currency,
                "Test Product",
                "Test description",
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                currency,
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

            // Act
            ProductDto result = getProductByIdUseCase.handle(query);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCurrency()).isEqualTo(currency);

            verify(productRepository).findById(eq(productId));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should return product with zero price")
    void shouldReturnProductWithZeroPrice() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-006",
            "Free Service",
            "Free tier service",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.ZERO,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("0.00");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with large price")
    void shouldReturnProductWithLargePrice() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-007",
            "Enterprise Plan",
            "Enterprise plan with premium features",
            ProductType.SERVICE,
            ProductCategory.CLOUD,
            BigDecimal.valueOf(999999.99),
            "PLN",
            "YEARLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("999999.99");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with validity dates")
    void shouldReturnProductWithValidityDates() {
        // Arrange
        UUID productId = UUID.randomUUID();
        LocalDate validityStart = LocalDate.now().minusMonths(1);
        LocalDate validityEnd = LocalDate.now().plusYears(1);

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-008",
            "Limited Offer",
            "Limited time offer",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(39.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            validityStart,
            validityEnd
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidityStart()).isEqualTo(validityStart);
        assertThat(result.getValidityEnd()).isEqualTo(validityEnd);

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product without validity end date")
    void shouldReturnProductWithoutValidityEndDate() {
        // Arrange
        UUID productId = UUID.randomUUID();
        LocalDate validityStart = LocalDate.now();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-009",
            "Ongoing Service",
            "Ongoing service",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(29.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            validityStart,
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidityStart()).isEqualTo(validityStart);
        assertThat(result.getValidityEnd()).isNull();

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with detailed description")
    void shouldReturnProductWithDetailedDescription() {
        // Arrange
        UUID productId = UUID.randomUUID();
        String detailedDescription = "This is a comprehensive product description with " +
            "multiple lines of text that provides detailed information about the product " +
            "features, benefits, and usage scenarios for potential customers.";

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-010",
            "Premium Product",
            detailedDescription,
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(79.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(detailedDescription);

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Arrange
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        when(productRepository.findById(eq(productId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getProductByIdUseCase.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product not found: " + productId);

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should check if product is active based on status and validity dates")
    void shouldCheckIfProductIsActive() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-011",
            "Active Product",
            "Currently active product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(30)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return product with isActive flag")
    void shouldReturnProductWithIsActiveFlag() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-012",
            "Product",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusYears(1)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();

        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return complete product information")
    void shouldReturnCompleteProductInformation() {
        // Arrange
        UUID productId = UUID.randomUUID();

        GetProductByIdQuery query = new GetProductByIdQuery(productId.toString());

        ProductEntity product = createTestProduct(
            productId,
            "PROD-013",
            "Complete Product",
            "Complete product information",
            ProductType.TARIFF,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(59.99),
            "USD",
            "QUARTERLY",
            ProductStatus.ACTIVE,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusYears(1)
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        ProductDto result = getProductByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId.toString());
        assertThat(result.getProductCode()).isEqualTo("PROD-013");
        assertThat(result.getName()).isEqualTo("Complete Product");
        assertThat(result.getDescription()).isEqualTo("Complete product information");
        assertThat(result.getProductType()).isEqualTo("TARIFF");
        assertThat(result.getCategory()).isEqualTo("MOBILE");
        assertThat(result.getPrice()).isEqualByComparingTo("59.99");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getBillingPeriod()).isEqualTo("QUARTERLY");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getValidityStart()).isNotNull();
        assertThat(result.getValidityEnd()).isNotNull();

        verify(productRepository).findById(eq(productId));
    }

    // Helper methods for test data
    private ProductEntity createTestProduct(
        UUID id,
        String productCode,
        String name,
        String description,
        ProductType productType,
        ProductCategory category,
        BigDecimal price,
        String currency,
        String billingPeriod,
        ProductStatus status,
        LocalDate validityStart,
        LocalDate validityEnd
    ) {
        ProductEntity product = new ProductEntity(
            productCode,
            name,
            description,
            productType,
            category,
            price,
            currency,
            billingPeriod,
            status,
            validityStart,
            validityEnd
        );
        product.setId(id);
        return product;
    }
}
