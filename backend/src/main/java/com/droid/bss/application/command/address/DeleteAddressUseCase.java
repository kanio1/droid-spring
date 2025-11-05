package com.droid.bss.application.command.address;

import com.droid.bss.domain.address.AddressEntity;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        UUID id = UUID.fromString(addressId);

        AddressEntity address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        // Soft delete
        address.softDelete();

        addressRepository.save(address);
    }
}
