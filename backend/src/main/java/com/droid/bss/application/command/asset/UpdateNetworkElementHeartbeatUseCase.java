package com.droid.bss.application.command.asset;

import com.droid.bss.domain.asset.NetworkElementEntity;
import com.droid.bss.domain.asset.NetworkElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating network element heartbeat
 */
@Service
public class UpdateNetworkElementHeartbeatUseCase {

    private final NetworkElementRepository elementRepository;

    public UpdateNetworkElementHeartbeatUseCase(NetworkElementRepository elementRepository) {
        this.elementRepository = elementRepository;
    }

    @Transactional
    public void handle(String elementId) {
        NetworkElementEntity element = elementRepository.findById(elementId)
                .orElseThrow(() -> new RuntimeException("Network element not found: " + elementId));

        element.updateHeartbeat();

        elementRepository.save(element);
    }
}
