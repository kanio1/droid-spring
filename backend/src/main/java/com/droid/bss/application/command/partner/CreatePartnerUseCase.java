package com.droid.bss.application.command.partner;

import com.droid.bss.domain.partner.PartnerEntity;
import com.droid.bss.domain.partner.PartnerType;
import com.droid.bss.infrastructure.database.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for creating partners
 */
@Service
@Transactional
public class CreatePartnerUseCase {

    private final PartnerRepository partnerRepository;

    public CreatePartnerUseCase(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public PartnerEntity handle(CreatePartnerCommand command) {
        // Validate
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Partner name is required");
        }
        if (command.partnerType() == null) {
            throw new IllegalArgumentException("Partner type is required");
        }

        // Generate partner code if not provided
        String partnerCode = command.partnerCode() != null ?
                command.partnerCode() : generatePartnerCode();

        // Check if code already exists
        if (partnerRepository.findByPartnerCode(partnerCode).isPresent()) {
            throw new IllegalArgumentException("Partner code already exists: " + partnerCode);
        }

        // Create partner
        PartnerEntity partner = new PartnerEntity(partnerCode, command.name(), command.partnerType());
        partner.setDescription(command.description());
        partner.setContactPerson(command.contactPerson());
        partner.setEmail(command.email());
        partner.setPhone(command.phone());
        partner.setAddress(command.address());
        partner.setTaxId(command.taxId());
        partner.setRegistrationNumber(command.registrationNumber());
        partner.setContractStartDate(command.contractStartDate());
        partner.setContractEndDate(command.contractEndDate());
        partner.setCommissionRate(command.commissionRate());
        partner.setSettlementFrequency(command.settlementFrequency());
        partner.setPaymentTerms(command.paymentTerms());
        partner.setCreditLimit(command.creditLimit());
        partner.setTerritory(command.territory());
        partner.setMarketSegment(command.marketSegment());
        partner.setContractValue(command.contractValue());
        partner.setServicesProvided(command.servicesProvided());
        partner.setNotes(command.notes());

        return partnerRepository.save(partner);
    }

    private String generatePartnerCode() {
        return "PART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Command DTO
    public record CreatePartnerCommand(
            String partnerCode,
            String name,
            String description,
            PartnerType partnerType,
            String contactPerson,
            String email,
            String phone,
            String address,
            String taxId,
            String registrationNumber,
            java.time.LocalDate contractStartDate,
            java.time.LocalDate contractEndDate,
            java.math.BigDecimal commissionRate,
            String settlementFrequency,
            String paymentTerms,
            java.math.BigDecimal creditLimit,
            String territory,
            String marketSegment,
            java.math.BigDecimal contractValue,
            String servicesProvided,
            String notes
    ) {
        public CreatePartnerCommand {
            if (commissionRate == null) commissionRate = java.math.BigDecimal.ZERO;
            if (creditLimit == null) creditLimit = java.math.BigDecimal.ZERO;
            if (contractValue == null) contractValue = java.math.BigDecimal.ZERO;
            if (settlementFrequency == null) settlementFrequency = "MONTHLY";
            if (paymentTerms == null) paymentTerms = "NET_30";
        }
    }
}
