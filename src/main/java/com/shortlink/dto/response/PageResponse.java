package com.shortlink.dto.response;

import java.util.List;

// Generic immutable record for paginated API responses.
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            page == 0,
            page >= totalPages - 1
        );
    }
}
