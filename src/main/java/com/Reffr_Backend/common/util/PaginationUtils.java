package com.Reffr_Backend.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    private static final int MAX_SIZE = 50;
    private static final int MIN_SIZE = 1;

    public static Pageable of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);

        return PageRequest.of(safePage, safeSize, sort);
    }
}
