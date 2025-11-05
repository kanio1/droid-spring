package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.AddressEntity;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case for retrieving a single address by ID
 */
@Service
public class GetAddressUseCase {

    private final AddressRepository addressRepository;

    public GetAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressResponse handle(String addressId) {
        UUID id = UUID.fromString(addressId);

        AddressEntity address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        return AddressResponse.from(address);
    }
}
