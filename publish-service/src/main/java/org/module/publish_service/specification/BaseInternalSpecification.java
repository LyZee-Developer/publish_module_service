package org.module.publish_service.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.module.publish_service.model.BaseActivateEntity_;
import org.module.publish_service.model.BaseEntity;
import org.module.publish_service.request.BaseInternalRequestFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:32 PM
 */

@Setter
@Getter
public class BaseInternalSpecification<ENTITY extends BaseEntity, FT extends BaseInternalRequestFilter> implements Specification<ENTITY> {

    protected FT queryFilters;
    protected List<Predicate> predicates = new ArrayList<>();

    public BaseInternalSpecification(FT filter) {
        this.queryFilters = filter;
    }

    @Override
    public final @Nullable Predicate toPredicate(Root<ENTITY> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        predicates.clear();
        //default filter: active only unless the request says otherwise
        Boolean isActivate = queryFilters != null && queryFilters.getIsActivate() != null
                ? queryFilters.getIsActivate()
                : Boolean.TRUE;
        predicates.add(cb.equal(root.get(BaseActivateEntity_.IS_ACTIVATE), isActivate));

        //module-specific filters
        addPredicates(root, query, cb);

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Override to add filters for a specific entity; the default filter is already applied.
     */
    protected void addPredicates(Root<ENTITY> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
    }
}
