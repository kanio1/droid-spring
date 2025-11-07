package com.droid.bss.api.partner;

import com.droid.bss.application.command.partner.*;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.partner.PartnerEntity;
import com.droid.bss.domain.partner.PartnerSettlement;
import com.droid.bss.domain.partner.PartnerType;
import com.droid.bss.infrastructure.audit.Audited;
import com.droid.bss.infrastructure.database.repository.PartnerRepository;
import com.droid.bss.infrastructure.database.repository.PartnerSettlementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for partner management
 */
@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final CreatePartnerUseCase createPartnerUseCase;
    private final UpdatePartnerUseCase updatePartnerUseCase;
    private final PartnerRepository partnerRepository;
    private final PartnerSettlementRepository settlementRepository;

    public PartnerController(
            CreatePartnerUseCase createPartnerUseCase,
            UpdatePartnerUseCase updatePartnerUseCase,
            PartnerRepository partnerRepository,
            PartnerSettlementRepository settlementRepository
    ) {
        this.createPartnerUseCase = createPartnerUseCase;
        this.updatePartnerUseCase = updatePartnerUseCase;
        this.partnerRepository = partnerRepository;
        this.settlementRepository = settlementRepository;
    }

    @GetMapping
    public ResponseEntity<List<PartnerEntity>> getAllPartners(
            @RequestParam(required = false) PartnerType type,
            @RequestParam(required = false) String status
    ) {
        List<PartnerEntity> partners;
        if (type != null) {
            partners = partnerRepository.findByPartnerType(type);
        } else {
            partners = partnerRepository.findAll();
        }
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerEntity> getPartnerById(@PathVariable String id) {
        Optional<PartnerEntity> partner = partnerRepository.findById(id);
        return partner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PartnerEntity> getPartnerByCode(@PathVariable String code) {
        Optional<PartnerEntity> partner = partnerRepository.findByPartnerCode(code);
        return partner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Audited(action = AuditAction.PARTNER_CREATE, entityType = "Partner", description = "Creating new partner")
    public ResponseEntity<PartnerEntity> createPartner(@RequestBody CreatePartnerUseCase.CreatePartnerCommand command) {
        PartnerEntity partner = createPartnerUseCase.handle(command);
        return ResponseEntity.ok(partner);
    }

    @PutMapping("/{id}")
    @Audited(action = AuditAction.PARTNER_UPDATE, entityType = "Partner", description = "Updating partner {id}")
    public ResponseEntity<PartnerEntity> updatePartner(
            @PathVariable String id,
            @RequestBody UpdatePartnerUseCase.UpdatePartnerCommand command
    ) {
        PartnerEntity partner = updatePartnerUseCase.handle(id, command);
        return ResponseEntity.ok(partner);
    }

    @PostMapping("/{id}/activate")
    @Audited(action = AuditAction.PARTNER_UPDATE, entityType = "Partner", description = "Activating partner {id}")
    public ResponseEntity<PartnerEntity> activatePartner(@PathVariable String id) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            PartnerEntity partner = partnerOpt.get();
            partner.activate();
            partnerRepository.save(partner);
            return ResponseEntity.ok(partner);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/suspend")
    @Audited(action = AuditAction.PARTNER_UPDATE, entityType = "Partner", description = "Suspending partner {id}")
    public ResponseEntity<PartnerEntity> suspendPartner(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.get("reason");
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            PartnerEntity partner = partnerOpt.get();
            partner.suspend(reason);
            partnerRepository.save(partner);
            return ResponseEntity.ok(partner);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/terminate")
    @Audited(action = AuditAction.PARTNER_UPDATE, entityType = "Partner", description = "Terminating partner {id}")
    public ResponseEntity<PartnerEntity> terminatePartner(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.get("reason");
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            PartnerEntity partner = partnerOpt.get();
            partner.terminate(reason);
            partnerRepository.save(partner);
            return ResponseEntity.ok(partner);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/settlements")
    public ResponseEntity<List<PartnerSettlement>> getPartnerSettlements(@PathVariable String id) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            List<PartnerSettlement> settlements = settlementRepository.findByPartnerIdOrderBySettlementDateDesc(id);
            return ResponseEntity.ok(settlements);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PartnerSummary> getPartnerSummary(@PathVariable String id) {
        Optional<PartnerEntity> partnerOpt = partnerRepository.findById(id);
        if (partnerOpt.isPresent()) {
            PartnerEntity partner = partnerOpt.get();
            PartnerSummary summary = new PartnerSummary(
                    partner.getPartnerCode(),
                    partner.getName(),
                    partner.getPartnerType(),
                    partner.getStatus(),
                    partner.getTotalSales(),
                    partner.getTotalCommission(),
                    partner.getCurrentBalance(),
                    partner.getCommissionRate(),
                    partner.getContractStartDate(),
                    partner.getContractEndDate()
            );
            return ResponseEntity.ok(summary);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<PartnerEntity>> getTopPerformers(@RequestParam(defaultValue = "10") int limit) {
        List<PartnerEntity> topPartners = partnerRepository.findTopPartnersBySales(limit);
        return ResponseEntity.ok(topPartners);
    }

    @GetMapping("/expiring-contracts")
    public ResponseEntity<List<PartnerEntity>> getExpiringContracts(
            @RequestParam(defaultValue = "30") int days
    ) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        List<PartnerEntity> expiringPartners = partnerRepository
                .findContractsExpiringBetween(LocalDate.now(), endDate);
        return ResponseEntity.ok(expiringPartners);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PartnerEntity>> searchPartners(@RequestParam String q) {
        List<PartnerEntity> partners = partnerRepository.searchPartners(q);
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/statistics")
    public ResponseEntity<PartnerStatistics> getPartnerStatistics() {
        long activePartners = partnerRepository.countByStatus(com.droid.bss.domain.partner.PartnerStatus.ACTIVE);
        long suspendedPartners = partnerRepository.countByStatus(com.droid.bss.domain.partner.PartnerStatus.SUSPENDED);
        long totalPartners = partnerRepository.count();

        return ResponseEntity.ok(new PartnerStatistics(
                totalPartners,
                activePartners,
                suspendedPartners,
                totalPartners - activePartners - suspendedPartners
        ));
    }

    // DTOs
    public record PartnerSummary(
            String partnerCode,
            String name,
            PartnerType type,
            com.droid.bss.domain.partner.PartnerStatus status,
            java.math.BigDecimal totalSales,
            java.math.BigDecimal totalCommission,
            java.math.BigDecimal currentBalance,
            java.math.BigDecimal commissionRate,
            java.time.LocalDate contractStartDate,
            java.time.LocalDate contractEndDate
    ) {}

    public record PartnerStatistics(
            long total,
            long active,
            long suspended,
            long other
    ) {}
}
