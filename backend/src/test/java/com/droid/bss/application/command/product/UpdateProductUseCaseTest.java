package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.UpdateProductCommand;
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
 * Test for UpdateProductUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProductUseCase Application Layer")
class UpdateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private UpdateProductUseCase updateProductUseCase;

    @Test
    @DisplayName("Should update product name successfully")
    void shouldUpdateProductName() {
        // Arrange
        UUID productId = UUID.randomUUID();
        String newName = "Updated Product Name";

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            newName,
            null, // no description change
            null, // no type change
            null, // no category change
            null, // no price change
            null, // no currency change
            null, // no billing period change
            null, // no status change
            null, // no validity start change
            null  // no validity end change
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-001",
            "Old Product Name",
            "Old description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-001",
            newName,
            "Old description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(newName);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product description successfully")
    void shouldUpdateProductDescription() {
        // Arrange
        UUID productId = UUID.randomUUID();
        String newDescription = "Updated product description";

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            newDescription,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-002",
            "Product Name",
            "Old description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-002",
            "Product Name",
            newDescription,
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(newDescription);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product price successfully")
    void shouldUpdateProductPrice() {
        // Arrange
        UUID productId = UUID.randomUUID();
        BigDecimal newPrice = BigDecimal.valueOf(99.99);

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            newPrice,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-003",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-003",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            newPrice,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("99.99");

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product type successfully")
    void shouldUpdateProductType() {
        // Arrange
        UUID productId = UUID.randomUUID();
        ProductType newType = ProductType.TARIFF;

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            newType,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-004",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-004",
            "Product Name",
            "Description",
            newType,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductType()).isEqualTo(newType);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product category successfully")
    void shouldUpdateProductCategory() {
        // Arrange
        UUID productId = UUID.randomUUID();
        ProductCategory newCategory = ProductCategory.MOBILE;

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            newCategory,
            null,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-005",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-005",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            newCategory,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isEqualTo(newCategory);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product currency successfully")
    void shouldUpdateProductCurrency() {
        // Arrange
        UUID productId = UUID.randomUUID();
        String newCurrency = "USD";

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            null,
            newCurrency,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-006",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-006",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            newCurrency,
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCurrency()).isEqualTo(newCurrency);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update billing period successfully")
    void shouldUpdateBillingPeriod() {
        // Arrange
        UUID productId = UUID.randomUUID();
        String newBillingPeriod = "YEARLY";

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            null,
            null,
            newBillingPeriod,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-007",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-007",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            newBillingPeriod,
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBillingPeriod()).isEqualTo(newBillingPeriod);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product status successfully")
    void shouldUpdateProductStatus() {
        // Arrange
        UUID productId = UUID.randomUUID();
        ProductStatus newStatus = ProductStatus.INACTIVE;

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            newStatus,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-008",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-008",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            newStatus,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(newStatus);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update validity dates successfully")
    void shouldUpdateValidityDates() {
        // Arrange
        UUID productId = UUID.randomUUID();
        LocalDate newValidityStart = LocalDate.now().plusDays(30);
        LocalDate newValidityEnd = LocalDate.now().plusYears(1);

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            newValidityStart,
            newValidityEnd
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-009",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-009",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            newValidityStart,
            newValidityEnd
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidityStart()).isEqualTo(newValidityStart);
        assertThat(result.getValidityEnd()).isEqualTo(newValidityEnd);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update multiple fields at once")
    void shouldUpdateMultipleFieldsAtOnce() {
        // Arrange
        UUID productId = UUID.randomUUID();

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            "Updated Product",
            "Updated description",
            ProductType.TARIFF,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(79.99),
            "USD",
            "QUARTERLY",
            ProductStatus.ACTIVE,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusYears(1)
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-010",
            "Old Product",
            "Old description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.INACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-010",
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

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getProductType()).isEqualTo(ProductType.TARIFF);
        assertThat(result.getCategory()).isEqualTo(ProductCategory.MOBILE);
        assertThat(result.getPrice()).isEqualByComparingTo("79.99");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getBillingPeriod()).isEqualTo("QUARTERLY");
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product to zero price")
    void shouldUpdateProductToZeroPrice() {
        // Arrange
        UUID productId = UUID.randomUUID();

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-011",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-011",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.ZERO,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("0.00");

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should update product with large price")
    void shouldUpdateProductWithLargePrice() {
        // Arrange
        UUID productId = UUID.randomUUID();
        BigDecimal newPrice = BigDecimal.valueOf(999999.99);

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            null,
            null,
            null,
            null,
            newPrice,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity existingProduct = createTestProduct(
            productId,
            "PROD-012",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        ProductEntity updatedProduct = createTestProduct(
            productId,
            "PROD-012",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            newPrice,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(updatedProduct);

        // Act
        ProductEntity result = updateProductUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("999999.99");

        verify(productRepository).findById(eq(productId));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Arrange
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            "Updated Product",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(productRepository.findById(eq(productId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateProductUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product not found: " + productId);

        verify(productRepository).findById(eq(productId));
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to update deleted product")
    void shouldThrowExceptionWhenTryingToUpdateDeletedProduct() {
        // Arrange
        UUID productId = UUID.randomUUID();

        UpdateProductCommand command = new UpdateProductCommand(
            productId.toString(),
            "Updated Product",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        ProductEntity deletedProduct = createTestProduct(
            productId,
            "PROD-013",
            "Product Name",
            "Description",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        );
        deletedProduct.setDeletedAt(LocalDate.now());

        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(deletedProduct));

        // Act & Assert
        assertThatThrownBy(() -> updateProductUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot update deleted product");

        verify(productRepository).findById(eq(productId));
        verify(productRepository, never()).save(any(ProductEntity.class));
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
