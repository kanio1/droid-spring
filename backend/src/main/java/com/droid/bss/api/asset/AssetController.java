package com.droid.bss.api.asset;

import com.droid.bss.application.command.asset.*;
import com.droid.bss.application.dto.asset.*;
import com.droid.bss.domain.asset.*;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.infrastructure.audit.Audited;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for Asset Management
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Asset and inventory management API")
public class AssetController {

    // Asset use cases
    private final CreateAssetUseCase createAssetUseCase;
    private final AssignAssetUseCase assignAssetUseCase;
    private final ReleaseAssetUseCase releaseAssetUseCase;

    // Network element use cases
    private final CreateNetworkElementUseCase createElementUseCase;
    private final UpdateNetworkElementHeartbeatUseCase updateHeartbeatUseCase;

    // SIM card use cases
    private final CreateSIMCardUseCase createSimUseCase;
    private final AssignSIMCardUseCase assignSimUseCase;

    // Repositories
    private final AssetRepository assetRepository;
    private final NetworkElementRepository elementRepository;
    private final SIMCardRepository simRepository;

    public AssetController(
            CreateAssetUseCase createAssetUseCase,
            AssignAssetUseCase assignAssetUseCase,
            ReleaseAssetUseCase releaseAssetUseCase,
            CreateNetworkElementUseCase createElementUseCase,
            UpdateNetworkElementHeartbeatUseCase updateHeartbeatUseCase,
            CreateSIMCardUseCase createSimUseCase,
            AssignSIMCardUseCase assignSimUseCase,
            AssetRepository assetRepository,
            NetworkElementRepository elementRepository,
            SIMCardRepository simRepository) {
        this.createAssetUseCase = createAssetUseCase;
        this.assignAssetUseCase = assignAssetUseCase;
        this.releaseAssetUseCase = releaseAssetUseCase;
        this.createElementUseCase = createElementUseCase;
        this.updateHeartbeatUseCase = updateHeartbeatUseCase;
        this.createSimUseCase = createSimUseCase;
        this.assignSimUseCase = assignSimUseCase;
        this.assetRepository = assetRepository;
        this.elementRepository = elementRepository;
        this.simRepository = simRepository;
    }

    // ========== ASSETS ==========

