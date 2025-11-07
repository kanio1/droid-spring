package com.droid.bss.application.query.monitoring;

import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.domain.monitoring.CostCalculationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class GetCostCalculationsUseCase {

    private final CostCalculationRepository costCalculationRepository;

    public GetCostCalculationsUseCase(CostCalculationRepository costCalculationRepository) {
        this.costCalculationRepository = costCalculationRepository;
    }

    public List<CostCalculation> getById(Long id) {
        return costCalculationRepository.findById(id)
                .map(List::of)
                .orElse(List.of());
    }

    public List<CostCalculation> getByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate) {
        return costCalculationRepository.findByCustomerIdAndPeriod(customerId, startDate, endDate);
    }

    public List<CostCalculation> getByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return costCalculationRepository.findByCustomerIdAndResourceType(customerId, resourceType);
    }

    public List<CostCalculation> getByStatus(String status) {
        return costCalculationRepository.findByStatus(status);
    }
}
