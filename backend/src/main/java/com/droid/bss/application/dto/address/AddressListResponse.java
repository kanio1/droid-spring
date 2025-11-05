package com.droid.bss.application.dto.address;

import java.util.List;

/**
 * Response DTO for paginated address list
 */
public record AddressListResponse(
        List<AddressResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty
) {}
