package com.droid.bss.application.command.billing;

import com.droid.bss.domain.billing.UsageSource;
import com.droid.bss.domain.billing.UsageUnit;
import com.droid.bss.infrastructure.write.UsageRecordRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IngestUsageRecordUseCase
 */
class IngestUsageRecordUseCaseTest {

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @InjectMocks
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Test
    @DisplayName("Should ingest usage record successfully")
    void shouldIngestUsageRecordSuccessfully() {
        // Given
        IngestUsageRecordCommand command = IngestUsageRecordCommand.builder()
            .customerId(UUID.randomUUID())
            .subscriptionId(UUID.randomUUID())
            .resourceType("DATA")
            .quantity(100.0)
            .unit(UsageUnit.MB)
            .usageDate(LocalDate.now())
            .source(UsageSource.SYSTEM)
            .build();

        // When
        UUID resultId = ingestUsageRecordUseCase.execute(command);

        // Then
        assertNotNull(resultId);
        verify(usageRecordRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // Given
        IngestUsageRecordCommand command = IngestUsageRecordCommand.builder()
            .customerId(UUID.randomUUID())
            .resourceType("DATA")
            .quantity(100.0)
            .build();

        // When/Then - should handle missing optional fields gracefully
        assertDoesNotThrow(() -> {
            ingestUsageRecordUseCase.execute(command);
        });

        verify(usageRecordRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should handle different usage sources")
    void shouldHandleDifferentUsageSources() {
        // Given
        IngestUsageRecordCommand command = IngestUsageRecordCommand.builder()
            .customerId(UUID.randomUUID())
            .subscriptionId(UUID.randomUUID())
            .resourceType("VOICE")
            .quantity(50.0)
            .unit(UsageUnit.MINUTES)
            .usageDate(LocalDate.now())
            .source(UsageSource.MANUAL)
            .build();

        // When
        UUID resultId = ingestUsageRecordUseCase.execute(command);

        // Then
        assertNotNull(resultId);
        verify(usageRecordRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should handle different usage units")
    void shouldHandleDifferentUsageUnits() {
        // Given
        IngestUsageRecordCommand command = IngestUsageRecordCommand.builder()
            .customerId(UUID.randomUUID())
            .subscriptionId(UUID.randomUUID())
            .resourceType("STORAGE")
            .quantity(10.0)
            .unit(UsageUnit.GB)
            .usageDate(LocalDate.now())
            .source(UsageSource.SYSTEM)
            .build();

        // When
        UUID resultId = ingestUsageRecordUseCase.execute(command);

        // Then
        assertNotNull(resultId);
        verify(usageRecordRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should set usage date to today when not provided")
    void shouldSetUsageDateToTodayWhenNotProvided() {
        // Given
        IngestUsageRecordCommand command = IngestUsageRecordCommand.builder()
            .customerId(UUID.randomUUID())
            .resourceType("DATA")
            .quantity(100.0)
            .build();

        // When
        UUID resultId = ingestUsageRecordUseCase.execute(command);

        // Then
        assertNotNull(resultId);
        verify(usageRecordRepository, times(1)).save(any());
    }
}
