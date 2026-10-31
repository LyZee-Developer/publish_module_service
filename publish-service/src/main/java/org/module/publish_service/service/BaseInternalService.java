package org.module.publish_service.service;

import org.module.publish_service.model.BaseEntity;
import org.module.publish_service.request.BaseInternalRequestFilter;
import org.module.publish_service.specification.BaseCombineRepositorySpecification;
import org.module.publish_service.specification.BaseInternalSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:32 PM
 */
public interface BaseInternalService<
    ENTITY extends BaseEntity,
    ENTITY_TYPE extends Serializable,
    FILTER extends BaseInternalRequestFilter> {

    Optional<ENTITY> getOne(FILTER filter);

    List<ENTITY> getAll(FILTER filter);

    <T> List<T> getAllOrView(FILTER filter, Class<T> clazz);

    <T> T getOneOrView(ENTITY_TYPE id, Class<T> clazz);

    <T> Page<T> getAllOrView(FILTER filter, Pageable pageable, Class<T> clazz);
}
