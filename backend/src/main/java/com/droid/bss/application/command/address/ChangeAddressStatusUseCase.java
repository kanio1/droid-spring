package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.application.dto.address.ChangeAddressStatusCommand;
import com.droid.bss.domain.address.AddressEntity;
import com.droid.bss.domain.address.AddressRepository;
import com.droid.bss.domain.address.AddressStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for changing address status
 */
@Service
public class ChangeAddressStatusUseCase {

    private final AddressRepository addressRepository;

    public ChangeAddressStatusUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public AddressResponse handle(ChangeAddressStatusCommand command) {
        UUID addressId = UUID.fromString(command.id());

        AddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + command.id()));

        AddressStatus status = AddressStatus.valueOf(command.status());

        // Prevent deactivating primary addresses
        if (address.isPrimary() && status == AddressStatus.INACTIVE) {
            throw new IllegalStateException("Cannot deactivate a primary address");
        }

        address.setStatus(status);

        AddressEntity saved = addressRepository.save(address);

        return AddressResponse.from(saved);
    }
}
