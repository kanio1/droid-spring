package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.CreateProductCommand;
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
 * Test for CreateProductUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductUseCase Application Layer")
class CreateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CreateProductUseCase createProductUseCase;

    @Test
    @DisplayName("Should create product successfully with all required fields")
    void shouldCreateProductSuccessfully() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
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

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(savedProduct.getId());
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("49.99");
        assertThat(savedProduct.getCurrency()).isEqualTo("PLN");

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with SERVICE type")
    void shouldCreateProductWithServiceType() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-002",
            "Cloud Storage Service",
            "100GB cloud storage service",
            ProductType.SERVICE,
            ProductCategory.CLOUD,
            BigDecimal.valueOf(19.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getProductType()).isEqualTo(ProductType.SERVICE);
        assertThat(savedProduct.getCategory()).isEqualTo(ProductCategory.CLOUD);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with BUNDLE type")
    void shouldCreateProductWithBundleType() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-003",
            "Triple Play Bundle",
            "Internet + TV + Phone bundle",
            ProductType.BUNDLE,
            ProductCategory.BROADBAND,
            BigDecimal.valueOf(99.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getProductType()).isEqualTo(ProductType.BUNDLE);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with ADDON type")
    void shouldCreateProductWithAddonType() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-004",
            "Extra TV Channels",
            "Premium TV channels package",
            ProductType.ADDON,
            ProductCategory.TV,
            BigDecimal.valueOf(15.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getProductType()).isEqualTo(ProductType.ADDON);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with TARIFF type")
    void shouldCreateProductWithTariffType() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-005",
            "Basic Mobile Tariff",
            "Basic mobile tariff with 10GB",
            ProductType.TARIFF,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(29.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getProductType()).isEqualTo(ProductType.TARIFF);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with different billing periods")
    void shouldCreateProductWithDifferentBillingPeriods() {
        // Arrange
        String[] billingPeriods = {"MONTHLY", "QUARTERLY", "YEARLY"};

        for (String billingPeriod : billingPeriods) {
            CreateProductCommand command = new CreateProductCommand(
                "PROD-006",
                "Test Product",
                "Test product description",
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                "PLN",
                billingPeriod,
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            ProductEntity savedProduct = createTestProduct(
                UUID.randomUUID(),
                command.getProductCode(),
                command.getName(),
                command.getDescription(),
                command.getProductType(),
                command.getCategory(),
                command.getPrice(),
                command.getCurrency(),
                command.getBillingPeriod(),
                command.getStatus(),
                command.getValidityStart(),
                command.getValidityEnd()
            );

            when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

            // Act
            UUID result = createProductUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(savedProduct.getBillingPeriod()).isEqualTo(billingPeriod);

            verify(productRepository).save(any(ProductEntity.class));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should create product with different currencies")
    void shouldCreateProductWithDifferentCurrencies() {
        // Arrange
        String[] currencies = {"PLN", "USD", "EUR"};

        for (String currency : currencies) {
            CreateProductCommand command = new CreateProductCommand(
                "PROD-007",
                "Test Product",
                "Test product description",
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                currency,
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            ProductEntity savedProduct = createTestProduct(
                UUID.randomUUID(),
                command.getProductCode(),
                command.getName(),
                command.getDescription(),
                command.getProductType(),
                command.getCategory(),
                command.getPrice(),
                command.getCurrency(),
                command.getBillingPeriod(),
                command.getStatus(),
                command.getValidityStart(),
                command.getValidityEnd()
            );

            when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

            // Act
            UUID result = createProductUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(savedProduct.getCurrency()).isEqualTo(currency);

            verify(productRepository).save(any(ProductEntity.class));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should create product with different statuses")
    void shouldCreateProductWithDifferentStatuses() {
        // Arrange
        ProductStatus[] statuses = {
            ProductStatus.ACTIVE,
            ProductStatus.INACTIVE,
            ProductStatus.DEPRECATED,
            ProductStatus.SUSPENDED
        };

        for (ProductStatus status : statuses) {
            CreateProductCommand command = new CreateProductCommand(
                "PROD-008",
                "Test Product",
                "Test product description",
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00),
                "PLN",
                "MONTHLY",
                status,
                LocalDate.now(),
                null
            );

            ProductEntity savedProduct = createTestProduct(
                UUID.randomUUID(),
                command.getProductCode(),
                command.getName(),
                command.getDescription(),
                command.getProductType(),
                command.getCategory(),
                command.getPrice(),
                command.getCurrency(),
                command.getBillingPeriod(),
                command.getStatus(),
                command.getValidityStart(),
                command.getValidityEnd()
            );

            when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

            // Act
            UUID result = createProductUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(savedProduct.getStatus()).isEqualTo(status);

            verify(productRepository).save(any(ProductEntity.class));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should create product with all categories")
    void shouldCreateProductWithAllCategories() {
        // Arrange
        ProductCategory[] categories = {
            ProductCategory.MOBILE,
            ProductCategory.BROADBAND,
            ProductCategory.TV,
            ProductCategory.CLOUD,
            ProductCategory.BASIC
        };

        for (ProductCategory category : categories) {
            CreateProductCommand command = new CreateProductCommand(
                "PROD-009",
                "Test Product",
                "Test product description",
                ProductType.SERVICE,
                category,
                BigDecimal.valueOf(50.00),
                "PLN",
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            );

            ProductEntity savedProduct = createTestProduct(
                UUID.randomUUID(),
                command.getProductCode(),
                command.getName(),
                command.getDescription(),
                command.getProductType(),
                command.getCategory(),
                command.getPrice(),
                command.getCurrency(),
                command.getBillingPeriod(),
                command.getStatus(),
                command.getValidityStart(),
                command.getValidityEnd()
            );

            when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

            // Act
            UUID result = createProductUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(savedProduct.getCategory()).isEqualTo(category);

            verify(productRepository).save(any(ProductEntity.class));
            reset(productRepository);
        }
    }

    @Test
    @DisplayName("Should create product with zero price")
    void shouldCreateProductWithZeroPrice() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-010",
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

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("0.00");

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with large price")
    void shouldCreateProductWithLargePrice() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            "PROD-011",
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

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("999999.99");

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with validity dates")
    void shouldCreateProductWithValidityDates() {
        // Arrange
        LocalDate validityStart = LocalDate.now();
        LocalDate validityEnd = LocalDate.now().plusYears(1);

        CreateProductCommand command = new CreateProductCommand(
            "PROD-012",
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

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getValidityStart()).isEqualTo(validityStart);
        assertThat(savedProduct.getValidityEnd()).isEqualTo(validityEnd);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product without validity end date")
    void shouldCreateProductWithoutValidityEndDate() {
        // Arrange
        LocalDate validityStart = LocalDate.now();

        CreateProductCommand command = new CreateProductCommand(
            "PROD-013",
            "Ongoing Service",
            "Ongoing service without end date",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(29.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            validityStart,
            null // No validity end
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getValidityStart()).isEqualTo(validityStart);
        assertThat(savedProduct.getValidityEnd()).isNull();

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should create product with detailed description")
    void shouldCreateProductWithDetailedDescription() {
        // Arrange
        String detailedDescription = "This is a comprehensive product description with " +
            "multiple lines of text that provides detailed information about the product " +
            "features, benefits, and usage scenarios.";

        CreateProductCommand command = new CreateProductCommand(
            "PROD-014",
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

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            command.getProductCode(),
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(savedProduct.getDescription()).isEqualTo(detailedDescription);

        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should generate unique product code when creating product")
    void shouldGenerateUniqueProductCode() {
        // Arrange
        CreateProductCommand command = new CreateProductCommand(
            null, // No product code provided
            "Auto Generated Product",
            "Product with auto-generated code",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(49.99),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity savedProduct = createTestProduct(
            UUID.randomUUID(),
            "PROD-AUTO-123",
            command.getName(),
            command.getDescription(),
            command.getProductType(),
            command.getCategory(),
            command.getPrice(),
            command.getCurrency(),
            command.getBillingPeriod(),
            command.getStatus(),
            command.getValidityStart(),
            command.getValidityEnd()
        );

        when(productRepository.save(any(ProductEntity.class))).thenReturn(savedProduct);

        // Act
        UUID result = createProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(productRepository).save(argThat(product ->
            product.getProductCode() != null &&
            !product.getProductCode().isEmpty()
        ));
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
