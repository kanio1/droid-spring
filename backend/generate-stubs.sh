#!/bin/bash
################################################################################
# Domain Model Stub Generator
# Generates minimal stub classes to enable compilation
################################################################################

set -e

STUBS_DIR="src/main/java-stubs"

echo "Creating stub directory..."
mkdir -p "$STUBS_DIR"

# Create package structure
echo "Creating package structure..."

# Common stubs
cat > "$STUBS_DIR/ServiceActivationService.java" << 'EOF'
package com.droid.bss.application.command.service;

import com.droid.bss.domain.service.ServiceActivationEntity;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ServiceActivationService {
    public Optional<ServiceActivationEntity> getServiceActivation(String id) {
        return Optional.empty();
    }
}
EOF

cat > "$STUBS_DIR/ServiceActivationRepository.java" << 'EOF'
package com.droid.bss.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceActivationRepository extends JpaRepository<ServiceActivationEntity, UUID> {
    Optional<ServiceActivationEntity> findById(UUID id);
}
EOF

cat > "$STUBS_DIR/ServiceActivationStepRepository.java" << 'EOF'
package com.droid.bss.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ServiceActivationStepRepository extends JpaRepository<ServiceActivationStepEntity, UUID> {
}
EOF

cat > "$STUBS_DIR/ServiceEventPublisher.java" << 'EOF'
package com.droid.bss.domain.service.event;

import com.droid.bss.domain.service.ServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceEventPublisher {
    public void publishServiceDeactivated(ServiceEntity service, String customerId, String reason) {
        // Stub implementation
    }
}
EOF

cat > "$STUBS_DIR/ActivationStatus.java" << 'EOF'
package com.droid.bss.domain.service;

public enum ActivationStatus {
    PENDING, ACTIVE, SUSPENDED, DEPRECATED, DEPProvisionING
}
EOF

cat > "$STUBS_DIR/ServiceActivationEntity.java" << 'EOF'
package com.droid.bss.domain.service;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "service_activations")
public class ServiceActivationEntity extends BaseEntity {

    @ManyToOne
    private ServiceEntity service;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivationStatus status;

    @OneToMany(mappedBy = "activation", cascade = CascadeType.ALL)
    private Set<ServiceActivationStepEntity> steps = new HashSet<>();

    public ServiceEntity getService() { return service; }
    public void setService(ServiceEntity service) { this.service = service; }

    public ActivationStatus getStatus() { return status; }
    public void setStatus(ActivationStatus status) { this.status = status; }

    public boolean isActive() { return status == ActivationStatus.ACTIVE; }

    public void addStep(ServiceActivationStepEntity step) {
        steps.add(step);
        step.setActivation(this);
    }
}
EOF

cat > "$STUBS_DIR/ServiceActivationStepEntity.java" << 'EOF'
package com.droid.bss.domain.service;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "service_activation_steps")
public class ServiceActivationStepEntity extends BaseEntity {

    @ManyToOne
    private ServiceActivationEntity activation;

    @Column(nullable = false)
    private Integer stepNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceActivationStepStatus status;

    @Column(name = "provisioning_system")
    private String provisioningSystem;

    @Column(name = "provisioning_command")
    private String provisioningCommand;

    public ServiceActivationEntity getActivation() { return activation; }
    public void setActivation(ServiceActivationEntity activation) { this.activation = activation; }

    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ServiceActivationStepStatus getStatus() { return status; }
    public void setStatus(ServiceActivationStepStatus status) { this.status = status; }

    public String getProvisioningSystem() { return provisioningSystem; }
    public void setProvisioningSystem(String provisioningSystem) { this.provisioningSystem = provisioningSystem; }

    public String getProvisioningCommand() { return provisioningCommand; }
    public void setProvisioningCommand(String provisioningCommand) { this.provisioningCommand = provisioningCommand; }

    public ServiceActivationStepEntity() {}

    public ServiceActivationStepEntity(Integer stepNumber, String name, String description, ServiceActivationStepStatus status) {
        this.stepNumber = stepNumber;
        this.name = name;
        this.description = description;
        this.status = status;
    }
}
EOF

cat > "$STUBS_DIR/BusinessMetrics.java" << 'EOF'
package com.droid.bss.infrastructure.metrics;

import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    public void incrementUsageRecordIngested() {}
    public void incrementUsageRecordRated() {}
}
EOF

cat > "$STUBS_DIR/IngestUsageRecordCommand.java" << 'EOF'
package com.droid.bss.application.dto.billing;

public record IngestUsageRecordCommand(
    String subscriptionId,
    String usageType,
    String usageUnit,
    Double usageAmount,
    String usageDate,
    String usageTime,
    String destinationType,
    String destinationNumber,
    String destinationCountry,
    String networkId,
    String ratePeriod,
    String source,
    String sourceFile
) {}
EOF

cat > "$STUBS_DIR/UsageRecordResponse.java" << 'EOF'
package com.droid.bss.application.dto.billing;

import com.droid.bss.domain.billing.UsageRecordEntity;

public class UsageRecordResponse {
    public static UsageRecordResponse from(UsageRecordEntity entity) {
        return new UsageRecordResponse();
    }
}
EOF

echo "Stub generation complete!"
echo "Generated stubs in: $STUBS_DIR"
EOF

chmod +x generate-stubs.sh
echo "Stub generator script created!"
