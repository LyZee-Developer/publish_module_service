package org.module.publish_service.specification;

import org.module.publish_service.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:32 PM
 */
@NoRepositoryBean
public interface BaseCombineRepositorySpecification<ENTITY extends BaseEntity, ENTITY_TYPE
        extends Serializable>
        extends JpaRepository<ENTITY, ENTITY_TYPE>,
        JpaSpecificationExecutor<ENTITY> {
}
