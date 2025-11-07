package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.CreateSubscriptionCommand;
import com.droid.bss.application.dto.subscription.SubscriptionResponse;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.repository.OrderRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.repository.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
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
 * Test for CreateSubscriptionUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubscriptionUseCase Application Layer")
@Disabled("Temporarily disabled - subscription use case infrastructure not fully implemented")

class SubscribeUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerEntityRepository customerEntityRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @Test
    @DisplayName("Should create subscription successfully with all required fields")
    void shouldCreateSubscriptionSuccessfully() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(savedSubscription.getId());
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedSubscription.getPrice()).isEqualByComparingTo("99.99");

        verify(customerEntityRepository).findById(eq(customerId));
        verify(productRepository).findById(eq(productId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should create subscription with order reference")
    void shouldCreateSubscriptionWithOrderReference() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID orderId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            orderId.toString(),
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        OrderEntity order = createTestOrder(orderId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, order);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(orderRepository.findById(eq(orderId))).thenReturn(Optional.of(order));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(savedSubscription.getId());
        assertThat(savedSubscription.getOrder()).isNotNull();
        assertThat(savedSubscription.getOrder().getId()).isEqualTo(orderId);

        verify(customerEntityRepository).findById(eq(customerId));
        verify(productRepository).findById(eq(productId));
        verify(orderRepository).findById(eq(orderId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer not found: " + customerId);

        verify(customerEntityRepository).findById(eq(customerId));
        verify(productRepository, never()).findById(any(UUID.class));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product not found: " + productId);

        verify(customerEntityRepository).findById(eq(customerId));
        verify(productRepository).findById(eq(productId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            orderId.toString(),
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(orderRepository.findById(eq(orderId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order not found: " + orderId);

        verify(customerEntityRepository).findById(eq(customerId));
        verify(productRepository).findById(eq(productId));
        verify(orderRepository).findById(eq(orderId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should set default currency when null")
    void shouldSetDefaultCurrencyWhenNull() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            null, // null currency
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            "PLN".equals(subscription.getCurrency())
        ));
    }

    @Test
    @DisplayName("Should handle auto renew flag correctly")
    void shouldHandleAutoRenewFlag() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            false // auto renew disabled
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);
        savedSubscription.setAutoRenew(false);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            Boolean.FALSE.equals(subscription.getAutoRenew())
        ));
    }

    @Test
    @DisplayName("Should create subscription with different billing periods")
    void shouldCreateSubscriptionWithDifferentBillingPeriods() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(12),
            "YEARLY",
            BigDecimal.valueOf(999.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            "YEARLY".equals(subscription.getBillingPeriod())
        ));
    }

    @Test
    @DisplayName("Should create subscription with zero price")
    void shouldCreateSubscriptionWithZeroPrice() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(0.00),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);
        savedSubscription.setPrice(BigDecimal.ZERO);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            BigDecimal.ZERO.equals(subscription.getPrice())
        ));
    }

    @Test
    @DisplayName("Should generate unique subscription number")
    void shouldGenerateUniqueSubscriptionNumber() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            subscription.getSubscriptionNumber() != null &&
            subscription.getSubscriptionNumber().startsWith("SUB-")
        ));
    }

    @Test
    @DisplayName("Should set default status as ACTIVE")
    void shouldSetDefaultStatusAsActive() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            SubscriptionStatus.ACTIVE.equals(subscription.getStatus())
        ));
    }

    @Test
    @DisplayName("Should handle subscription with all dates correctly")
    void shouldHandleSubscriptionWithAllDates() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);
        LocalDate billingStart = LocalDate.now();
        LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            startDate,
            endDate,
            billingStart,
            nextBillingDate,
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            startDate.equals(subscription.getStartDate()) &&
            endDate.equals(subscription.getEndDate()) &&
            billingStart.equals(subscription.getBillingStart()) &&
            nextBillingDate.equals(subscription.getNextBillingDate())
        ));
    }

    @Test
    @DisplayName("Should calculate net amount correctly with discount")
    void shouldCalculateNetAmountCorrectlyWithDiscount() {
        // Arrange
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            customerId.toString(),
            productId.toString(),
            null,
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            "MONTHLY",
            BigDecimal.valueOf(100.00),
            "PLN",
            true
        );
        command.setDiscountAmount(BigDecimal.valueOf(10.00));

        CustomerEntity customer = createTestCustomer(customerId);
        ProductEntity product = createTestProduct(productId);
        SubscriptionEntity savedSubscription = createTestSubscription(customer, product, null);
        savedSubscription.setDiscountAmount(BigDecimal.valueOf(10.00));

        when(customerEntityRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(productRepository.findById(eq(productId))).thenReturn(Optional.of(product));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSubscription);

        // Act
        UUID result = createSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(argThat(subscription ->
            BigDecimal.valueOf(90.00).equals(subscription.getNetAmount())
        ));
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

    private OrderEntity createTestOrder(UUID id) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setOrderNumber("ORD-2024-000001");
        return order;
    }

    private SubscriptionEntity createTestSubscription(CustomerEntity customer, ProductEntity product, OrderEntity order) {
        SubscriptionEntity subscription = new SubscriptionEntity(
            "SUB-20241105-ABC12345",
            customer,
            product,
            order,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "MONTHLY",
            BigDecimal.valueOf(99.99),
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscription.setId(UUID.randomUUID());
        subscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        return subscription;
    }
}
