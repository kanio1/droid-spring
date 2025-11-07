package com.droid.bss.application.command.address;

import com.droid.bss.domain.address.AddressId;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for soft-deleting an address
 */
@Service
public class DeleteAddressUseCase {

    private final AddressRepository addressRepository;

    public DeleteAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public void handle(String addressId) {
        AddressId id = new AddressId(java.util.UUID.fromString(addressId));

        // Check if address exists
        addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        // Soft delete
        addressRepository.deleteById(id);
    }
}
