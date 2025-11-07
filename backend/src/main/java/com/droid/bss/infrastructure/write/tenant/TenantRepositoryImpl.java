/**
 * Tenant Repository Implementation - Adapter (Hexagonal Architecture)
 *
 * Implementation of TenantRepository port
 * This is an ADAPTER in hexagonal architecture
 */
package com.droid.bss.infrastructure.write.tenant;

import com.droid.bss.domain.tenant.Tenant;
import com.droid.bss.domain.tenant.TenantRepository;
import com.droid.bss.domain.tenant.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository implementation for Tenant entity
 * This is the adapter that implements the port interface
 */
@Repository
public interface TenantRepositoryImpl extends TenantRepository, JpaRepository<Tenant, UUID> {
    // Inherited methods from TenantRepository interface
    // Additional custom implementations can be added here if needed
}
