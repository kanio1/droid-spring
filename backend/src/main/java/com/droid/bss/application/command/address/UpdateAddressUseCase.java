package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.application.dto.address.UpdateAddressCommand;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for updating an existing address
 */
@Service
public class UpdateAddressUseCase {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public UpdateAddressUseCase(AddressRepository addressRepository, CustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AddressResponse handle(UpdateAddressCommand command) {
        UUID addressIdUuid = UUID.fromString(command.id());
        AddressId addressId = AddressId.of(addressIdUuid);

        // Find existing address
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + command.id()));

        // Check version for optimistic locking
        if (address.getVersion() != command.version()) {
            throw new RuntimeException("Address has been modified by another user");
        }

        // Validate customer exists
        CustomerId customerId = CustomerId.of(command.customerId());
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + command.customerId()));

        // Parse enums
        AddressType type = AddressType.valueOf(command.type());
        Country country = Country.valueOf(command.country());

        // Check if primary address constraint would be violated
        if (command.isPrimary()) {
            addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                    customerId.value(), type)
                    .filter(existing -> !existing.getId().equals(addressId))
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Customer already has a primary " + type.getDescription().toLowerCase() + " address"
                        );
                    });
        }

        // Update address using immutable pattern
        Address updated = address.updateAddress(
                command.street(),
                command.getHouseNumber().orElse(null),
                command.getApartmentNumber().orElse(null),
                command.postalCode(),
                command.city(),
                command.getRegion().orElse(null),
                country,
                command.getLatitude().orElse(null),
                command.getLongitude().orElse(null),
                command.getNotes().orElse(null)
        );

        // Save and return
        Address saved = addressRepository.save(updated);

        return AddressResponse.from(saved);
    }
}
