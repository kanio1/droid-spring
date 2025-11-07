package com.droid.bss.api.asset;

import com.droid.bss.application.command.asset.*;
import com.droid.bss.application.dto.asset.*;
import com.droid.bss.domain.asset.*;
import com.droid.bss.domain.asset.AssetStatus;
import com.droid.bss.domain.asset.AssetType;
import com.droid.bss.domain.asset.NetworkElementType;
import com.droid.bss.domain.asset.SIMCardStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer test for AssetController
 * Tests all asset management endpoints including assets, network elements, and SIM cards
 */
@WebMvcTest(AssetController.class)
class AssetControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Asset use case mocks
    @MockBean
    private CreateAssetUseCase createAssetUseCase;

    @MockBean
    private AssignAssetUseCase assignAssetUseCase;

    @MockBean
    private ReleaseAssetUseCase releaseAssetUseCase;

    // Network element use case mocks
    @MockBean
    private CreateNetworkElementUseCase createNetworkElementUseCase;

    @MockBean
    private UpdateNetworkElementHeartbeatUseCase updateNetworkElementHeartbeatUseCase;

    // SIM card use case mocks
    @MockBean
    private CreateSIMCardUseCase createSIMCardUseCase;

    @MockBean
    private AssignSIMCardUseCase assignSIMCardUseCase;

    // Repository mocks
    @MockBean
    private AssetRepository assetRepository;

    @MockBean
    private NetworkElementRepository networkElementRepository;

    @MockBean
    private SIMCardRepository simCardRepository;

    private final UUID assetId = UUID.randomUUID();
    private final UUID elementId = UUID.randomUUID();
    private final UUID simId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    // ========== ASSET TESTS ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateAsset() throws Exception {
        // Given
        CreateAssetCommand command = new CreateAssetCommand(
                "ASSET-001",
                "ROUTER",
                "Cisco Router 2960",
                "24-port gigabit switch",
                "SN123456789",
                "C2960X-24T-L",
                "Cisco",
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2027, 1, 15),
                "Warehouse A",
                "CC-001",
                "New equipment"
        );

        AssetResponse response = createAssetResponse();
        when(createAssetUseCase.handle(any(CreateAssetCommand.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId.toString()))
                .andExpect(jsonPath("$.assetTag").value("ASSET-001"))
                .andExpect(jsonPath("$.assetType").value("ROUTER"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAssignAsset() throws Exception {
        // Given
        AssignAssetCommand command = new AssignAssetCommand(
                "CUSTOMER",
                customerId.toString(),
                "John Doe",
                LocalDate.now(),
                "Primary router for customer"
        );

        AssetResponse response = createAssetResponse();
        when(assignAssetUseCase.handle(eq(assetId.toString()), any(AssignAssetCommand.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets/{assetId}/assign", assetId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId.toString()))
                .andExpect(jsonPath("$.assignedToType").value("CUSTOMER"))
                .andExpect(jsonPath("$.assignedToId").value(customerId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReleaseAsset() throws Exception {
        // Given
        AssetResponse response = createAssetResponse();
        when(releaseAssetUseCase.handle(assetId.toString())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets/{assetId}/release", assetId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllAssets() throws Exception {
        // Given
        List<AssetResponse> responses = List.of(createAssetResponse());
        when(assetRepository.findAll()).thenReturn(List.of(createAssetEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()))
                .andExpect(jsonPath("$[0].assetTag").value("ASSET-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAvailableAssets() throws Exception {
        // Given
        List<AssetResponse> responses = List.of(createAssetResponse());
        when(assetRepository.findAvailable()).thenReturn(List.of(createAssetEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAssetsByCustomer() throws Exception {
        // Given
        List<AssetResponse> responses = List.of(createAssetResponse());
        when(assetRepository.findByCustomerId(customerId.toString()))
                .thenReturn(List.of(createAssetEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/by-customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAssetsWithExpiringWarranty() throws Exception {
        // Given
        List<AssetResponse> responses = List.of(createAssetResponse());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);
        when(assetRepository.findWarrantyExpiringBetween(eq(startDate), eq(endDate)))
                .thenReturn(List.of(createAssetEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/warranty-expiring?days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateNetworkElement() throws Exception {
        // Given
        CreateNetworkElementCommand command = new CreateNetworkElementCommand(
                "NODE-001",
                "Router",
                "10.0.0.1",
                "Data Center 1",
                "Production",
                "Cisco",
                "IOS-XE 17.03.08"
        );

        NetworkElementResponse response = createNetworkElementResponse();
        when(createNetworkElementUseCase.handle(any(CreateNetworkElementCommand.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets/elements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(elementId.toString()))
                .andExpect(jsonPath("$.elementTag").value("NODE-001"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldUpdateNetworkElementHeartbeat() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/assets/elements/{elementId}/heartbeat", elementId)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllNetworkElements() throws Exception {
        // Given
        List<NetworkElementResponse> responses = List.of(createNetworkElementResponse());
        when(networkElementRepository.findAll()).thenReturn(List.of(createNetworkElementEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/elements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(elementId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetOnlineNetworkElements() throws Exception {
        // Given
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<NetworkElementResponse> responses = List.of(createNetworkElementResponse());
        when(networkElementRepository.findOnlineElements(eq(threshold)))
                .thenReturn(List.of(createNetworkElementEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/elements/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(elementId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetNetworkElementsInMaintenance() throws Exception {
        // Given
        List<NetworkElementResponse> responses = List.of(createNetworkElementResponse());
        when(networkElementRepository.findInMaintenance())
                .thenReturn(List.of(createNetworkElementEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/elements/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(elementId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateSIMCard() throws Exception {
        // Given
        CreateSIMCardCommand command = new CreateSIMCardCommand(
                "SIM-001",
                "8988211000000000001",
                "ACTIVE",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2034, 1, 1),
                "Data Center 1",
                "Primary SIM pool"
        );

        SIMCardResponse response = createSIMCardResponse();
        when(createSIMCardUseCase.handle(any(CreateSIMCardCommand.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets/sim-cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simId.toString()))
                .andExpect(jsonPath("$.iccid").value("8988211000000000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAssignSIMCard() throws Exception {
        // Given
        SIMCardResponse response = createSIMCardResponse();
        when(assignSIMCardUseCase.handle(
                eq(simId.toString()),
                eq("CUSTOMER"),
                eq(customerId.toString()),
                eq("John Doe")))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/assets/sim-cards/{simId}/assign?assignedToType=CUSTOMER&assignedToId={customerId}&assignedToName=John Doe", simId, customerId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllSIMCards() throws Exception {
        // Given
        List<SIMCardResponse> responses = List.of(createSIMCardResponse());
        when(simCardRepository.findAll()).thenReturn(List.of(createSIMCardEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/sim-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAvailableSIMCards() throws Exception {
        // Given
        List<SIMCardResponse> responses = List.of(createSIMCardResponse());
        when(simCardRepository.findAvailable()).thenReturn(List.of(createSIMCardEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/sim-cards/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetSIMCardsByCustomer() throws Exception {
        // Given
        List<SIMCardResponse> responses = List.of(createSIMCardResponse());
        when(simCardRepository.findByCustomerId(customerId.toString()))
                .thenReturn(List.of(createSIMCardEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/sim-cards/by-customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetExpiredSIMCards() throws Exception {
        // Given
        LocalDate now = LocalDate.now();
        List<SIMCardResponse> responses = List.of(createSIMCardResponse());
        when(simCardRepository.findExpired(eq(now))).thenReturn(List.of(createSIMCardEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/sim-cards/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetExpiringSIMCards() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);
        List<SIMCardResponse> responses = List.of(createSIMCardResponse());
        when(simCardRepository.findExpiringBetween(eq(startDate), eq(endDate)))
                .thenReturn(List.of(createSIMCardEntity()));

        // When & Then
        mockMvc.perform(get("/api/assets/sim-cards/expiring?days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(simId.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyAccessWithoutProperRole() throws Exception {
        // When & Then - USER role should not have access
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidAssetCreation() throws Exception {
        // Given - Missing required field (assetTag)
        CreateAssetCommand command = new CreateAssetCommand(
                null,  // Missing assetTag
                "ROUTER",
                "Cisco Router 2960",
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

        // When & Then
        mockMvc.perform(post("/api/assets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleAssetNotFoundOnAssign() throws Exception {
        // Given
        AssignAssetCommand command = new AssignAssetCommand(
                "CUSTOMER",
                customerId.toString(),
                "John Doe",
                LocalDate.now(),
                null
        );
        when(assignAssetUseCase.handle(eq("999"), any(AssignAssetCommand.class)))
                .thenThrow(new RuntimeException("Asset not found"));

        // When & Then
        mockMvc.perform(post("/api/assets/999/assign", assetId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleNetworkElementNotFoundOnHeartbeat() throws Exception {
        // Given
        String nonExistentId = "999";
        when(updateNetworkElementHeartbeatUseCase.handle(nonExistentId))
                .thenThrow(new RuntimeException("Network element not found"));

        // When & Then
        mockMvc.perform(post("/api/assets/elements/{elementId}/heartbeat", nonExistentId)
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== HELPER METHODS ==========

    private AssetResponse createAssetResponse() {
        return new AssetResponse(
                assetId.toString(),
                "ASSET-001",
                "ROUTER",
                "Cisco Router 2960",
                "24-port gigabit switch",
                "SN123456789",
                "C2960X-24T-L",
                "Cisco",
                "AVAILABLE",
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2027, 1, 15),
                "Warehouse A",
                "CUSTOMER",
                customerId.toString(),
                "John Doe",
                LocalDate.now(),
                "CC-001",
                "Notes"
        );
    }

    private AssetEntity createAssetEntity() {
        AssetEntity asset = new AssetEntity(
                "ASSET-001",
                AssetType.ROUTER,
                "Cisco Router 2960",
                AssetStatus.AVAILABLE
        );
        asset.setId(assetId.toString());
        asset.setSerialNumber("SN123456789");
        asset.setModelNumber("C2960X-24T-L");
        asset.setManufacturer("Cisco");
        asset.setPurchaseDate(LocalDate.of(2024, 1, 15));
        asset.setWarrantyExpiry(LocalDate.of(2027, 1, 15));
        asset.setLocation("Warehouse A");
        asset.setAssignedToType("CUSTOMER");
        asset.setAssignedToId(customerId.toString());
        asset.setAssignedToName("John Doe");
        asset.setAssignedDate(LocalDate.now());
        asset.setCostCenter("CC-001");
        asset.setNotes("Notes");
        return asset;
    }

    private NetworkElementResponse createNetworkElementResponse() {
        return new NetworkElementResponse(
                elementId.toString(),
                "NODE-001",
                "Router",
                "10.0.0.1",
                "Data Center 1",
                "Production",
                "AVAILABLE",
                "Cisco",
                "IOS-XE 17.03.08",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private NetworkElementEntity createNetworkElementEntity() {
        NetworkElementEntity element = new NetworkElementEntity(
                "NODE-001",
                NetworkElementType.ROUTER,
                "Router"
        );
        element.setId(elementId.toString());
        element.setIpAddress("10.0.0.1");
        element.setLocation("Data Center 1");
        element.setStatus(AssetStatus.AVAILABLE);
        element.setSoftwareVersion("IOS-XE 17.03.08");
        element.setLastHeartbeat(LocalDateTime.now());
        element.setCreatedAt(LocalDateTime.now());
        return element;
    }

    private SIMCardResponse createSIMCardResponse() {
        return new SIMCardResponse(
                simId.toString(),
                "SIM-001",
                "8988211000000000001",
                "ACTIVE",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2034, 1, 1),
                "Data Center 1",
                "Primary SIM pool",
                "CUSTOMER",
                customerId.toString(),
                "John Doe",
                LocalDate.now()
        );
    }

    private SIMCardEntity createSIMCardEntity() {
        SIMCardEntity sim = new SIMCardEntity(
                "SIM-001",
                "8988211000000000001",
                SIMCardStatus.ACTIVE
        );
        sim.setId(simId.toString());
        sim.setActivationDate(LocalDate.of(2024, 1, 1));
        sim.setExpiryDate(LocalDate.of(2034, 1, 1));
        sim.setLocation("Data Center 1");
        sim.setNotes("Primary SIM pool");
        sim.setAssignedToType("CUSTOMER");
        sim.setAssignedToId(customerId.toString());
        sim.setAssignedToName("John Doe");
        sim.setAssignedDate(LocalDate.now());
        return sim;
    }
}
