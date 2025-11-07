package com.droid.bss.domain.monitoring;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for CostModel
 */
public interface CostModelRepository {

    Optional<CostModel> findById(Long id);

    Optional<CostModel> findByModelName(String modelName);

    List<CostModel> findByActiveTrue();

    CostModel save(CostModel costModel);

    void deleteById(Long id);
}
