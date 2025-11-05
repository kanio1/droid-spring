package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.application.dto.address.CreateAddressCommand;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for creating a new address
 */
@Service
public class CreateAddressUseCase {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public CreateAddressUseCase(AddressRepository addressRepository, CustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AddressResponse handle(CreateAddressCommand command) {
        // Validate customer exists
        CustomerId customerId = CustomerId.of(command.customerId());
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + command.customerId()));

        // Parse enums
        AddressType type = AddressType.valueOf(command.type());
        Country country = Country.valueOf(command.country());

        // Check if customer already has a primary address for this type
        if (command.isPrimary()) {
            addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                    customerId.value(), type)
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Customer already has a primary " + type.getDescription().toLowerCase() + " address"
                        );
                    });
        }

        // Create address
        AddressEntity address = new AddressEntity(
                customer,
                type,
                AddressStatus.ACTIVE,
                command.street(),
                command.postalCode(),
                command.city(),
                country
        );

        // Set optional fields
        if (command.getHouseNumber().isPresent()) {
            address.setHouseNumber(command.getHouseNumber().get());
        }
        if (command.getApartmentNumber().isPresent()) {
            address.setApartmentNumber(command.getApartmentNumber().get());
        }
        if (command.getRegion().isPresent()) {
            address.setRegion(command.getRegion().get());
        }
        if (command.getLatitude().isPresent()) {
            address.setLatitude(command.getLatitude().get());
        }
        if (command.getLongitude().isPresent()) {
            address.setLongitude(command.getLongitude().get());
        }
        address.setIsPrimary(command.isPrimary());

        AddressEntity saved = addressRepository.save(address);

        return AddressResponse.from(saved);
    }
}
