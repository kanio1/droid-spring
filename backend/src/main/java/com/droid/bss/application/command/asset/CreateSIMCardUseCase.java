package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.SIMCardResponse;
import com.droid.bss.application.dto.asset.CreateSIMCardCommand;
import com.droid.bss.domain.asset.SIMCardEntity;
import com.droid.bss.domain.asset.SIMCardRepository;
import com.droid.bss.domain.asset.SIMCardStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use case for creating SIM cards
 */
@Service
public class CreateSIMCardUseCase {

    private final SIMCardRepository simRepository;

    public CreateSIMCardUseCase(SIMCardRepository simRepository) {
        this.simRepository = simRepository;
    }

    @Transactional
    public SIMCardResponse handle(CreateSIMCardCommand command) {
        // Check if ICCID already exists
        simRepository.findByIccidAndDeletedAtIsNull(command.iccid())
                .ifPresent(s -> {
                    throw new IllegalStateException("ICCID already exists: " + command.iccid());
                });

        // Check if IMSI already exists
        if (command.imsi() != null) {
            simRepository.findByImsiAndDeletedAtIsNull(command.imsi())
                    .ifPresent(s -> {
                        throw new IllegalStateException("IMSI already exists: " + command.imsi());
                    });
        }

        // Create SIM card
        SIMCardEntity sim = new SIMCardEntity();
        sim.setIccid(command.iccid());
        sim.setMsisdn(command.msisdn());
        sim.setImsi(command.imsi());
        sim.setNetworkOperator(command.networkOperator());
        sim.setApn(command.apn());
        sim.setStatus(SIMCardStatus.valueOf(command.status()));

        // Set activation date if status is ASSIGNED
        if (sim.getStatus() == SIMCardStatus.ASSIGNED) {
            sim.setActivationDate(LocalDate.now());
        }

        SIMCardEntity saved = simRepository.save(sim);

        return SIMCardResponse.from(saved);
    }
}
