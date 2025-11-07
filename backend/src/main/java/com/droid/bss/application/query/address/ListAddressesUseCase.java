package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.AddressListResponse;
import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for listing addresses with pagination and filtering
 */
@Service
public class ListAddressesUseCase {

    private final AddressRepository addressRepository;

    public ListAddressesUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressListResponse handle(
            String customerId,
            String type,
            String status,
            String country,
            String searchTerm,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);

        List<Address> addresses;

        // Build query based on filters
        if (customerId != null && !customerId.isEmpty()) {
            UUID customerUUID = UUID.fromString(customerId);
            if (type != null && !type.isEmpty()) {
                AddressType typeEnum = AddressType.valueOf(type);
                addresses = addressRepository.findByCustomerIdAndTypeAndDeletedAtIsNull(
                        customerUUID, typeEnum, pageable
                );
            } else if (status != null && !status.isEmpty()) {
                AddressStatus statusEnum = AddressStatus.valueOf(status);
                addresses = addressRepository.findByCustomerIdAndStatusAndDeletedAtIsNull(
                        customerUUID, statusEnum, pageable
                );
            } else {
                addresses = addressRepository.findByCustomerIdAndDeletedAtIsNull(customerUUID);
            }
        } else if (searchTerm != null && !searchTerm.isEmpty()) {
            addresses = addressRepository.searchByTerm(searchTerm);
        } else if (type != null && !type.isEmpty()) {
            AddressType typeEnum = AddressType.valueOf(type);
            addresses = addressRepository.findByTypeAndStatusAndDeletedAtIsNull(
                    typeEnum, AddressStatus.ACTIVE, pageable
            );
        } else if (country != null && !country.isEmpty()) {
            Country countryEnum = Country.valueOf(country);
            addresses = addressRepository.findByCountryAndDeletedAtIsNull(countryEnum);
        } else {
            // No filters, return all non-deleted addresses
            addresses = addressRepository.findAll(pageable);
        }

        // Convert to response DTOs
        List<AddressResponse> responses = addresses.stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());

        return new AddressListResponse(
                responses,
                page,
                size,
                (long) addresses.size(),
                (int) Math.ceil((double) addresses.size() / size),
                page == 0,
                page >= (addresses.size() / size),
                addresses.size(),
                addresses.isEmpty()
        );
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);
                return PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
            }
        }
        return PageRequest.of(page, size);
    }

    private Page<AddressEntity> createPageFromList(List<AddressEntity> addresses, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), addresses.size());

        if (start >= addresses.size()) {
            return new PageImpl<>(List.of(), pageable, addresses.size());
        }

        List<AddressEntity> pageContent = addresses.subList(start, end);
        return new PageImpl<>(pageContent, pageable, addresses.size());
    }
}
