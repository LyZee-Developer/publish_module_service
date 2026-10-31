package org.module.publish_service.service.impl;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.hibernate.Hibernate;
import org.module.publish_service.exception.ConflictException;
import org.module.publish_service.exception.ResourceNotFoundException;
import org.module.publish_service.model.BaseActivateEntity;
import org.module.publish_service.model.BaseEntity;
import org.module.publish_service.request.BaseInternalRequestFilter;
import org.module.publish_service.service.BaseInternalService;
import org.module.publish_service.specification.BaseCombineRepositorySpecification;
import org.module.publish_service.specification.BaseInternalSpecification;
import org.module.publish_service.util.EntityGraphUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:32 PM
 */
public abstract class BaseInternalServiceImpl<ENTITY extends BaseEntity, ENTITY_TYPE extends Serializable, REPO extends BaseCombineRepositorySpecification<ENTITY, ENTITY_TYPE>, FILTER extends BaseInternalRequestFilter, SPECI extends BaseInternalSpecification<ENTITY, FILTER>> implements BaseInternalService<ENTITY, ENTITY_TYPE, FILTER> {

    @Autowired
    protected REPO repository;

    public abstract String currentUser();

    private final Class<ENTITY> entityClass;

    private final Constructor<FILTER> filterConstructor;

    private final Constructor<SPECI> specificationConstructor;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resolves FILTER and SPECI from the subclass's generic arguments and looks up
     * FILTER's no-arg and SPECI's (FILTER) constructors once, so subclasses don't need to build either.
     */
    @SuppressWarnings("unchecked")
    protected BaseInternalServiceImpl() {
        Class<?>[] types = GenericTypeResolver.resolveTypeArguments(getClass(), BaseInternalServiceImpl.class);
        if (types == null || types[0] == null || types[3] == null || types[4] == null) {
            throw new IllegalStateException(getClass().getName() + " must declare concrete ENTITY, FILTER and SPECI types");
        }
        this.entityClass = (Class<ENTITY>) types[0];
        Class<FILTER> filterType = (Class<FILTER>) types[3];
        Class<SPECI> specificationType = (Class<SPECI>) types[4];

        try {
            this.filterConstructor = filterType.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(filterType.getName() + " needs a public no-arg constructor", ex);
        }

        try {
            this.specificationConstructor = specificationType.getConstructor(filterType);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(specificationType.getName() + " needs a public constructor (" + filterType.getSimpleName() + " filter)", ex);
        }
    }


    public Page<ENTITY> findAllSpePageFetch(FILTER filter, Pageable pageable) {
        return findAll(filter, pageable);
    }

    public List<ENTITY> findAllSpecFetch(FILTER filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ENTITY> cq = cb.createQuery(entityClass);

        Root<ENTITY> root = cq.from(entityClass);

        /*
         * Specification / WHERE
         */
        Predicate predicate = this.getSpecification(filter).toPredicate(root, cq, cb);

        if (predicate != null) {
            cq.where(predicate);
        }

        /*
         * DISTINCT prevents duplicate root entities when @OneToMany is fetched.
         * Unknown paths are dropped, so they never break the query.
         */
        List<String> fetchPaths = EntityGraphUtils.validPaths(entityManager, entityClass, filter.getFetches());

        if (!fetchPaths.isEmpty()) {
            cq.distinct(true);
        }

        TypedQuery<ENTITY> query = entityManager.createQuery(cq);

        if (!fetchPaths.isEmpty()) {
            EntityGraph<ENTITY> graph = EntityGraphUtils.build(entityManager, entityClass, fetchPaths);
            query.setHint("jakarta.persistence.fetchgraph", graph);
        }

        /*
         * No fetches   -> associations hold only their ids
         * With fetches -> fetched associations are loaded, the others are null
         */
        return EntityGraphUtils.shape(entityManager, entityClass, query.getResultList(), filter.getFetches());
    }

    public List<ENTITY> findAll(List<String> fetchPaths) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ENTITY> cq = cb.createQuery(entityClass);
        cq.from(entityClass);

        TypedQuery<ENTITY> query = entityManager.createQuery(cq);

        if (fetchPaths != null && !fetchPaths.isEmpty()) {
            EntityGraph<ENTITY> graph = EntityGraphUtils.build(entityManager, entityClass, fetchPaths);
            query.setHint("jakarta.persistence.fetchgraph", graph);
        }

