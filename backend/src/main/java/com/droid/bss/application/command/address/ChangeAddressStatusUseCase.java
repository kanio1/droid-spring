package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.application.dto.address.ChangeAddressStatusCommand;
import com.droid.bss.domain.address.*;
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
        UUID addressIdUuid = UUID.fromString(command.id());
        AddressId addressId = AddressId.of(addressIdUuid);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + command.id()));

        AddressStatus status = AddressStatus.valueOf(command.status());

        // Prevent deactivating primary addresses
        if (address.isPrimary() && status == AddressStatus.INACTIVE) {
            throw new IllegalStateException("Cannot deactivate a primary address");
        }

        // Update status using immutable pattern
        Address updated = address.changeStatus(status);

        Address saved = addressRepository.save(updated);

        return AddressResponse.from(saved);
    }
}
