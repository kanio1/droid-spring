package com.droid.bss.application.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Generic paginated response wrapper
 * @param <T> the type of content items
 */
@Schema(
    name = "PageResponse",
    description = "Paginated response wrapper with content and pagination metadata"
)
public record PageResponse<T>(
    @Schema(
        description = "List of content items for the current page",
        implementation = Object.class
    )
    List<T> content,

    @Schema(
        description = "Current page number (0-based)",
        example = "0",
        minimum = "0"
    )
    int page,

    @Schema(
        description = "Number of items per page",
        example = "20",
        minimum = "1"
    )
    int size,

    @Schema(
        description = "Total number of elements across all pages",
        example = "125",
        minimum = "0"
    )
    long totalElements,

    @Schema(
        description = "Total number of pages",
        example = "7",
        minimum = "0"
    )
    int totalPages,

    @Schema(
        description = "Whether this is the first page",
        example = "true"
    )
    boolean first,

    @Schema(
        description = "Whether this is the last page",
        example = "false"
    )
    boolean last,

    @Schema(
        description = "Whether the content is empty",
        example = "false"
    )
    boolean empty
) {
    
    public static <T> PageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = totalPages == 0 || page == totalPages - 1;
        boolean empty = content.isEmpty();

        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last, empty);
    }
}
