package com.droid.bss.application.command.monitoring;

import com.droid.bss.domain.monitoring.OptimizationRecommendation;
import com.droid.bss.domain.monitoring.OptimizationRecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ManageOptimizationRecommendationUseCase {

    private final OptimizationRecommendationRepository optimizationRecommendationRepository;

    public ManageOptimizationRecommendationUseCase(OptimizationRecommendationRepository optimizationRecommendationRepository) {
        this.optimizationRecommendationRepository = optimizationRecommendationRepository;
    }

    public Optional<OptimizationRecommendation> acknowledgeRecommendation(Long id) {
        Optional<OptimizationRecommendation> recOpt = optimizationRecommendationRepository.findById(id);
        if (recOpt.isPresent()) {
            OptimizationRecommendation rec = recOpt.get();
            rec.acknowledge();
            return Optional.of(optimizationRecommendationRepository.save(rec));
        }
        return Optional.empty();
    }

    public Optional<OptimizationRecommendation> implementRecommendation(Long id) {
        Optional<OptimizationRecommendation> recOpt = optimizationRecommendationRepository.findById(id);
        if (recOpt.isPresent()) {
            OptimizationRecommendation rec = recOpt.get();
            rec.implement();
            return Optional.of(optimizationRecommendationRepository.save(rec));
        }
        return Optional.empty();
    }

    public Optional<OptimizationRecommendation> dismissRecommendation(Long id) {
        Optional<OptimizationRecommendation> recOpt = optimizationRecommendationRepository.findById(id);
        if (recOpt.isPresent()) {
            OptimizationRecommendation rec = recOpt.get();
            rec.dismiss();
            return Optional.of(optimizationRecommendationRepository.save(rec));
        }
        return Optional.empty();
    }

    public void deleteRecommendation(Long id) {
        optimizationRecommendationRepository.deleteById(id);
    }
}
