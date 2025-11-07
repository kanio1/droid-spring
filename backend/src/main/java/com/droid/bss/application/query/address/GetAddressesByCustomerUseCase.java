package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.AddressListResponse;
import com.droid.bss.domain.address.AddressRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub class for GetAddressesByCustomerUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GetAddressesByCustomerUseCase {

    private final AddressRepository addressRepository;

    public GetAddressesByCustomerUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressListResponse> handle(GetAddressesQuery query) {
        // Stub implementation
        System.out.println("Getting addresses for customer: " + query.customerId());
        return List.of();
    }
}
