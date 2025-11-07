package com.droid.bss.application.command.partner;

import com.droid.bss.domain.partner.PartnerEntity;
import com.droid.bss.infrastructure.database.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating partners
 */
@Service
@Transactional
public class UpdatePartnerUseCase {

    private final PartnerRepository partnerRepository;

    public UpdatePartnerUseCase(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public PartnerEntity handle(String partnerId, UpdatePartnerCommand command) {
        // Get existing partner
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        // Update fields if provided
        if (command.name() != null) {
            partner.setName(command.name());
        }
        if (command.description() != null) {
            partner.setDescription(command.description());
        }
        if (command.contactPerson() != null) {
            partner.setContactPerson(command.contactPerson());
        }
        if (command.email() != null) {
            partner.setEmail(command.email());
        }
        if (command.phone() != null) {
            partner.setPhone(command.phone());
        }
        if (command.address() != null) {
            partner.setAddress(command.address());
        }
        if (command.taxId() != null) {
            partner.setTaxId(command.taxId());
        }
        if (command.registrationNumber() != null) {
            partner.setRegistrationNumber(command.registrationNumber());
        }
        if (command.contractStartDate() != null) {
            partner.setContractStartDate(command.contractStartDate());
        }
        if (command.contractEndDate() != null) {
            partner.setContractEndDate(command.contractEndDate());
        }
        if (command.commissionRate() != null) {
            partner.setCommissionRate(command.commissionRate());
        }
        if (command.settlementFrequency() != null) {
            partner.setSettlementFrequency(command.settlementFrequency());
        }
        if (command.paymentTerms() != null) {
            partner.setPaymentTerms(command.paymentTerms());
        }
        if (command.creditLimit() != null) {
            partner.setCreditLimit(command.creditLimit());
        }
        if (command.territory() != null) {
            partner.setTerritory(command.territory());
        }
        if (command.marketSegment() != null) {
            partner.setMarketSegment(command.marketSegment());
        }
        if (command.contractValue() != null) {
            partner.setContractValue(command.contractValue());
        }
        if (command.servicesProvided() != null) {
            partner.setServicesProvided(command.servicesProvided());
        }
        if (command.notes() != null) {
            partner.setNotes(command.notes());
        }

        return partnerRepository.save(partner);
    }

    // Command DTO
    public record UpdatePartnerCommand(
            String name,
            String description,
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
    ) {}
}
