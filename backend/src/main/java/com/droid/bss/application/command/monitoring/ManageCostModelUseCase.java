package com.droid.bss.application.command.monitoring;

import com.droid.bss.domain.monitoring.CostModel;
import com.droid.bss.domain.monitoring.CostModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ManageCostModelUseCase {

    private final CostModelRepository costModelRepository;

    public ManageCostModelUseCase(CostModelRepository costModelRepository) {
        this.costModelRepository = costModelRepository;
    }

    public CostModel create(String modelName, String description, String billingPeriod,
                           BigDecimal baseCost, BigDecimal overageRate, BigDecimal includedUsage,
                           String currency, boolean active) {
        Optional<CostModel> existing = costModelRepository.findByModelName(modelName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Cost model already exists with name: " + modelName);
        }

        CostModel model = new CostModel();
        model.setModelName(modelName);
        model.setDescription(description);
        model.setBillingPeriod(billingPeriod);
        model.setBaseCost(baseCost);
        model.setOverageRate(overageRate);
        model.setIncludedUsage(includedUsage);
        model.setCurrency(currency);
        model.setActive(active);
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());

        return costModelRepository.save(model);
    }

    public CostModel update(Long id, String description, BigDecimal baseCost, BigDecimal overageRate,
                           BigDecimal includedUsage, String currency, boolean active) {
        Optional<CostModel> modelOpt = costModelRepository.findById(id);
        if (modelOpt.isEmpty()) {
            throw new IllegalArgumentException("Cost model not found with id: " + id);
        }

        CostModel model = modelOpt.get();
        model.setDescription(description);
        model.setBaseCost(baseCost);
        model.setOverageRate(overageRate);
        model.setIncludedUsage(includedUsage);
        model.setCurrency(currency);
        model.setActive(active);
        model.setUpdatedAt(Instant.now());

        return costModelRepository.save(model);
    }

    public void delete(Long id) {
        Optional<CostModel> modelOpt = costModelRepository.findById(id);
        if (modelOpt.isEmpty()) {
            throw new IllegalArgumentException("Cost model not found with id: " + id);
        }
        costModelRepository.deleteById(id);
    }

    public List<CostModel> getActiveModels() {
        return costModelRepository.findByActiveTrue();
    }

    public Optional<CostModel> getById(Long id) {
        return costModelRepository.findById(id);
    }

    public Optional<CostModel> getByModelName(String modelName) {
        return costModelRepository.findByModelName(modelName);
    }
}
