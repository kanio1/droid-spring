package com.droid.bss.api.billing;

import com.droid.bss.AbstractIntegrationTest;
import com.droid.bss.application.dto.billing.IngestUsageRecordCommand;
import com.droid.bss.application.dto.billing.StartBillingCycleCommand;
import com.droid.bss.domain.billing.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer test for BillingController
 */
@WebMvcTest(BillingController.class)
class BillingControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestUsageRecordUseCase ingestUseCase;

    @MockBean
    private StartBillingCycleUseCase startCycleUseCase;

    @MockBean
    private ProcessBillingCycleUseCase processCycleUseCase;

    @MockBean
    private UsageRecordRepository usageRecordRepository;

    @MockBean
    private BillingCycleRepository billingCycleRepository;

    private final UUID customerId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID cycleId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldIngestUsageRecord() throws Exception {
        // Given
        IngestUsageRecordCommand command = new IngestUsageRecordCommand(
                subscriptionId.toString(),
                "VOICE",
                "MINUTES",
                BigDecimal.TEN,
                LocalDate.now(),
                LocalTime.of(10, 0),
                "NATIONAL",
                "+48123456789",
                "POL",
                "NETWORK-1",
                "PEAK",
                "SYSTEM",
                "cdr.txt"
        );

        UsageRecordResponse response = new UsageRecordResponse(
                UUID.randomUUID().toString(),
                subscriptionId.toString(),
                "VOICE",
                "MINUTES",
                BigDecimal.TEN,
                LocalDate.now(),
                LocalTime.of(10, 0),
                "NATIONAL",
                "+48123456789",
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(6.15),
                "PLN",
                true,
                LocalDate.now()
        );

        when(ingestUseCase.handle(any(IngestUsageRecordCommand.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/billing/usage-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.isRated").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldGetUnratedUsageRecords() throws Exception {
        // Given
        UsageRecordEntity usage1 = createUsageRecord("VOICE", BigDecimal.TEN);
        UsageRecordEntity usage2 = createUsageRecord("SMS", BigDecimal.ONE);

        when(usageRecordRepository.findUnrated()).thenReturn(List.of(usage1, usage2));

        // When & Then
        mockMvc.perform(get("/api/billing/usage-records"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldStartBillingCycle() throws Exception {
        // Given
        StartBillingCycleCommand command = new StartBillingCycleCommand(
                customerId.toString(),
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                "MONTHLY"
        );

        BillingCycleEntity cycle = new BillingCycleEntity();
        cycle.setId(cycleId.toString());
        cycle.setCustomerId(customerId.toString());
        cycle.setCycleStart(command.cycleStart());
        cycle.setCycleEnd(command.cycleEnd());
        cycle.setBillingDate(command.billingDate());
        cycle.setBillingCycleType(BillingCycleType.MONTHLY);
        cycle.setStatus(BillingCycleStatus.PENDING);

        when(startCycleUseCase.handle(any(StartBillingCycleCommand.class))).thenReturn(cycle);

        // When & Then
        mockMvc.perform(post("/api/billing/cycles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldProcessBillingCycle() throws Exception {
        // Given
        BillingCycleEntity cycle = new BillingCycleEntity();
        cycle.setId(cycleId.toString());
        cycle.setCustomerId(customerId.toString());
        cycle.setCycleStart(LocalDate.now().minusDays(10));
        cycle.setCycleEnd(LocalDate.now().minusDays(1));
        cycle.setBillingDate(LocalDate.now());
        cycle.setBillingCycleType(BillingCycleType.MONTHLY);
        cycle.setStatus(BillingCycleStatus.PROCESSED);
        cycle.setTotalAmount(BigDecimal.valueOf(100.0));
        cycle.setTaxAmount(BigDecimal.valueOf(23.0));
        cycle.setTotalWithTax(BigDecimal.valueOf(123.0));
        cycle.setInvoiceCount(1);
        cycle.setProcessedAt(java.time.LocalDateTime.now());

        when(processCycleUseCase.handle(cycleId.toString())).thenReturn(cycle);

        // When & Then
        mockMvc.perform(post("/api/billing/cycles/{cycleId}/process", cycleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cycleId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.totalAmount").value(100.0))
                .andExpect(jsonPath("$.invoiceCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetBillingCycles() throws Exception {
        // Given
        BillingCycleEntity cycle1 = new BillingCycleEntity();
        cycle1.setId(UUID.randomUUID().toString());
        cycle1.setStatus(BillingCycleStatus.PENDING);

        BillingCycleEntity cycle2 = new BillingCycleEntity();
        cycle2.setId(UUID.randomUUID().toString());
        cycle2.setStatus(BillingCycleStatus.PROCESSED);

        when(billingCycleRepository.findAll()).thenReturn(List.of(cycle1, cycle2));

        // When & Then
        mockMvc.perform(get("/api/billing/cycles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetPendingBillingCycles() throws Exception {
        // Given
        BillingCycleEntity pendingCycle = new BillingCycleEntity();
        pendingCycle.setId(UUID.randomUUID().toString());
        pendingCycle.setStatus(BillingCycleStatus.PENDING);
        pendingCycle.setBillingDate(LocalDate.now());

        when(billingCycleRepository.findPendingForProcessing(LocalDate.now()))
                .thenReturn(List.of(pendingCycle));

        // When & Then
        mockMvc.perform(get("/api/billing/cycles/pending"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void shouldRejectUnauthorizedAccess() throws Exception {
        // When & Then - should redirect to login or return 401
        mockMvc.perform(get("/api/billing/cycles"))
                .andExpect(status().isUnauthorized());
    }

    private UsageRecordEntity createUsageRecord(String usageType, BigDecimal amount) {
        UsageRecordEntity entity = new UsageRecordEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsageType(UsageType.valueOf(usageType));
        entity.setUsageUnit(UsageUnit.MINUTES);
        entity.setUsageAmount(amount);
        entity.setUsageDate(LocalDate.now());
        entity.setUsageTime(LocalTime.now());
        entity.setDestinationType(DestinationType.NATIONAL);
        entity.setDestinationNumber("+48123456789");
        entity.setRatePeriod(RatePeriod.PEAK);
        entity.setSource(UsageSource.SYSTEM);
        entity.setSourceFile("test.txt");
        return entity;
    }
}
