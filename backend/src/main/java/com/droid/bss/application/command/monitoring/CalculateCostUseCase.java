package com.droid.bss.application.command.monitoring;

import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.domain.monitoring.CostCalculationRepository;
import com.droid.bss.domain.monitoring.CostModel;
import com.droid.bss.domain.monitoring.CostModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class CalculateCostUseCase {

    private final CostCalculationRepository costCalculationRepository;
    private final CostModelRepository costModelRepository;

    public CalculateCostUseCase(CostCalculationRepository costCalculationRepository,
                                CostModelRepository costModelRepository) {
        this.costCalculationRepository = costCalculationRepository;
        this.costModelRepository = costModelRepository;
    }

    public CostCalculation calculate(Long customerId, String resourceType, String billingPeriod,
                                     Instant periodStart, Instant periodEnd, BigDecimal totalUsage,
                                     Long costModelId, String currency) {
        Optional<CostModel> costModelOpt = costModelRepository.findById(costModelId);
        if (costModelOpt.isEmpty()) {
            throw new IllegalArgumentException("Cost model not found with id: " + costModelId);
        }

        CostModel costModel = costModelOpt.get();
        if (!costModel.isActive()) {
            throw new IllegalArgumentException("Cost model is not active: " + costModel.getModelName());
        }

        CostCalculation calculation = new CostCalculation(
                customerId,
                resourceType,
                billingPeriod,
                periodStart,
                periodEnd,
                totalUsage,
                costModel,
                currency
        );

        return costCalculationRepository.save(calculation);
    }

    public CostCalculation recalculate(Long calculationId) {
        Optional<CostCalculation> calculationOpt = costCalculationRepository.findById(calculationId);
        if (calculationOpt.isEmpty()) {
            throw new IllegalArgumentException("Cost calculation not found with id: " + calculationId);
        }

        CostCalculation calculation = calculationOpt.get();
        calculation.calculateCosts();

        return costCalculationRepository.save(calculation);
    }
}
