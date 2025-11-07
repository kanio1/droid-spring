package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.Address;
import com.droid.bss.domain.address.AddressId;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Service;

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
        AddressId id = new AddressId(java.util.UUID.fromString(addressId));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        return AddressResponse.from(address);
    }
}
