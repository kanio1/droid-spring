package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.CancelSubscriptionCommand;
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
 * Test for CancelSubscriptionUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelSubscriptionUseCase Application Layer")
class CancelSubscriptionUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerEntityRepository customerEntityRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CancelSubscriptionUseCase cancelSubscriptionUseCase;

    @Test
    @DisplayName("Should cancel active subscription successfully")
    void shouldCancelActiveSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        String reason = "Customer requested cancellation";

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            reason
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

        SubscriptionEntity cancelledSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now(),
            LocalDate.now() // End date set to today when cancelled
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

        // Act
        SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now());

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should cancel suspended subscription successfully")
    void shouldCancelSuspendedSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        String reason = "Account closure requested";

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            reason
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now().minusMonths(2),
            LocalDate.now().plusMonths(1)
        );

        SubscriptionEntity cancelledSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now().minusMonths(2),
            LocalDate.now()
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

        // Act
        SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should cancel subscription without reason")
    void shouldCancelSubscriptionWithoutReason() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            null // No reason provided
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

        SubscriptionEntity cancelledSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now(),
            LocalDate.now()
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

        // Act
        SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should set end date to current date when cancelling")
    void shouldSetEndDateToCurrentDateWhenCancelling() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        LocalDate originalEndDate = LocalDate.now().plusMonths(3);

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Customer requested early termination"
        );

        CustomerEntity customer = createTestCustomer(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        ProductEntity product = createTestProduct(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));

        SubscriptionEntity subscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusMonths(2),
            originalEndDate
        );

        SubscriptionEntity cancelledSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now().minusMonths(2),
            LocalDate.now()
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

        // Act
        SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(result.getEndDate()).isNotEqualTo(originalEndDate);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription not found")
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Arrange
        UUID subscriptionId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Test cancellation"
        );

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cancelSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription not found: " + subscriptionId);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel expired subscription")
    void shouldThrowExceptionWhenTryingToCancelExpiredSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Test cancellation"
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
        assertThatThrownBy(() -> cancelSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot cancel subscription with status: " + SubscriptionStatus.EXPIRED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel already cancelled subscription")
    void shouldThrowExceptionWhenTryingToCancelAlreadyCancelledSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Test cancellation"
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
        assertThatThrownBy(() -> cancelSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot cancel subscription with status: " + SubscriptionStatus.CANCELLED);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when subscription is pending")
    void shouldThrowExceptionWhenSubscriptionIsPending() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Test cancellation"
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

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> cancelSubscriptionUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot cancel subscription with status: " + SubscriptionStatus.PENDING);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should preserve subscription data when cancelling")
    void shouldPreserveSubscriptionDataWhenCancelling() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        String subscriptionNumber = "SUB-20241105-ABC12345";

        CancelSubscriptionCommand command = new CancelSubscriptionCommand(
            subscriptionId.toString(),
            "Test cancellation"
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
        subscription.setSubscriptionNumber(subscriptionNumber);

        SubscriptionEntity cancelledSubscription = createTestSubscription(
            subscriptionId,
            customer,
            product,
            null,
            SubscriptionStatus.CANCELLED,
            LocalDate.now().minusMonths(1),
            LocalDate.now()
        );
        cancelledSubscription.setSubscriptionNumber(subscriptionNumber);

        when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

        // Act
        SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubscriptionNumber()).isEqualTo(subscriptionNumber);
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getProduct()).isEqualTo(product);

        verify(subscriptionRepository).findById(eq(subscriptionId));
        verify(subscriptionRepository).save(any(SubscriptionEntity.class));
    }

    @Test
    @DisplayName("Should handle cancellation with different reason types")
    void shouldHandleCancellationWithDifferentReasonTypes() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();

        String[] reasons = {
            "Customer requested cancellation",
            "Service no longer needed",
            "Relocated to different region",
            "Switching to competitor",
            null // No reason
        };

        for (String reason : reasons) {
            CancelSubscriptionCommand command = new CancelSubscriptionCommand(
                subscriptionId.toString(),
                reason
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

            SubscriptionEntity cancelledSubscription = createTestSubscription(
                subscriptionId,
                customer,
                product,
                null,
                SubscriptionStatus.CANCELLED,
                LocalDate.now(),
                LocalDate.now()
            );

            when(subscriptionRepository.findById(eq(subscriptionId))).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(cancelledSubscription);

            // Act
            SubscriptionEntity result = cancelSubscriptionUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

            reset(subscriptionRepository);
        }
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
