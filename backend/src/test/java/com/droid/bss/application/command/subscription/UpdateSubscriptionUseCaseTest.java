package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.UpdateSubscriptionCommand;
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
 * Test for UpdateSubscriptionUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSubscriptionUseCase Application Layer")
class UpdateSubscriptionUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerEntityRepository customerEntityRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private UpdateSubscriptionUseCase updateSubscriptionUseCase;

    @Test
    @DisplayName("Should update subscription price successfully")
    void shouldUpdateSubscriptionPrice() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        BigDecimal newPrice = BigDecimal.valueOf(149.99);

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null, // no product change
            newPrice,
            null, // no currency change
            null, // no billing period change
            null  // no auto renew change
        );

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
        subscription.setPrice(BigDecimal.valueOf(99.99));

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        updatedSubscription.setPrice(newPrice);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("149.99");
        assertThat(result.getNetAmount()).isEqualByComparingTo("149.99"); // No discount

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should update subscription product successfully")
    void shouldUpdateSubscriptionProduct() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID newProductId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            newProductId.toString(),
            null, // no price change
            null, // no currency change
            null, // no billing period change
            null  // no auto renew change
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity oldProduct = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        ProductEntity newProduct = createTestProduct(newProductId);
        newProduct.setName("Enterprise Service");
        newProduct.setPrice(BigDecimal.valueOf(199.99));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            oldProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            newProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(productRepository.findById(eq(newProductId))).thenReturn(Optional.of(newProduct));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProduct().getId()).isEqualTo(newProductId);
        assertThat(result.getProduct().getName()).isEqualTo("Enterprise Service");

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(productRepository).findById(eq(newProductId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should update billing period successfully")
    void shouldUpdateBillingPeriod() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            null,
            null,
            "YEARLY", // Change to yearly billing
            null
        );

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

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        updatedSubscription.setBillingPeriod("YEARLY");

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBillingPeriod()).isEqualTo("YEARLY");

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should update auto renew flag successfully")
    void shouldUpdateAutoRenewFlag() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            null,
            null,
            null,
            false // Disable auto renew
        );

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
        subscription.setAutoRenew(true);

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        updatedSubscription.setAutoRenew(false);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAutoRenew()).isFalse();

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should update multiple fields at once")
    void shouldUpdateMultipleFieldsAtOnce() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID newProductId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            newProductId.toString(),
            BigDecimal.valueOf(179.99),
            "USD",
            "QUARTERLY",
            false
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity oldProduct = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        ProductEntity newProduct = createTestProduct(newProductId);
        newProduct.setCurrency("USD");

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            oldProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        subscription.setBillingPeriod("MONTHLY");
        subscription.setAutoRenew(true);

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            newProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        updatedSubscription.setBillingPeriod("QUARTERLY");
        updatedSubscription.setAutoRenew(false);
        updatedSubscription.setCurrency("USD");

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(productRepository.findById(eq(newProductId))).thenReturn(Optional.of(newProduct));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProduct().getId()).isEqualTo(newProductId);
        assertThat(result.getPrice()).isEqualByComparingTo("179.99");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getBillingPeriod()).isEqualTo("QUARTERLY");
        assertThat(result.getAutoRenew()).isFalse();

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(productRepository).findById(eq(newProductId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should recalculate net amount when price changes")
    void shouldRecalculateNetAmountWhenPriceChanges() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        BigDecimal newPrice = BigDecimal.valueOf(200.00);
        BigDecimal discountAmount = BigDecimal.valueOf(20.00);

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            newPrice,
            null,
            null,
            null
        );

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
        subscription.setPrice(BigDecimal.valueOf(100.00));
        subscription.setDiscountAmount(discountAmount);
        subscription.calculateNetAmount();

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusMonths(2)
        );
        updatedSubscription.setPrice(newPrice);
        updatedSubscription.setDiscountAmount(discountAmount);
        updatedSubscription.calculateNetAmount();

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("200.00");
        assertThat(result.getNetAmount()).isEqualByComparingTo("180.00"); // 200 - 20

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Arrange
        UUID subscriptionId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            BigDecimal.valueOf(150.00),
            null,
            null,
            null
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription not found: " + subscriptionId);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
        verify(productRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw exception when new product not found")
    void shouldThrowExceptionWhenNewProductNotFound() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID newProductId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            newProductId.toString(),
            null,
            null,
            null,
            null
        );

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

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(productRepository.findById(eq(newProductId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product not found: " + newProductId);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(productRepository).findById(eq(newProductId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to update expired subscription")
    void shouldThrowExceptionWhenTryingToUpdateExpiredSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            BigDecimal.valueOf(150.00),
            null,
            null,
            null
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.EXPIRED,
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusMonths(3)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> updateSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot update subscription with status: " + SubscriptionStatus.EXPIRED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to update cancelled subscription")
    void shouldThrowExceptionWhenTryingToUpdateCancelledSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            BigDecimal.valueOf(150.00),
            null,
            null,
            null
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now().minusMonths(2),
            LocalDate.now().minusDays(1)
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> updateSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot update subscription with status: " + SubscriptionStatus.CANCELLED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should allow update when subscription is pending")
    void shouldAllowUpdateWhenSubscriptionIsPending() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
            subscriptionId.toString(),
            null,
            BigDecimal.valueOf(150.00),
            null,
            null,
            null
        );

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

        SubscriptionEntity updatedSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.PENDING,
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        );
        updatedSubscription.setPrice(BigDecimal.valueOf(150.00));

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(updatedSubscription);

        // Act
        SubscriptionEntity result = updateSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPrice()).isEqualByComparingTo("150.00");

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
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