        return EntityGraphUtils.shape(entityManager, entityClass, query.getResultList(), fetchPaths);
    }

    /**
     * A filter with default values (e.g. isActivate = true, no search).
     */
    protected FILTER getFilter() {
        return BeanUtils.instantiateClass(filterConstructor);
    }

    /**
     * Override only when the specification can't be built from its (FILTER) constructor.
     */
    protected SPECI getSpecification(FILTER filter) {
        return BeanUtils.instantiateClass(specificationConstructor, filter);
    }

    public ENTITY save(ENTITY entity) {
        try {
            if (entity instanceof BaseActivateEntity) {
                ((BaseActivateEntity) entity).setIsActivate(Boolean.TRUE);
            }
            if (entity != null) {
                entity.setCreatedAt(LocalDateTime.now());
                entity.setCreatedBy(currentUser());
            }
            return this.repository.save(entity);
        } catch (Exception ex) {
            throw new ConflictException(ex.getCause().getMessage());
        }
    }

    public List<ENTITY> saveAll(List<ENTITY> entities) {
        try {
            if (!entities.isEmpty()) {
                for (ENTITY entity : entities) {
                    if (entity instanceof BaseActivateEntity) {
                        ((BaseActivateEntity) entity).setIsActivate(Boolean.TRUE);
                        entity.setCreatedBy(currentUser());
                        entity.setCreatedAt(LocalDateTime.now());
                    }
                }
            }
            return repository.saveAll(entities);
        } catch (Exception ex) {
            throw new ConflictException(ex.getCause().getMessage());
        }
    }

    public List<ENTITY> update(List<ENTITY> entities) {
        try {
            if (!entities.isEmpty()) {
                for (ENTITY entity : entities) {
                    if (entity instanceof BaseActivateEntity) {
                        ((BaseActivateEntity) entity).setIsActivate(((BaseActivateEntity) entity).getIsActivate());
                        entity.setModifiedBy(currentUser());
                        entity.setModifiedAt(LocalDateTime.now());
                    }
                }
            }
            return repository.saveAll(entities);
        } catch (Exception ex) {
            throw new ConflictException(ex.getCause().getMessage());
        }
    }

    public Page<ENTITY> findAll(Pageable pageable) {
        return findAll(getFilter(), pageable);
    }

    /**
     * Maps every entity to viewDTO through its public (ENTITY) constructor, e.g. UserDTO.view(User user).
     */
    public <T> List<T> findAllView(Class<T> viewDTO) {
        Constructor<T> constructor = getViewConstructor(viewDTO);
        return findAllSpecViewDTO(entity -> BeanUtils.instantiateClass(constructor, entity));
    }

    public ENTITY update(ENTITY entity) {
        try {
            if (entity instanceof BaseActivateEntity) {
                Boolean modifyActivate = ((BaseActivateEntity) entity).getIsActivate();
                ((BaseActivateEntity) entity).setIsActivate(modifyActivate);
            }
            if (entity != null) {
                entity.setModifiedAt(LocalDateTime.now());
                entity.setModifiedBy(currentUser());
            }
            assert entity != null;
            return this.repository.save(entity);
        } catch (Exception ex) {
            throw new ConflictException(ex.getCause().getMessage());
        }
    }

    public List<ENTITY> findAllSpec() {
        return findAll(getFilter());
    }

    public Optional<ENTITY> findOne(ENTITY_TYPE id) {
        return Optional.ofNullable(id).flatMap(this.repository::findById);
    }

    public ENTITY findOneOrThrow(ENTITY_TYPE id) {
        if (id == null) {
            throw new ResourceNotFoundException("ID cannot be null");
        }
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));
    }

    //  *** private access
    private <T> Constructor<T> getViewConstructor(Class<T> viewDTO) {
        try {
            return viewDTO.getConstructor(entityClass);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(viewDTO.getName() + " needs a public constructor (" + entityClass.getSimpleName() + " entity)", ex);
        }
    }

    private <T> List<T> findAllSpecViewDTO(Function<? super ENTITY, ? extends T> mapper) {
        List<ENTITY> entities = this.findAllSpec();
        if (entities.isEmpty()) return Collections.emptyList();
        return entities.stream().<T>map(mapper).toList();
    }

    public List<ENTITY> findAll(FILTER filter) {
        return repository.findAll(getSpecification(filter));
    }

    /**
     * Paged search that honours filter.fetches:
     * - no fetches   : one paged query, associations hold only their ids
     * - with fetches : page the root ids first, then load just those rows with the fetch graph,
     * so @OneToMany fetches never force Hibernate into in-memory pagination
     */
    public Page<ENTITY> findAll(FILTER filter, Pageable pageable) {
        if (filter == null) filter = getFilter();
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(findAllSpecFetch(filter));
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Specification<ENTITY> specification = getSpecification(filter);
        List<String> fetchPaths = EntityGraphUtils.validPaths(entityManager, entityClass, filter.getFetches());

        long total = count(cb, specification);
        if (total == 0 || pageable.getOffset() >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        List<ENTITY> content = fetchPaths.isEmpty()
            ? findPage(cb, specification, pageable)
            : findPageWithGraph(cb, specification, pageable, fetchPaths);

        /*
         * Self-referencing rows (e.g. an Employee that is another one's lineManager) can come back as
         * Hibernate proxies: shape the real entity, otherwise its lazy collections stay untouched and fail
         * with "Cannot lazily initialize collection ... (no session)" during JSON serialization.
         */
        content = new ArrayList<>(content);
        content.replaceAll(entity -> (ENTITY) Hibernate.unproxy(entity));

        EntityGraphUtils.shape(entityManager, entityClass, content, filter.getFetches());
        return new PageImpl<>(content, pageable, total);
    }

    private List<ENTITY> findPage(CriteriaBuilder cb, Specification<ENTITY> specification, Pageable pageable) {
        CriteriaQuery<ENTITY> cq = cb.createQuery(entityClass);
        Root<ENTITY> root = cq.from(entityClass);
        applyWhere(cb, cq, root, specification);
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        return entityManager.createQuery(cq)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    }

    private List<ENTITY> findPageWithGraph(CriteriaBuilder cb, Specification<ENTITY> specification,
                                           Pageable pageable, List<String> fetchPaths) {
        String idName = EntityGraphUtils.idAttribute(entityManager, entityClass).getName();

        // 1. ids of the requested page (filter + sort + limit, no joins from the graph)
        CriteriaQuery<Object> idQuery = cb.createQuery(Object.class);
        Root<ENTITY> idRoot = idQuery.from(entityClass);
        idQuery.select(idRoot.get(idName));
        applyWhere(cb, idQuery, idRoot, specification);
        idQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), idRoot, cb));

        List<Object> ids = entityManager.createQuery(idQuery)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultList();
        if (ids.isEmpty()) return new ArrayList<>();

        // 2. load those rows with the fetch graph
        CriteriaQuery<ENTITY> cq = cb.createQuery(entityClass);
        Root<ENTITY> root = cq.from(entityClass);
        cq.where(root.get(idName).in(ids));
        cq.distinct(true);

        TypedQuery<ENTITY> query = entityManager.createQuery(cq);
        query.setHint("jakarta.persistence.fetchgraph", EntityGraphUtils.build(entityManager, entityClass, fetchPaths));

        // 3. restore the page order from step 1
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        Map<Object, ENTITY> byId = new HashMap<>();
        query.getResultList().forEach(entity -> byId.putIfAbsent(unitUtil.getIdentifier(entity), entity));

        List<ENTITY> ordered = new ArrayList<>(ids.size());
        for (Object id : ids) {
            ENTITY entity = byId.get(id);
            if (entity != null) ordered.add(entity);
        }
        return ordered;
    }

    private long count(CriteriaBuilder cb, Specification<ENTITY> specification) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ENTITY> countRoot = countQuery.from(entityClass);
        applyWhere(cb, countQuery, countRoot, specification);
        countQuery.select(countQuery.isDistinct() ? cb.countDistinct(countRoot) : cb.count(countRoot));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private void applyWhere(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<ENTITY> root, Specification<ENTITY> specification) {
        if (specification == null) return;
        Predicate predicate = specification.toPredicate(root, cq, cb);
        if (predicate != null) cq.where(predicate);
    }

    @Override
    public Optional<ENTITY> getOne(FILTER filter) {
        return repository.findOne(getSpecification(filter));
    }

    @Override
    public List<ENTITY> getAll(FILTER filter) {
        return this.findAllSpecFetch(filter);
    }

    @Override
    public <T> Page<T> getAllOrView(FILTER filter, Pageable pageable, Class<T> clazz) {
        Page<ENTITY> all = this.findAll(filter, pageable);
        if (clazz.isAssignableFrom(entityClass)) {
            return all.map(clazz::cast);
        }
        Constructor<T> constructor = getViewConstructor(clazz);
        return all.map(entity -> BeanUtils.instantiateClass(constructor, entity));
    }

    @Override
    public <T> List<T> getAllOrView(FILTER filter, Class<T> clazz) {
        List<ENTITY> all = this.findAllSpecFetch(filter);
        if (all.isEmpty()) return Collections.emptyList();

        /*
         * clazz is the entity itself (or a supertype) -> return as-is
         * otherwise -> map through clazz's public (ENTITY) constructor, e.g. UserDTO(User user)
         */
        if (clazz.isAssignableFrom(entityClass)) {
            return all.stream().map(clazz::cast).toList();
        }
        Constructor<T> constructor = getViewConstructor(clazz);
        return all.stream().map(entity -> BeanUtils.instantiateClass(constructor, entity)).toList();
    }

    @Override
    public <T> T getOneOrView(ENTITY_TYPE id, Class<T> clazz) {
        ENTITY one = this.findOneOrThrow(id);
        Constructor<T> constructor = getViewConstructor(clazz);
        return BeanUtils.instantiateClass(constructor, one);
    }
}
