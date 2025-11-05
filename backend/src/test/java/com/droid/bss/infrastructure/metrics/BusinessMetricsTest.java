package com.droid.bss.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for BusinessMetrics
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessMetrics Infrastructure Layer")
class BusinessMetricsTest {

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private BusinessMetrics businessMetrics;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    @Mock
    private Timer.Sample timerSample;

    @Test
    @DisplayName("Should increment customer created counter")
    void shouldIncrementCustomerCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementCustomerCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment customer updated counter")
    void shouldIncrementCustomerUpdatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementCustomerUpdated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment customer status changed counter")
    void shouldIncrementCustomerStatusChangedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementCustomerStatusChanged();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment invoice created counter")
    void shouldIncrementInvoiceCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementInvoiceCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment invoice paid counter")
    void shouldIncrementInvoicePaidCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementInvoicePaid();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should start and stop invoice processing timer")
    void shouldStartAndStopInvoiceProcessingTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startInvoiceProcessing();
        businessMetrics.recordInvoiceProcessing(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should increment order created counter")
    void shouldIncrementOrderCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementOrderCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment payment created counter")
    void shouldIncrementPaymentCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementPaymentCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment payment completed counter")
    void shouldIncrementPaymentCompletedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementPaymentCompleted();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should start and stop payment processing timer")
    void shouldStartAndStopPaymentProcessingTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startPaymentProcessing();
        businessMetrics.recordPaymentProcessing(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should increment subscription created counter")
    void shouldIncrementSubscriptionCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementSubscriptionCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment subscription renewed counter")
    void shouldIncrementSubscriptionRenewedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementSubscriptionRenewed();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should start and stop customer query timer")
    void shouldStartAndStopCustomerQueryTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startCustomerQuery();
        businessMetrics.recordCustomerQuery(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should start and stop service activation timer")
    void shouldStartAndStopServiceActivationTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startServiceActivation();
        businessMetrics.recordServiceActivation(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should start and stop service deactivation timer")
    void shouldStartAndStopServiceDeactivationTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startServiceDeactivation();
        businessMetrics.recordServiceDeactivation(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should increment usage record ingested counter")
    void shouldIncrementUsageRecordIngestedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementUsageRecordIngested();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment usage record rated counter")
    void shouldIncrementUsageRecordRatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementUsageRecordRated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment billing cycle started counter")
    void shouldIncrementBillingCycleStartedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementBillingCycleStarted();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment billing cycle processed counter")
    void shouldIncrementBillingCycleProcessedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementBillingCycleProcessed();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment rating rule matched counter")
    void shouldIncrementRatingRuleMatchedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementRatingRuleMatched();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should start and stop usage rating timer")
    void shouldStartAndStopUsageRatingTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startUsageRating();
        businessMetrics.recordUsageRating(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should start and stop billing cycle processing timer")
    void shouldStartAndStopBillingCycleProcessingTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startBillingCycleProcessing();
        businessMetrics.recordBillingCycleProcessing(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should increment asset created counter")
    void shouldIncrementAssetCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementAssetCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment asset assigned counter")
    void shouldIncrementAssetAssignedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementAssetAssigned();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment asset released counter")
    void shouldIncrementAssetReleasedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementAssetReleased();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment network element created counter")
    void shouldIncrementNetworkElementCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementNetworkElementCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment network element heartbeat counter")
    void shouldIncrementNetworkElementHeartbeatCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementNetworkElementHeartbeat();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment SIM card created counter")
    void shouldIncrementSIMCardCreatedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementSIMCardCreated();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should increment SIM card assigned counter")
    void shouldIncrementSIMCardAssignedCounter() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(mock(AtomicLong.class));

        // Act
        businessMetrics.incrementSIMCardAssigned();

        // Assert
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should start and stop asset operation timer")
    void shouldStartAndStopAssetOperationTimer() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample = businessMetrics.startAssetOperation();
        businessMetrics.recordAssetOperation(sample);

        // Assert
        verify(timer).start();
        verify(timer).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should set active subscriptions gauge")
    void shouldSetActiveSubscriptionsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setActiveSubscriptions(100);

        // Assert
        verify(gauge).set(100);
    }

    @Test
    @DisplayName("Should set pending invoices gauge")
    void shouldSetPendingInvoicesGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setPendingInvoices(50);

        // Assert
        verify(gauge).set(50);
    }

    @Test
    @DisplayName("Should set total customers gauge")
    void shouldSetTotalCustomersGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setTotalCustomers(1000);

        // Assert
        verify(gauge).set(1000);
    }

    @Test
    @DisplayName("Should set active services gauge")
    void shouldSetActiveServicesGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setActiveServices(500);

        // Assert
        verify(gauge).set(500);
    }

    @Test
    @DisplayName("Should set pending activations gauge")
    void shouldSetPendingActivationsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setPendingActivations(25);

        // Assert
        verify(gauge).set(25);
    }

    @Test
    @DisplayName("Should set unrated usage gauge")
    void shouldSetUnratedUsageGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setUnratedUsage(10000);

        // Assert
        verify(gauge).set(10000);
    }

    @Test
    @DisplayName("Should set pending billing cycles gauge")
    void shouldSetPendingBillingCyclesGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setPendingBillingCycles(10);

        // Assert
        verify(gauge).set(10);
    }

