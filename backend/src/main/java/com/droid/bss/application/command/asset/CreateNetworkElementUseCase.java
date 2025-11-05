package com.droid.bss.application.command.asset;

import com.droid.bss.application.dto.asset.NetworkElementResponse;
import com.droid.bss.application.dto.asset.CreateNetworkElementCommand;
import com.droid.bss.domain.asset.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for creating network elements
 */
@Service
public class CreateNetworkElementUseCase {

    private final NetworkElementRepository elementRepository;

    public CreateNetworkElementUseCase(NetworkElementRepository elementRepository) {
        this.elementRepository = elementRepository;
    }

    @Transactional
    public NetworkElementResponse handle(CreateNetworkElementCommand command) {
        // Check if element ID already exists
        elementRepository.findByElementIdAndDeletedAtIsNull(command.elementId())
                .ifPresent(e -> {
                    throw new IllegalStateException("Element ID already exists: " + command.elementId());
                });

        // Create network element
        NetworkElementEntity element = new NetworkElementEntity();
        element.setElementId(command.elementId());
        element.setElementType(NetworkElementType.valueOf(command.elementType()));
        element.setName(command.name());
        element.setDescription(command.description());
        element.setIpAddress(command.ipAddress());
        element.setMacAddress(command.macAddress());
        element.setFirmwareVersion(command.firmwareVersion());
        element.setSoftwareVersion(command.softwareVersion());
        element.setLocation(command.location());
        element.setRackPosition(command.rackPosition());
        element.setPortCount(command.portCount());
        element.setCapacity(command.capacity());
        element.setStatus(AssetStatus.valueOf(command.status()));
        element.setOperationalSince(LocalDateTime.now());

        NetworkElementEntity saved = elementRepository.save(element);

        return NetworkElementResponse.from(saved);
    }
}
