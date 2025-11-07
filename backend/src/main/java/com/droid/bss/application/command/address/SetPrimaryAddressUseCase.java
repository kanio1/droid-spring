package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.application.dto.address.SetPrimaryAddressCommand;
import com.droid.bss.domain.address.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for setting an address as primary
 */
@Service
public class SetPrimaryAddressUseCase {

    private final AddressRepository addressRepository;

    public SetPrimaryAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public AddressResponse handle(SetPrimaryAddressCommand command) {
        UUID addressIdUuid = UUID.fromString(command.addressId());
        AddressId addressId = AddressId.of(addressIdUuid);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + command.addressId()));

        // Check if address is active
        if (!address.isActive()) {
            throw new IllegalStateException("Cannot set an inactive address as primary");
        }

        // Unset primary flag from existing primary address of same type
        addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                address.getCustomerId().value(),
                address.getType()
        ).ifPresent(existing -> {
            Address unmarked = existing.unmarkAsPrimary();
            addressRepository.save(unmarked);
        });

        // Set this address as primary
        Address marked = address.markAsPrimary();

        Address saved = addressRepository.save(marked);

        return AddressResponse.from(saved);
    }
}
