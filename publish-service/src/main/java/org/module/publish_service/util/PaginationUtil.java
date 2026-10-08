package org.module.publish_service.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.awt.print.Pageable;
import java.util.Set;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/8/2026 10:34 PM
 */
public final class PaginationUtil {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationUtil() {
    }

    public static Pageable of(Integer page, Integer size) {
        return (Pageable) PageRequest.of(safePage(page) - 1, safeSize(size));
    }

    public static Pageable of(Integer page, Integer size, String sortBy, String direction,
                              Set<String> allowedSortFields, String defaultSortField) {
        String field = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : defaultSortField;
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return (Pageable) PageRequest.of(safePage(page) - 1, safeSize(size), Sort.by(dir, field));
    }

    private static int safePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    private static int safeSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
