package org.module.publish_service.util;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/31/2026 5:02 PM
 */
@RequiredArgsConstructor
public class SpecificationUtil<T> {

    private CriteriaBuilder builder;
    private CriteriaQuery<?> query;
    private Root<T> root;

    public SpecificationUtil(Root<T> root, CriteriaQuery<?> cq, CriteriaBuilder bc) {
        this.builder = bc;
        this.query = cq;
        this.root = root;
    }

    public Predicate matchText(String field, String value) {
        return builder.equal(builder.lower(root.get(field)), value.toLowerCase());
    }

    public Predicate matchNumber(String field, String value) {
        return builder.equal(root.get(field), value);
    }

    public Predicate search(String field, String value) {
        return builder.like(builder.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    public Predicate concatSearch(String first, String last, String value) {
        return builder.like(builder.lower(merge(first, last)), "%" + value.toLowerCase() + "%");
    }

    private Expression<String> merge(String first, String last) {
        return builder.concat(builder.concat(root.get(first), " "), root.get(last));
    }

}
