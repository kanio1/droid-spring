package com.droid.bss.api.service;

import com.droid.bss.application.command.service.CreateServiceActivationUseCase;
import com.droid.bss.application.command.service.DeactivateServiceUseCase;
import com.droid.bss.application.command.service.ServiceActivationService;
import com.droid.bss.domain.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for ServiceController
 */
@WebMvcTest(ServiceController.class)
class ServiceControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceActivationService service;

    @MockBean
    private CreateServiceActivationUseCase createActivationUseCase;

    @MockBean
    private DeactivateServiceUseCase deactivateServiceUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllServices_shouldReturnServices() throws Exception {
        // Arrange
        ServiceEntity service1 = createService("INTERNET-100M", "Internet 100 Mbps", ServiceType.INTERNET);
        ServiceEntity service2 = createService("TV-BASIC", "TV Basic Package", ServiceType.TELEVISION);
        when(service.getAllServices()).thenReturn(List.of(service1, service2));

        // Act & Assert
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceCode").value("INTERNET-100M"))
                .andExpect(jsonPath("$[1].serviceCode").value("TV-BASIC"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createServiceActivation_shouldCreateActivation() throws Exception {
        // Arrange
        CreateServiceActivationCommand command = new CreateServiceActivationCommand(
                "customer-123",
                "INTERNET-100M",
                "CORR-001",
                null,
                "Test activation"
        );

        ServiceActivationResponse response = new ServiceActivationResponse(
                "activation-123",
                "customer-123",
                "INTERNET-100M",
                "Internet 100 Mbps",
                "PENDING",
                null,
                null,
                null,
                "Test activation",
                null,
                "CORR-001",
                0,
                3,
                List.of()
        );

        when(createActivationUseCase.handle(any(CreateServiceActivationCommand.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/services/activations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("activation-123"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkActivationEligibility_whenEligible_shouldReturnTrue() throws Exception {
        // Arrange
        ActivationEligibility eligibility = new ActivationEligibility(true, null);
        when(service.checkActivationEligibility("customer-123", "INTERNET-100M"))
                .thenReturn(eligibility);

        // Act & Assert
        mockMvc.perform(post("/api/services/check-eligibility")
                        .param("customerId", "customer-123")
                        .param("serviceCode", "INTERNET-100M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reason").isEmpty());
    }

    private ServiceEntity createService(String code, String name, ServiceType type) {
        ServiceEntity service = new ServiceEntity(code, name, null, type, ServiceStatus.ACTIVE, null);
        return service;
    }
}
