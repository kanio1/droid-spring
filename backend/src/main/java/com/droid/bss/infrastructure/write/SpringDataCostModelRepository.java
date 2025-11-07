package com.droid.bss.infrastructure.write;

import com.droid.bss.infrastructure.database.entity.CostModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataCostModelRepository extends JpaRepository<CostModelEntity, Long> {

    Optional<CostModelEntity> findByModelName(String modelName);

    List<CostModelEntity> findByActiveTrue();
}
