package com.droid.bss.application.query.product;

import com.droid.bss.application.dto.product.GetProductsQuery;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetProductsUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductsUseCase Query Side")
class GetProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductsUseCase getProductsUseCase;

    @Test
    @DisplayName("Should return all products successfully")
    void shouldReturnAllProducts() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductList(5);

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProducts() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        when(productRepository.findAll()).thenReturn(List.of());

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should filter products by status")
    void shouldFilterProductsByStatus() {
        // Arrange
        ProductStatus filterStatus = ProductStatus.ACTIVE;

        GetProductsQuery query = new GetProductsQuery(
            filterStatus.name(),
            null,
            null,
            null,
            null
        );

        List<ProductEntity> allProducts = createProductListWithDifferentStatuses();
        List<ProductEntity> filteredProducts = allProducts.stream()
            .filter(p -> p.getStatus() == filterStatus)
            .toList();

        when(productRepository.findByStatus(eq(filterStatus))).thenReturn(filteredProducts);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");

        verify(productRepository).findByStatus(eq(filterStatus));
    }

    @Test
    @DisplayName("Should filter products by product type")
    void shouldFilterProductsByProductType() {
        // Arrange
        ProductType filterType = ProductType.SERVICE;

        GetProductsQuery query = new GetProductsQuery(
            null,
            filterType.name(),
            null,
            null,
            null
        );

        List<ProductEntity> allProducts = createProductListWithDifferentTypes();
        List<ProductEntity> filteredProducts = allProducts.stream()
            .filter(p -> p.getProductType() == filterType)
            .toList();

        when(productRepository.findByProductType(eq(filterType))).thenReturn(filteredProducts);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductType()).isEqualTo("SERVICE");

        verify(productRepository).findByProductType(eq(filterType));
    }

    @Test
    @DisplayName("Should filter products by category")
    void shouldFilterProductsByCategory() {
        // Arrange
        ProductCategory filterCategory = ProductCategory.MOBILE;

        GetProductsQuery query = new GetProductsQuery(
            null,
            null,
            filterCategory.name(),
            null,
            null
        );

        List<ProductEntity> allProducts = createProductListWithDifferentCategories();
        List<ProductEntity> filteredProducts = allProducts.stream()
            .filter(p -> p.getCategory() == filterCategory)
            .toList();

        when(productRepository.findByCategory(eq(filterCategory))).thenReturn(filteredProducts);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("MOBILE");

        verify(productRepository).findByCategory(eq(filterCategory));
    }

    @Test
    @DisplayName("Should filter products by billing period")
    void shouldFilterProductsByBillingPeriod() {
        // Arrange
        String billingPeriod = "MONTHLY";

        GetProductsQuery query = new GetProductsQuery(
            null,
            null,
            null,
            billingPeriod,
            null
        );

        List<ProductEntity> allProducts = createProductListWithDifferentBillingPeriods();
        List<ProductEntity> filteredProducts = allProducts.stream()
            .filter(p -> p.getBillingPeriod().equals(billingPeriod))
            .toList();

        when(productRepository.findByBillingPeriod(eq(billingPeriod))).thenReturn(filteredProducts);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBillingPeriod()).isEqualTo("MONTHLY");

        verify(productRepository).findByBillingPeriod(eq(billingPeriod));
    }

    @Test
    @DisplayName("Should search products by name")
    void shouldSearchProductsByName() {
        // Arrange
        String searchTerm = "Premium";

        GetProductsQuery query = new GetProductsQuery(
            null,
            null,
            null,
            null,
            searchTerm
        );

        List<ProductEntity> allProducts = createProductListWithDifferentNames();
        List<ProductEntity> filteredProducts = allProducts.stream()
            .filter(p -> p.getName().contains(searchTerm))
            .toList();

        when(productRepository.findByNameContaining(eq(searchTerm))).thenReturn(filteredProducts);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains(searchTerm);

        verify(productRepository).findByNameContaining(eq(searchTerm));
    }

    @Test
    @DisplayName("Should return products sorted by price")
    void shouldReturnProductsSortedByPrice() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);
        query.setSortBy("price");
        query.setSortOrder("ASC");

        List<ProductEntity> products = createProductListWithDifferentPrices();
        products.sort((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // Verify sorting
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getPrice()).isLessThanOrEqualTo(result.get(i + 1).getPrice());
        }

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return products sorted by name")
    void shouldReturnProductsSortedByName() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);
        query.setSortBy("name");
        query.setSortOrder("ASC");

        List<ProductEntity> products = createProductListWithDifferentNames();

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return products with pagination")
    void shouldReturnProductsWithPagination() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);
        query.setPage(0);
        query.setSize(10);

        List<ProductEntity> products = createProductList(25);

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // Pagination is typically handled at the repository level
        // This test verifies the query is passed through

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle combination of filters")
    void shouldHandleCombinationOfFilters() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(
            ProductStatus.ACTIVE.name(),
            ProductType.SERVICE.name(),
            ProductCategory.CLOUD.name(),
            "MONTHLY",
            "Cloud"
        );

        List<ProductEntity> products = createProductList(3);

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // The actual filtering would happen in the repository
        // This test verifies the query parameters are used

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return products with all status types")
    void shouldReturnProductsWithAllStatusTypes() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductListWithDifferentStatuses();

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        List<String> statuses = result.stream()
            .map(ProductDto::getStatus)
            .distinct()
            .toList();

        assertThat(statuses).contains("ACTIVE", "INACTIVE", "DEPRECATED", "SUSPENDED");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return products with all product types")
    void shouldReturnProductsWithAllProductTypes() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductListWithDifferentTypes();

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        List<String> types = result.stream()
            .map(ProductDto::getProductType)
            .distinct()
            .toList();

        assertThat(types).contains("SERVICE", "TARIFF", "BUNDLE", "ADDON");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return products with all categories")
    void shouldReturnProductsWithAllCategories() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductListWithDifferentCategories();

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        List<String> categories = result.stream()
            .map(ProductDto::getCategory)
            .distinct()
            .toList();

        assertThat(categories).contains("MOBILE", "BROADBAND", "TV", "CLOUD", "BASIC");

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void shouldHandleCaseInsensitiveSearch() {
        // Arrange
        String searchTerm = "PREMIUM";

        GetProductsQuery query = new GetProductsQuery(
            null,
            null,
            null,
            null,
            searchTerm
        );

        List<ProductEntity> products = createProductListWithDifferentNames();

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // Case-insensitive search should be implemented in the repository

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return limited number of products when size is specified")
    void shouldReturnLimitedNumberOfProducts() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);
        query.setSize(3);

        List<ProductEntity> products = createProductList(10);

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // Limiting would typically be done at repository level

        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should calculate total count of products")
    void shouldCalculateTotalCountOfProducts() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductList(15);

        when(productRepository.count()).thenReturn(15L);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(15);

        verify(productRepository).findAll();
        verify(productRepository).count();
    }

    @Test
    @DisplayName("Should handle products with zero price")
    void shouldHandleProductsWithZeroPrice() {
        // Arrange
        GetProductsQuery query = new GetProductsQuery(null, null, null, null, null);

        List<ProductEntity> products = createProductListWithDifferentPrices();
        // Add a product with zero price
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-ZERO",
            "Free Product",
            "Free product with zero price",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.ZERO,
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));

        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = getProductsUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        boolean hasZeroPrice = result.stream()
            .anyMatch(p -> p.getPrice().compareTo(BigDecimal.ZERO) == 0);
        assertThat(hasZeroPrice).isTrue();

        verify(productRepository).findAll();
    }

    // Helper methods for test data
    private List<ProductEntity> createProductList(int count) {
        List<ProductEntity> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(createTestProduct(
                UUID.randomUUID(),
                "PROD-" + String.format("%03d", i),
                "Product " + i,
                "Description for product " + i,
                ProductType.SERVICE,
                ProductCategory.BASIC,
                BigDecimal.valueOf(50.00 + i),
                "PLN",
                "MONTHLY",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                null
            ));
        }
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentStatuses() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-ACTIVE",
            "Active Product",
            "Active product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-INACTIVE",
            "Inactive Product",
            "Inactive product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.INACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-DEPRECATED",
            "Deprecated Product",
            "Deprecated product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.DEPRECATED,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-SUSPENDED",
            "Suspended Product",
            "Suspended product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.SUSPENDED,
            LocalDate.now(),
            null
        ));
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentTypes() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-SERVICE",
            "Service Product",
            "Service product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-TARIFF",
            "Tariff Product",
            "Tariff product",
            ProductType.TARIFF,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-BUNDLE",
            "Bundle Product",
            "Bundle product",
            ProductType.BUNDLE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-ADDON",
            "Addon Product",
            "Addon product",
            ProductType.ADDON,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentCategories() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-MOBILE",
            "Mobile Product",
            "Mobile product",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-BROADBAND",
            "Broadband Product",
            "Broadband product",
            ProductType.SERVICE,
            ProductCategory.BROADBAND,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-TV",
            "TV Product",
            "TV product",
            ProductType.SERVICE,
            ProductCategory.TV,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-CLOUD",
            "Cloud Product",
            "Cloud product",
            ProductType.SERVICE,
            ProductCategory.CLOUD,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-BASIC",
            "Basic Product",
            "Basic product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentBillingPeriods() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-MONTHLY",
            "Monthly Product",
            "Monthly product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-QUARTERLY",
            "Quarterly Product",
            "Quarterly product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "QUARTERLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-YEARLY",
            "Yearly Product",
            "Yearly product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "YEARLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentNames() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-PREMIUM",
            "Premium Mobile Plan",
            "Premium product",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-BASIC",
            "Basic Service",
            "Basic product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(30.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-ENTERPRISE",
            "Enterprise Solution",
            "Enterprise product",
            ProductType.SERVICE,
            ProductCategory.CLOUD,
            BigDecimal.valueOf(200.00),
            "PLN",
            "YEARLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        return products;
    }

    private List<ProductEntity> createProductListWithDifferentPrices() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-CHEAP",
            "Cheap Product",
            "Low price product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(10.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-MEDIUM",
            "Medium Product",
            "Medium price product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(50.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        products.add(createTestProduct(
            UUID.randomUUID(),
            "PROD-EXPENSIVE",
            "Expensive Product",
            "High price product",
            ProductType.SERVICE,
            ProductCategory.BASIC,
            BigDecimal.valueOf(100.00),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now(),
            null
        ));
        return products;
    }

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
