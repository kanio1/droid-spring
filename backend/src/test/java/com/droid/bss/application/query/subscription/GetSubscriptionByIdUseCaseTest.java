package com.droid.bss.application.query.subscription;

import com.droid.bss.application.dto.subscription.GetSubscriptionByIdQuery;
import com.droid.bss.application.dto.subscription.SubscriptionDto;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.repository.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import com.droid.bss.domain.subscription.SubscriptionStatus;
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
 * Test for GetSubscriptionByIdUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetSubscriptionByIdUseCase Query Side")
class GetSubscriptionByIdUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerEntityRepository customerEntityRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetSubscriptionByIdUseCase getSubscriptionByIdUseCase;

    @Test
    @DisplayName("Should return subscription by ID successfully")
    void shouldReturnSubscriptionById() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(subscriptionId.toString());
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCustomerEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getProductName()).isEqualTo("Premium Service");
        assertThat(result.getPrice()).isEqualByComparingTo("99.99");

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(customerEntityRepository).findById(eq(customer.getId()));
        verify(productRepository).findById(eq(product.getId()));
    }

    @Test
    @DisplayName("Should return subscription with ACTIVE status")
    void shouldReturnSubscriptionWithActiveStatus() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(2),
            LocalDate.now().plusMonths(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with SUSPENDED status")
    void shouldReturnSubscriptionWithSuspendedStatus() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now().minusMonths(3),
            LocalDate.now().plusMonths(9)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUSPENDED");

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with CANCELLED status")
    void shouldReturnSubscriptionWithCancelledStatus() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now().minusMonths(4),
            LocalDate.now().minusDays(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getEndDate()).isNotNull();

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with EXPIRED status")
    void shouldReturnSubscriptionWithExpiredStatus() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.EXPIRED,
            LocalDate.now().minusYears(2),
            LocalDate.now().minusYears(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("EXPIRED");

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with PENDING status")
    void shouldReturnSubscriptionWithPendingStatus() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.PENDING,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with customer details")
    void shouldReturnSubscriptionWithCustomerDetails() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(customerId);
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane.smith@example.com");
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomerEmail()).isEqualTo("jane.smith@example.com");
        assertThat(result.getCustomerId()).isEqualTo(customerId.toString());

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(customerEntityRepository).findById(eq(customerId));
    }

    @Test
    @DisplayName("Should return subscription with product details")
    void shouldReturnSubscriptionWithProductDetails() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(productId);
        product.setName("Enterprise Service");
        product.setDescription("Enterprise service package with premium features");

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("Enterprise Service");
        assertThat(result.getProductId()).isEqualTo(productId.toString());

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(productRepository).findById(eq(productId));
    }

    @Test
    @DisplayName("Should return subscription with all billing information")
    void shouldReturnSubscriptionWithAllBillingInformation() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        subscription.setBillingPeriod("QUARTERLY");
        subscription.setCurrency("USD");
        subscription.setPrice(BigDecimal.valueOf(299.99));
        subscription.setDiscountAmount(BigDecimal.valueOf(30.00));
        subscription.calculateNetAmount();

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBillingPeriod()).isEqualTo("QUARTERLY");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getPrice()).isEqualByComparingTo("299.99");
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getNetAmount()).isEqualByComparingTo("269.99");
        assertThat(result.getNextBillingDate()).isNotNull();

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with auto renew information")
    void shouldReturnSubscriptionWithAutoRenewInformation() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );
        subscription.setAutoRenew(false);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAutoRenew()).isFalse();

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with subscription number")
    void shouldReturnSubscriptionWithSubscriptionNumber() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );
        subscription.setSubscriptionNumber("SUB-20241105-XYZ98765");

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubscriptionNumber()).isEqualTo("SUB-20241105-XYZ98765");

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Arrange
        UUID subscriptionId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getSubscriptionByIdUseCase.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription not found: " + subscriptionId);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(customerEntityRepository, never()).findById(any(UUID.class));
        verify(productRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should return subscription with correct dates")
    void shouldReturnSubscriptionWithCorrectDates() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusMonths(2);
        LocalDate endDate = LocalDate.now().plusMonths(10);
        LocalDate billingStart = LocalDate.now().minusMonths(2);
        LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            startDate,
            endDate
        );
        subscription.setBillingStart(billingStart);
        subscription.setNextBillingDate(nextBillingDate);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        assertThat(result.getBillingStart()).isEqualTo(billingStart);
        assertThat(result.getNextBillingDate()).isEqualTo(nextBillingDate);

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    @Test
    @DisplayName("Should return subscription with zero price")
    void shouldReturnSubscriptionWithZeroPrice() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        GetSubscriptionByIdQuery query = new GetSubscriptionByIdQuery(subscriptionId.toString());

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );
        subscription.setPrice(BigDecimal.ZERO);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(customerEntityRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(product.getId()))).thenReturn(Optional.of(product));

        // Act
        SubscriptionDto result = getSubscriptionByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("0.00");

        verify(subscriptionRepository).findById(eq(subscriptionId));
    }

    // Helper methods for test data
    private CustomerEntity createTestCustomer(UUID id) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        return customer;
    }

    private ProductEntity createTestProduct(UUID id) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName("Premium Service");
        product.setDescription("Premium service package");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setCurrency("PLN");
        return product;
    }

    private SubscriptionEntity createTestSubscription(
        UUID id,
        CustomerEntity customer,
        ProductEntity product,
        com.droid.bss.domain.order.OrderEntity order,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        SubscriptionEntity subscription = new SubscriptionEntity(
            "SUB-20241105-ABC12345",
            customer,
            product,
            order,
            status,
            startDate,
            LocalDate.now(),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscription.setId(id);
        subscription.setEndDate(endDate);
        subscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        return subscription;
    }
}
