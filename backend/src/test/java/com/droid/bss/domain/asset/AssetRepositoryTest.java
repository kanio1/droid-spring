package com.droid.bss.domain.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository layer test for Asset-related entities
 * Tests AssetRepository, NetworkElementRepository, and SIMCardRepository
 */
@DataJpaTest
@ActiveProfiles("test")
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private NetworkElementRepository networkElementRepository;

    @Autowired
    private SIMCardRepository simCardRepository;

    // Test data IDs
    private final String customerId1 = "customer-001";
    private final String customerId2 = "customer-002";

    // Asset entities
    private AssetEntity availableRouter;
    private AssetEntity assignedSwitch;
    private AssetEntity maintenanceModem;
    private AssetEntity damagedSetTopBox;

    // Network element entities
    private NetworkElementEntity activeRouter;
    private NetworkElementEntity offlineSwitch;
    private NetworkElementEntity maintenanceElement;

    // SIM card entities
    private SIMCardEntity availableSim;
    private SIMCardEntity assignedSim;
    private SIMCardEntity expiredSim;
    private SIMCardEntity suspendedSim;

    @BeforeEach
    void setUp() {
        // Clear all repositories
        assetRepository.deleteAll();
        networkElementRepository.deleteAll();
        simCardRepository.deleteAll();

        // Create asset entities
        availableRouter = new AssetEntity(
                "ASSET-001",
                AssetType.ROUTER,
                "Cisco Router 2960",
                AssetStatus.AVAILABLE
        );
        availableRouter.setSerialNumber("SN123456789");
        availableRouter.setWarrantyExpiry(LocalDate.now().plusDays(365));
        availableRouter.setLocation("Warehouse A");

        assignedSwitch = new AssetEntity(
                "ASSET-002",
                AssetType.SWITCH,
                "Cisco Switch 3850",
                AssetStatus.IN_USE
        );
        assignedSwitch.setSerialNumber("SN987654321");
        assignedSwitch.setAssignedToType("CUSTOMER");
        assignedSwitch.setAssignedToId(customerId1);
        assignedSwitch.setAssignedToName("John Doe");
        assignedSwitch.setAssignedDate(LocalDate.now().minusDays(30));
        assignedSwitch.setLocation("Customer Site");

        maintenanceModem = new AssetEntity(
                "ASSET-003",
                AssetType.MODEM,
                "Arris Modem SB6183",
                AssetStatus.MAINTENANCE
        );
        maintenanceModem.setSerialNumber("SN555666777");
        maintenanceModem.setLocation("Repair Center");

        damagedSetTopBox = new AssetEntity(
                "ASSET-004",
                AssetType.SET_TOP_BOX,
                "Samsung STB",
                AssetStatus.DAMAGED
        );
        damagedSetTopBox.setSerialNumber("SN111222333");

        assetRepository.saveAll(List.of(availableRouter, assignedSwitch, maintenanceModem, damagedSetTopBox));

        // Create network element entities
        activeRouter = new NetworkElementEntity(
                "NE-001",
                NetworkElementType.ROUTER,
                "Core Router"
        );
        activeRouter.setIpAddress("10.0.0.1");
        activeRouter.setStatus(AssetStatus.AVAILABLE);
        activeRouter.setLastHeartbeat(LocalDateTime.now().minusMinutes(1));
        activeRouter.setLocation("Data Center 1");

        offlineSwitch = new NetworkElementEntity(
                "NE-002",
                NetworkElementType.SWITCH,
                "Access Switch"
        );
        offlineSwitch.setIpAddress("10.0.1.1");
        offlineSwitch.setStatus(AssetStatus.IN_USE);
        offlineSwitch.setLastHeartbeat(LocalDateTime.now().minusHours(2)); // Stale heartbeat
        offlineSwitch.setLocation("Branch Office");

        maintenanceElement = new NetworkElementEntity(
                "NE-003",
                NetworkElementType.ACCESS_POINT,
                "WiFi AP"
        );
        maintenanceElement.setIpAddress("10.0.2.1");
        maintenanceElement.setStatus(AssetStatus.MAINTENANCE);
        maintenanceElement.setMaintenanceMode(true);
        maintenanceElement.setLastHeartbeat(LocalDateTime.now().minusDays(1));
        maintenanceElement.setLocation("Office Building");

        networkElementRepository.saveAll(List.of(activeRouter, offlineSwitch, maintenanceElement));

        // Create SIM card entities
        availableSim = new SIMCardEntity(
                "SIM-001",
                "8988211000000000001",
                SIMCardStatus.AVAILABLE
        );
        availableSim.setImsi("310260000000000");
        availableSim.setMsisdn("+1234567890");
        availableSim.setActivationDate(LocalDate.now().minusDays(10));
        availableSim.setExpiryDate(LocalDate.now().plusDays(365));

        assignedSim = new SIMCardEntity(
                "SIM-002",
                "8988211000000000002",
                SIMCardStatus.ACTIVE
        );
        assignedSim.setImsi("310260000000001");
        assignedSim.setMsisdn("+1234567891");
        assignedSim.setAssignedToType("CUSTOMER");
        assignedSim.setAssignedToId(customerId1);
        assignedSim.setAssignedToName("John Doe");
        assignedSim.setAssignedDate(LocalDate.now().minusDays(15));
        assignedSim.setActivationDate(LocalDate.now().minusDays(30));
        assignedSim.setExpiryDate(LocalDate.now().plusDays(335));

        expiredSim = new SIMCardEntity(
                "SIM-003",
                "8988211000000000003",
                SIMCardStatus.ACTIVE
        );
        expiredSim.setImsi("310260000000002");
        expiredSim.setMsisdn("+1234567892");
        expiredSim.setActivationDate(LocalDate.now().minusDays(400));
        expiredSim.setExpiryDate(LocalDate.now().minusDays(30)); // Already expired

        suspendedSim = new SIMCardEntity(
                "SIM-004",
                "8988211000000000004",
                SIMCardStatus.SUSPENDED
        );
        suspendedSim.setImsi("310260000000003");
        suspendedSim.setMsisdn("+1234567893");
        suspendedSim.setActivationDate(LocalDate.now().minusDays(100));
        suspendedSim.setExpiryDate(LocalDate.now().plusDays(265));

        simCardRepository.saveAll(List.of(availableSim, assignedSim, expiredSim, suspendedSim));
    }

    // ========== ASSET REPOSITORY TESTS ==========

    @Test
    void shouldFindAssetsByStatus() {
        // Given - 1 AVAILABLE asset exists

        // When
        List<AssetEntity> availableAssets = assetRepository.findByStatus(AssetStatus.AVAILABLE);

        // Then
        assertEquals(1, availableAssets.size());
        assertEquals("ASSET-001", availableAssets.get(0).getAssetTag());
    }

    @Test
    void shouldFindAssetsByAssetType() {
        // Given - 1 ROUTER asset exists

        // When
        List<AssetEntity> routers = assetRepository.findByAssetType(AssetType.ROUTER);

        // Then
        assertEquals(1, routers.size());
        assertEquals("Cisco Router 2960", routers.get(0).getName());
    }

    @Test
    void shouldFindAssetsByCustomerId() {
        // Given - 1 asset assigned to customer-001

        // When
        List<AssetEntity> customerAssets = assetRepository.findByCustomerId(customerId1);

        // Then
        assertEquals(1, customerAssets.size());
        assertEquals("ASSET-002", customerAssets.get(0).getAssetTag());
        assertEquals("John Doe", customerAssets.get(0).getAssignedToName());
    }

    @Test
    void shouldFindAvailableAssets() {
        // When
        List<AssetEntity> available = assetRepository.findAvailable();

        // Then
        assertEquals(1, available.size());
        assertEquals("ASSET-001", available.get(0).getAssetTag());
    }

    @Test
    void shouldFindAssetsWithExpiringWarranty() {
        // Given - Asset with warranty in 365 days

        // When
        List<AssetEntity> expiringAssets = assetRepository.findWarrantyExpiringBetween(
                LocalDate.now(),
                LocalDate.now().plusDays(365)
        );

        // Then
        assertEquals(1, expiringAssets.size());
        assertEquals("ASSET-001", expiringAssets.get(0).getAssetTag());
    }

    @Test
    void shouldFindAssetByAssetTag() {
        // When
        Optional<AssetEntity> asset = assetRepository.findByAssetTagAndDeletedAtIsNull("ASSET-001");

        // Then
        assertTrue(asset.isPresent());
        assertEquals("Cisco Router 2960", asset.get().getName());
    }

    @Test
    void shouldFindAssetBySerialNumber() {
        // When
        Optional<AssetEntity> asset = assetRepository.findBySerialNumberAndDeletedAtIsNull("SN123456789");

        // Then
        assertTrue(asset.isPresent());
        assertEquals("ASSET-001", asset.get().getAssetTag());
    }

    @Test
    void shouldCountAssetsByType() {
        // When
        List<Object[]> counts = assetRepository.countByAssetType();

        // Then
        assertTrue(counts.size() > 0);
        // Verify that we have counts for each asset type
        boolean hasRouter = counts.stream().anyMatch(arr -> arr[0].equals(AssetType.ROUTER));
        assertTrue(hasRouter);
    }

    @Test
    void shouldCountAssetsByStatus() {
        // When
        List<Object[]> counts = assetRepository.countByStatus();

        // Then
        assertTrue(counts.size() > 0);
        boolean hasAvailable = counts.stream().anyMatch(arr -> arr[0].equals(AssetStatus.AVAILABLE));
        assertTrue(hasAvailable);
    }

    @Test
    void shouldFindAssetsInMaintenance() {
        // When
        List<AssetEntity> maintenanceAssets = assetRepository.findInMaintenance();

        // Then
        assertEquals(1, maintenanceAssets.size());
        assertEquals("ASSET-003", maintenanceAssets.get(0).getAssetTag());
    }

    @Test
    void shouldFindAssetsByLocation() {
        // When
        List<AssetEntity> warehouseAssets = assetRepository.findByLocation("Warehouse A");

        // Then
        assertEquals(1, warehouseAssets.size());
        assertEquals("ASSET-001", warehouseAssets.get(0).getAssetTag());
    }

    @Test
    void shouldFindDamagedAssets() {
        // When
        List<AssetEntity> damaged = assetRepository.findDamaged();

        // Then
        assertEquals(1, damaged.size());
        assertEquals("ASSET-004", damaged.get(0).getAssetTag());
    }

    // ========== NETWORK ELEMENT REPOSITORY TESTS ==========

    @Test
    void shouldFindNetworkElementsByType() {
        // Given - 1 ROUTER element exists

        // When
        List<NetworkElementEntity> routers = networkElementRepository.findByElementType(NetworkElementType.ROUTER);

        // Then
        assertEquals(1, routers.size());
        assertEquals("Core Router", routers.get(0).getName());
    }

    @Test
    void shouldFindNetworkElementsByStatus() {
        // Given - 1 AVAILABLE element exists

        // When
        List<NetworkElementEntity> availableElements = networkElementRepository.findByStatus(AssetStatus.AVAILABLE);

        // Then
        assertEquals(1, availableElements.size());
        assertEquals("NE-001", availableElements.get(0).getElementId());
    }

    @Test
    void shouldFindNetworkElementByElementId() {
        // When
        Optional<NetworkElementEntity> element = networkElementRepository.findByElementIdAndDeletedAtIsNull("NE-001");

        // Then
        assertTrue(element.isPresent());
        assertEquals("Core Router", element.get().getName());
    }

    @Test
    void shouldFindOnlineNetworkElements() {
        // Given - Elements with recent vs stale heartbeats

        // When
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<NetworkElementEntity> online = networkElementRepository.findOnlineElements(threshold);

        // Then - Only active router has recent heartbeat
        assertEquals(1, online.size());
        assertEquals("NE-001", online.get(0).getElementId());
    }

    @Test
    void shouldFindElementsNeedingHeartbeat() {
        // When
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<NetworkElementEntity> needingHeartbeat = networkElementRepository.findElementsNeedingHeartbeat(threshold);

        // Then - Switch and maintenance element have stale heartbeats
        assertEquals(2, needingHeartbeat.size());
        List<String> elementIds = needingHeartbeat.stream()
                .map(NetworkElementEntity::getElementId)
                .toList();
        assertTrue(elementIds.contains("NE-002"));
        assertTrue(elementIds.contains("NE-003"));
    }

    @Test
    void shouldFindNetworkElementsInMaintenance() {
        // When
        List<NetworkElementEntity> maintenanceElements = networkElementRepository.findInMaintenance();

        // Then
        assertEquals(1, maintenanceElements.size());
        assertEquals("NE-003", maintenanceElements.get(0).getElementId());
    }

    @Test
    void shouldFindNetworkElementsByLocation() {
        // When
        List<NetworkElementEntity> dataCenterElements = networkElementRepository.findByLocation("Data Center 1");

        // Then
        assertEquals(1, dataCenterElements.size());
        assertEquals("NE-001", dataCenterElements.get(0).getElementId());
    }

    @Test
    void shouldCountNetworkElementsByType() {
        // When
        List<Object[]> counts = networkElementRepository.countByElementType();

        // Then
        assertTrue(counts.size() > 0);
        boolean hasRouter = counts.stream().anyMatch(arr -> arr[0].equals(NetworkElementType.ROUTER));
        assertTrue(hasRouter);
    }

    @Test
    void shouldCountNetworkElementsByStatus() {
        // When
        List<Object[]> counts = networkElementRepository.countByStatus();

        // Then
        assertTrue(counts.size() > 0);
    }

    @Test
    void shouldFindNetworkElementsByIpAddress() {
        // When
        List<NetworkElementEntity> byIp = networkElementRepository.findByIpAddress("10.0.0.1");

        // Then
        assertEquals(1, byIp.size());
        assertEquals("NE-001", byIp.get(0).getElementId());
    }

    // ========== SIM CARD REPOSITORY TESTS ==========

    @Test
    void shouldFindSIMCardsByStatus() {
        // Given - 1 AVAILABLE SIM exists

        // When
        List<SIMCardEntity> availableSims = simCardRepository.findByStatus(SIMCardStatus.AVAILABLE);

        // Then
        assertEquals(1, availableSims.size());
        assertEquals("SIM-001", availableSims.get(0).getSimTag());
    }

    @Test
    void shouldFindSIMCardByIccid() {
        // When
        Optional<SIMCardEntity> sim = simCardRepository.findByIccidAndDeletedAtIsNull("8988211000000000001");

        // Then
        assertTrue(sim.isPresent());
        assertEquals("SIM-001", sim.get().getSimTag());
    }

    @Test
    void shouldFindSIMCardByImsi() {
        // When
        Optional<SIMCardEntity> sim = simCardRepository.findByImsiAndDeletedAtIsNull("310260000000000");

        // Then
        assertTrue(sim.isPresent());
        assertEquals("SIM-001", sim.get().getSimTag());
    }

    @Test
    void shouldFindSIMCardByMsisdn() {
        // When
        Optional<SIMCardEntity> sim = simCardRepository.findByMsisdnAndDeletedAtIsNull("+1234567890");

        // Then
        assertTrue(sim.isPresent());
        assertEquals("SIM-001", sim.get().getSimTag());
    }

    @Test
    void shouldFindAvailableSIMCards() {
        // When
        List<SIMCardEntity> available = simCardRepository.findAvailable();

        // Then
        assertEquals(1, available.size());
        assertEquals("SIM-001", available.get(0).getSimTag());
    }

    @Test
    void shouldFindSIMCardsByCustomerId() {
        // Given - 1 SIM assigned to customer-001

        // When
        List<SIMCardEntity> customerSims = simCardRepository.findByCustomerId(customerId1);

        // Then
        assertEquals(1, customerSims.size());
        assertEquals("SIM-002", customerSims.get(0).getSimTag());
    }

    @Test
    void shouldFindExpiredSIMCards() {
        // When
        List<SIMCardEntity> expired = simCardRepository.findExpired(LocalDate.now());

        // Then - SIM-003 expired 30 days ago
        assertEquals(1, expired.size());
        assertEquals("SIM-003", expired.get(0).getSimTag());
    }

    @Test
    void shouldFindSIMCardsExpiringBetween() {
        // When - Find SIMs expiring in next 365 days
        List<SIMCardEntity> expiring = simCardRepository.findExpiringBetween(
                LocalDate.now(),
                LocalDate.now().plusDays(365)
        );

        // Then - SIM-001 (365 days) and SIM-002 (335 days) should be in range
        assertTrue(expiring.size() >= 2);
    }

    @Test
    void shouldFindSIMCardsWithDataLimits() {
        // When - SIMs without data limits in this test
        List<SIMCardEntity> withLimits = simCardRepository.findWithDataLimits();

        // Then - All SIMs have null data limits in this test
        assertEquals(0, withLimits.size());
    }

    @Test
    void shouldCountSIMCardsByStatus() {
        // When
        List<Object[]> counts = simCardRepository.countByStatus();

        // Then
        assertTrue(counts.size() > 0);
        boolean hasAvailable = counts.stream().anyMatch(arr -> arr[0].equals(SIMCardStatus.AVAILABLE));
        assertTrue(hasAvailable);
    }

    @Test
    void shouldFindSuspendedSIMCards() {
        // When
        List<SIMCardEntity> suspended = simCardRepository.findSuspended();

        // Then
        assertEquals(1, suspended.size());
        assertEquals("SIM-004", suspended.get(0).getSimTag());
    }

    @Test
    void shouldFindDamagedSIMCards() {
        // When - No damaged SIMs in this test

        // Then
        assertEquals(0, simCardRepository.findDamaged().size());
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void shouldSupportComplexAssetQueries() {
        // Test that complex queries work together

        // Find all assets for a customer
        List<AssetEntity> customerAssets = assetRepository.findByCustomerId(customerId1);
        assertEquals(1, customerAssets.size());

        // Find available assets
        List<AssetEntity> available = assetRepository.findAvailable();
        assertEquals(1, available.size());

        // Verify they are different
        assertNotEquals(customerAssets.get(0).getId(), available.get(0).getId());
    }

    @Test
    void shouldSupportNetworkElementHeartbeatQueries() {
        // Test heartbeat-based queries work together

        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(5);
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(5);

        List<NetworkElementEntity> online = networkElementRepository.findOnlineElements(recentThreshold);
        List<NetworkElementEntity> offline = networkElementRepository.findElementsNeedingHeartbeat(staleThreshold);

        // All elements should be categorized
        assertEquals(3, online.size() + offline.size());
    }

    @Test
    void shouldSupportSIMCardStatusQueries() {
        // Test SIM card status queries work together

        List<SIMCardEntity> available = simCardRepository.findAvailable();
        List<SIMCardEntity> assigned = simCardRepository.findByCustomerId(customerId1);
        List<SIMCardEntity> suspended = simCardRepository.findSuspended();

        // Verify they are different sets
        assertNotEquals(available.get(0).getId(), assigned.get(0).getId());
        assertNotEquals(suspended.get(0).getId(), available.get(0).getId());
    }

    @Test
    void shouldHandleEmptyResultsGracefully() {
        // Test queries return empty results when no matches

        // Customer with no assets
        List<AssetEntity> noAssets = assetRepository.findByCustomerId("non-existent-customer");
        assertEquals(0, noAssets.size());

        // Non-existent element ID
        Optional<NetworkElementEntity> noElement = networkElementRepository.findByElementIdAndDeletedAtIsNull("NON-EXISTENT");
        assertFalse(noElement.isPresent());

        // Non-existent ICCID
        Optional<SIMCardEntity> noSim = simCardRepository.findByIccidAndDeletedAtIsNull("9999999999999999999");
        assertFalse(noSim.isPresent());
    }
}
