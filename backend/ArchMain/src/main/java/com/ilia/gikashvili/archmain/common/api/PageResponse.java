package com.ilia.gikashvili.archmain.common.api;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 *
 * @param <T> the type of items in the page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