    @Test
    @DisplayName("Should set total assets gauge")
    void shouldSetTotalAssetsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setTotalAssets(200);

        // Assert
        verify(gauge).set(200);
    }

    @Test
    @DisplayName("Should set available assets gauge")
    void shouldSetAvailableAssetsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setAvailableAssets(150);

        // Assert
        verify(gauge).set(150);
    }

    @Test
    @DisplayName("Should set assets in use gauge")
    void shouldSetAssetsInUseGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setAssetsInUse(50);

        // Assert
        verify(gauge).set(50);
    }

    @Test
    @DisplayName("Should set total SIM cards gauge")
    void shouldSetTotalSIMCardsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setTotalSIMCards(1000);

        // Assert
        verify(gauge).set(1000);
    }

    @Test
    @DisplayName("Should set available SIM cards gauge")
    void shouldSetAvailableSIMCardsGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setAvailableSIMCards(800);

        // Assert
        verify(gauge).set(800);
    }

    @Test
    @DisplayName("Should set network elements online gauge")
    void shouldSetNetworkElementsOnlineGauge() {
        // Arrange
        AtomicLong gauge = mock(AtomicLong.class);
        when(meterRegistry.gauge(anyString(), any(AtomicLong.class))).thenReturn(gauge);

        // Act
        businessMetrics.setNetworkElementsOnline(50);

        // Assert
        verify(gauge).set(50);
    }

    @Test
    @DisplayName("Should handle multiple counter increments")
    void shouldHandleMultipleCounterIncrements() {
        // Arrange
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Act
        businessMetrics.incrementCustomerCreated();
        businessMetrics.incrementCustomerUpdated();
        businessMetrics.incrementCustomerStatusChanged();
        businessMetrics.incrementInvoiceCreated();
        businessMetrics.incrementOrderCreated();

        // Assert
        verify(counter, times(5)).increment();
    }

    @Test
    @DisplayName("Should handle multiple timer operations")
    void shouldHandleMultipleTimerOperations() {
        // Arrange
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        when(timer.start()).thenReturn(timerSample);

        // Act
        Timer.Sample sample1 = businessMetrics.startInvoiceProcessing();
        businessMetrics.recordInvoiceProcessing(sample1);

        Timer.Sample sample2 = businessMetrics.startPaymentProcessing();
        businessMetrics.recordPaymentProcessing(sample2);

        Timer.Sample sample3 = businessMetrics.startCustomerQuery();
        businessMetrics.recordCustomerQuery(sample3);

        // Assert
        verify(timer, times(3)).start();
        verify(timer, times(3)).record(any(Runnable.class));
    }

    @Test
    @DisplayName("Should handle gauge updates")
    void shouldHandleGaugeUpdates() {
        // Arrange
        AtomicLong gauge1 = mock(AtomicLong.class);
        AtomicLong gauge2 = mock(AtomicLong.class);
        AtomicLong gauge3 = mock(AtomicLong.class);

        when(meterRegistry.gauge(anyString(), any(AtomicLong.class)))
            .thenReturn(gauge1)
            .thenReturn(gauge2)
            .thenReturn(gauge3);

        // Act
        businessMetrics.setActiveSubscriptions(100);
        businessMetrics.setPendingInvoices(50);
        businessMetrics.setTotalCustomers(1000);

        // Assert
        verify(gauge1).set(100);
        verify(gauge2).set(50);
        verify(gauge3).set(1000);
    }
}
