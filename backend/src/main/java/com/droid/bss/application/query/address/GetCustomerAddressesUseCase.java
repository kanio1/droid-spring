package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.Address;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all addresses for a customer
 */
@Service
public class GetCustomerAddressesUseCase {

    private final AddressRepository addressRepository;

    public GetCustomerAddressesUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressResponse> handle(String customerId) {
        java.util.UUID id = java.util.UUID.fromString(customerId);

        List<Address> addresses = addressRepository.findByCustomerIdAndDeletedAtIsNull(id);

        return addresses.stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }
}
