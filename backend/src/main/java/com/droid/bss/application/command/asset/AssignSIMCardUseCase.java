package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.SIMCardResponse;
import com.droid.bss.domain.asset.SIMCardEntity;
import com.droid.bss.domain.asset.SIMCardRepository;
import com.droid.bss.domain.asset.SIMCardStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use case for assigning SIM cards
 */
@Service
public class AssignSIMCardUseCase {

    private final SIMCardRepository simRepository;

    public AssignSIMCardUseCase(SIMCardRepository simRepository) {
        this.simRepository = simRepository;
    }

    @Transactional
    public SIMCardResponse handle(String simId, String assignedToType, String assignedToId, String assignedToName) {
        SIMCardEntity sim = simRepository.findById(simId)
                .orElseThrow(() -> new RuntimeException("SIM card not found: " + simId));

        // Check if SIM is available
        if (!sim.isAvailable()) {
            throw new IllegalStateException("SIM card is not available: " + simId);
        }

        // Assign SIM
        sim.assignTo(assignedToType, assignedToId, assignedToName);
        sim.setActivationDate(LocalDate.now());

        SIMCardEntity saved = simRepository.save(sim);

        return SIMCardResponse.from(saved);
    }
}