    @PostMapping
    @Operation(
        summary = "Create asset",
        description = "Create a new asset in inventory"
    )
    @ApiResponse(responseCode = "201", description = "Asset created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.create_asset", description = "Time to create an asset")
    @Audited(action = AuditAction.ASSET_CREATE, entityType = "Asset", description = "Creating new asset")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetCommand command) {
        var response = createAssetUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{assetId}/assign")
    @Operation(
        summary = "Assign asset",
        description = "Assign an asset to a customer or location"
    )
    @ApiResponse(responseCode = "200", description = "Asset assigned successfully")
    @ApiResponse(responseCode = "404", description = "Asset not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.assign_asset", description = "Time to assign an asset")
    @Audited(action = AuditAction.ASSET_UPDATE, entityType = "Asset", description = "Assigning asset {assetId}")
    public ResponseEntity<AssetResponse> assignAsset(
            @Parameter(description = "Asset ID", required = true) @PathVariable String assetId,
            @Valid @RequestBody AssignAssetCommand command) {
        var response = assignAssetUseCase.handle(assetId, command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{assetId}/release")
    @Operation(
        summary = "Release asset",
        description = "Release an asset back to inventory"
    )
    @ApiResponse(responseCode = "200", description = "Asset released successfully")
    @ApiResponse(responseCode = "404", description = "Asset not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.release_asset", description = "Time to release an asset")
    @Audited(action = AuditAction.ASSET_UPDATE, entityType = "Asset", description = "Releasing asset {assetId}")
    public ResponseEntity<AssetResponse> releaseAsset(
            @Parameter(description = "Asset ID", required = true) @PathVariable String assetId) {
        var response = releaseAssetUseCase.handle(assetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all assets",
        description = "Get all assets in inventory"
    )
    @ApiResponse(responseCode = "200", description = "Assets retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.get_assets", description = "Time to get all assets")
    public ResponseEntity<List<AssetResponse>> getAllAssets() {
        var assets = assetRepository.findAll();
        var responses = assets.stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    @Operation(
        summary = "Get available assets",
        description = "Get all available (unassigned) assets"
    )
    @ApiResponse(responseCode = "200", description = "Available assets retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<AssetResponse>> getAvailableAssets() {
        var assets = assetRepository.findAvailable();
        var responses = assets.stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-customer/{customerId}")
    @Operation(
        summary = "Get assets by customer",
        description = "Get all assets assigned to a customer"
    )
    @ApiResponse(responseCode = "200", description = "Customer assets retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<AssetResponse>> getAssetsByCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String customerId) {
        var assets = assetRepository.findByCustomerId(customerId);
        var responses = assets.stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/warranty-expiring")
    @Operation(
        summary = "Get assets with expiring warranty",
        description = "Get assets with warranty expiring in next N days"
    )
    @ApiResponse(responseCode = "200", description = "Expiring warranty assets retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AssetResponse>> getAssetsWithExpiringWarranty(
            @Parameter(description = "Number of days", required = false) @RequestParam(defaultValue = "30") int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        LocalDate startDate = LocalDate.now();
        var assets = assetRepository.findWarrantyExpiringBetween(startDate, endDate);
        var responses = assets.stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ========== NETWORK ELEMENTS ==========

    @PostMapping("/elements")
    @Operation(
        summary = "Create network element",
        description = "Create a new network element"
    )
    @ApiResponse(responseCode = "201", description = "Network element created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.create_element", description = "Time to create a network element")
    @Audited(action = AuditAction.ASSET_CREATE, entityType = "NetworkElement", description = "Creating network element")
    public ResponseEntity<NetworkElementResponse> createNetworkElement(
            @Valid @RequestBody CreateNetworkElementCommand command) {
        var response = createElementUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/elements/{elementId}/heartbeat")
    @Operation(
        summary = "Update network element heartbeat",
        description = "Update heartbeat for a network element"
    )
    @ApiResponse(responseCode = "200", description = "Heartbeat updated successfully")
    @ApiResponse(responseCode = "404", description = "Network element not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('SYSTEM')")
    @Audited(action = AuditAction.ASSET_UPDATE, entityType = "NetworkElement", description = "Updating heartbeat for network element {elementId}")
    public ResponseEntity<Void> updateNetworkElementHeartbeat(
            @Parameter(description = "Element ID", required = true) @PathVariable String elementId) {
        updateHeartbeatUseCase.handle(elementId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/elements")
    @Operation(
        summary = "Get all network elements",
        description = "Get all network elements"
    )
    @ApiResponse(responseCode = "200", description = "Network elements retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<NetworkElementResponse>> getAllNetworkElements() {
        var elements = elementRepository.findAll();
        var responses = elements.stream()
                .map(NetworkElementResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/elements/online")
    @Operation(
        summary = "Get online network elements",
        description = "Get network elements with recent heartbeat"
    )
    @ApiResponse(responseCode = "200", description = "Online network elements retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<NetworkElementResponse>> getOnlineNetworkElements() {
        var threshold = java.time.LocalDateTime.now().minusMinutes(5);
        var elements = elementRepository.findOnlineElements(threshold);
        var responses = elements.stream()
                .map(NetworkElementResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/elements/maintenance")
    @Operation(
        summary = "Get network elements in maintenance",
        description = "Get network elements in maintenance mode"
    )
    @ApiResponse(responseCode = "200", description = "Maintenance elements retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<NetworkElementResponse>> getNetworkElementsInMaintenance() {
        var elements = elementRepository.findInMaintenance();
        var responses = elements.stream()
                .map(NetworkElementResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ========== SIM CARDS ==========

    @PostMapping("/sim-cards")
    @Operation(
        summary = "Create SIM card",
        description = "Create a new SIM card"
    )
    @ApiResponse(responseCode = "201", description = "SIM card created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.create_sim", description = "Time to create a SIM card")
    @Audited(action = AuditAction.ASSET_CREATE, entityType = "SIMCard", description = "Creating SIM card")
    public ResponseEntity<SIMCardResponse> createSIMCard(@Valid @RequestBody CreateSIMCardCommand command) {
        var response = createSimUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sim-cards/{simId}/assign")
    @Operation(
        summary = "Assign SIM card",
        description = "Assign a SIM card to a customer or device"
    )
    @ApiResponse(responseCode = "200", description = "SIM card assigned successfully")
    @ApiResponse(responseCode = "404", description = "SIM card not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.assets.api.assign_sim", description = "Time to assign a SIM card")
    @Audited(action = AuditAction.ASSET_UPDATE, entityType = "SIMCard", description = "Assigning SIM card {simId}")
    public ResponseEntity<SIMCardResponse> assignSIMCard(
            @Parameter(description = "SIM ID", required = true) @PathVariable String simId,
            @RequestParam String assignedToType,
            @RequestParam String assignedToId,
            @RequestParam String assignedToName) {
        var response = assignSimUseCase.handle(simId, assignedToType, assignedToId, assignedToName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sim-cards")
    @Operation(
        summary = "Get all SIM cards",
        description = "Get all SIM cards"
    )
    @ApiResponse(responseCode = "200", description = "SIM cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<SIMCardResponse>> getAllSIMCards() {
        var sims = simRepository.findAll();
        var responses = sims.stream()
                .map(SIMCardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sim-cards/available")
    @Operation(
        summary = "Get available SIM cards",
        description = "Get all available (unassigned) SIM cards"
    )
    @ApiResponse(responseCode = "200", description = "Available SIM cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<SIMCardResponse>> getAvailableSIMCards() {
        var sims = simRepository.findAvailable();
        var responses = sims.stream()
                .map(SIMCardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sim-cards/by-customer/{customerId}")
    @Operation(
        summary = "Get SIM cards by customer",
        description = "Get all SIM cards assigned to a customer"
    )
    @ApiResponse(responseCode = "200", description = "Customer SIM cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<SIMCardResponse>> getSIMCardsByCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String customerId) {
        var sims = simRepository.findByCustomerId(customerId);
        var responses = sims.stream()
                .map(SIMCardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sim-cards/expired")
    @Operation(
        summary = "Get expired SIM cards",
        description = "Get all expired SIM cards"
    )
    @ApiResponse(responseCode = "200", description = "Expired SIM cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<SIMCardResponse>> getExpiredSIMCards() {
        var sims = simRepository.findExpired(LocalDate.now());
        var responses = sims.stream()
                .map(SIMCardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sim-cards/expiring")
    @Operation(
        summary = "Get expiring SIM cards",
        description = "Get SIM cards expiring in next N days"
    )
    @ApiResponse(responseCode = "200", description = "Expiring SIM cards retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<SIMCardResponse>> getExpiringSIMCards(
            @Parameter(description = "Number of days", required = false) @RequestParam(defaultValue = "30") int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        LocalDate startDate = LocalDate.now();
        var sims = simRepository.findExpiringBetween(startDate, endDate);
        var responses = sims.stream()
                .map(SIMCardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
