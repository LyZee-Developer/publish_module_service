package org.module.publish_service.service.impl;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:32 PM
 */
public abstract class BaseInternalServiceImpl<
    ENTITY extends BaseActivateEntity,
    ENTITY_TYPE extends Serializable,
    REPO extends BaseCombineRepositorySpecification<ENTITY, ENTITY_TYPE>,
    FILTER extends BaseInternalRequestFilter,
    SPECI extends BaseInternalSpecification<ENTITY, FILTER>>
    implements BaseInternalService<ENTITY, ENTITY_TYPE, FILTER> {

    /**
     * JPA hint name used to tell Hibernate which associations to load.
     */
    private static final String FETCH_GRAPH_HINT = "jakarta.persistence.fetchgraph";

    // =====================================================================
    // Fields
    // =====================================================================

    /**
     * Repository of the subclass, e.g. EmployeeRepository.
     */
    @Autowired
    protected REPO repository;

    /**
     * Used to build our own criteria queries (needed for "fetches").
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * The entity class, e.g. Employee.class.
     */
    private final Class<ENTITY> entityClass;

    /**
     * FILTER() constructor, used to create a default filter.
     */
    private final Constructor<FILTER> filterConstructor;

    /**
     * SPECI(FILTER) constructor, used to create the specification.
     */
    private final Constructor<SPECI> specificationConstructor;

    /**
     * The user written into createdBy / modifiedBy. Every subclass must implement it.
     */
    public abstract String currentUser();

    // =====================================================================
    // Constructor
    // =====================================================================

    /**
     * Reads the generic types written on the subclass (e.g. Employee, EmployeeRequestFilter, ...)
     * and looks up the constructors we need, only once at start-up.
     * If something is missing the application fails at start-up with a clear message.
     */
    @SuppressWarnings("unchecked")
    protected BaseInternalServiceImpl() {
        // index: 0 = ENTITY, 1 = ENTITY_TYPE, 2 = REPO, 3 = FILTER, 4 = SPECI
        Class<?>[] types = GenericTypeResolver.resolveTypeArguments(getClass(), BaseInternalServiceImpl.class);
        if (types == null || types[0] == null || types[3] == null || types[4] == null) {
            throw new IllegalStateException(getClass().getName() + " must declare concrete ENTITY, FILTER and SPECI types");
        }

        this.entityClass = (Class<ENTITY>) types[0];
        Class<FILTER> filterType = (Class<FILTER>) types[3];
        Class<SPECI> specificationType = (Class<SPECI>) types[4];

        // FILTER must have: public FILTER()
        try {
            this.filterConstructor = filterType.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(filterType.getName() + " needs a public no-arg constructor", ex);
        }

        // SPECI must have: public SPECI(FILTER filter)
        try {
            this.specificationConstructor = specificationType.getConstructor(filterType);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(specificationType.getName() + " needs a public constructor (" + filterType.getSimpleName() + " filter)", ex);
        }
    }

    // =====================================================================
    // Helpers that a subclass may override
    // =====================================================================

    /**
     * Creates a filter with default values (e.g. isActivate = true, no search).
     */
    protected FILTER getFilter() {
        return BeanUtils.instantiateClass(filterConstructor);
    }

    /**
     * Creates the specification (WHERE clause) for the filter.
     * Override only when the specification can't be built from its (FILTER) constructor.
     */
    protected SPECI getSpecification(FILTER filter) {
        return BeanUtils.instantiateClass(specificationConstructor, filter);
    }

    // =====================================================================
    // Create / Update
    // =====================================================================

    /**
     * Wraps a save failure in a ConflictException, keeping the original exception
     * as the cause so the full stack trace is still logged.
     */
    private ConflictException toConflict(Exception ex) {
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        return new ConflictException(root.getMessage(), ex);
    }

    /**
     * Saves a new entity: active = true, createdAt = now, createdBy = current user.
     */
    public ENTITY save(ENTITY entity) {
        try {
            if (entity instanceof BaseActivateEntity activateEntity) {
                activateEntity.setIsActivate(Boolean.TRUE);
            }
            if (entity != null) {
                entity.setCreatedAt(LocalDateTime.now());
                entity.setCreatedBy(currentUser());
            }
            return repository.save(entity);
        } catch (Exception ex) {
            throw toConflict(ex);
        }
    }

    /**
     * Saves many new entities. Audit fields are set only for BaseActivateEntity entities.
     */
    public List<ENTITY> saveAll(List<ENTITY> entities) {
        try {
            for (ENTITY entity : entities) {
                if (entity instanceof BaseActivateEntity activateEntity) {
                    activateEntity.setIsActivate(Boolean.TRUE);
                    entity.setCreatedBy(currentUser());
                    entity.setCreatedAt(LocalDateTime.now());
                }
            }
            return repository.saveAll(entities);
        } catch (Exception ex) {
            throw toConflict(ex);
        }
    }

    /**
     * Updates an entity: modifiedAt = now, modifiedBy = current user.
     * The isActivate value sent by the caller is kept as it is.
     */
    public ENTITY update(ENTITY entity) {
        try {
            if (entity != null) {
                entity.setModifiedAt(LocalDateTime.now());
                entity.setModifiedBy(currentUser());
            }
            assert entity != null;
            return repository.save(entity);
        } catch (Exception ex) {
            throw toConflict(ex);
        }
    }

    /**
     * Updates many entities. Audit fields are set only for BaseActivateEntity entities.
     * The isActivate value sent by the caller is kept as it is.
     */
    public List<ENTITY> update(List<ENTITY> entities) {
        try {
            for (ENTITY entity : entities) {
                if (entity != null) {
                    entity.setModifiedBy(currentUser());
                    entity.setModifiedAt(LocalDateTime.now());
                }
            }
            return repository.saveAll(entities);
        } catch (Exception ex) {
            throw toConflict(ex);
        }
    }

    // =====================================================================
    // Find one
    // =====================================================================

    /**
     * Finds by id. Returns an empty Optional when the id is null or not found.
     */
    public Optional<ENTITY> findOne(ENTITY_TYPE id) {
        return Optional.ofNullable(id).flatMap(repository::findById);
    }

    @Override
    public Optional<ENTITY> getOne(ENTITY_TYPE id) {
        return Optional.ofNullable(id).flatMap(repository::findById);
    }

    /**
     * Finds by id, or throws ResourceNotFoundException when the id is null or not found.
     */
    public ENTITY findOneOrThrow(ENTITY_TYPE id) {
        if (id == null) {
            throw new ResourceNotFoundException("ID cannot be null");
        }
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Data not found with id: " + id));
    }

    @Override
    public Optional<ENTITY> getOne(FILTER filter) {
        return repository.findOne(getSpecification(filter));
    }

    /**
     * Finds by id and converts it to the view class, e.g. new EmployeeViewDTO(employee).
     */
    @Override
    public <T> T getOneOrView(ENTITY_TYPE id, Class<T> clazz) {
        ENTITY entity = findOneOrThrow(id);
        return toView(entity, getViewConstructor(clazz));
    }

    // =====================================================================
    // Find all - simple (no "fetches" support)
    // =====================================================================

    /**
     * All rows that match the filter, through the repository.
     */
    public List<ENTITY> findAll(FILTER filter) {
        return repository.findAll(getSpecification(filter));
    }

    /**
     * All rows that match the default filter.
     */
    public List<ENTITY> findAllSpec() {
        return findAll(getFilter());
    }

    /**
     * All rows (default filter) converted to the view class, e.g. UserDTO(User user).
     */
    public <T> List<T> findAllView(Class<T> viewDTO) {
        Constructor<T> constructor = getViewConstructor(viewDTO);
        List<ENTITY> entities = findAllSpec();
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(entity -> toView(entity, constructor)).toList();
    }

    // =====================================================================
    // Find all - with "fetches" support
    // =====================================================================

    /**
     * All rows that match the filter, without paging.
     * Associations listed in filter.fetches are loaded, the others hold only their ids.
     */
    public List<ENTITY> findAllSpecFetch(FILTER filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ENTITY> query = cb.createQuery(entityClass);
        Root<ENTITY> root = query.from(entityClass);

        // WHERE ...
        applyWhere(cb, query, root, getSpecification(filter));

        // Keep only fetch names that really exist on the entity
        List<String> fetchPaths = EntityGraphUtils.validPaths(entityManager, entityClass, filter.getFetches());

        // DISTINCT: loading a @OneToMany would otherwise repeat the parent once per child
        if (!fetchPaths.isEmpty()) {
            query.distinct(true);
        }

        TypedQuery<ENTITY> typedQuery = entityManager.createQuery(query);
        applyFetchGraph(typedQuery, fetchPaths);

        // fetched associations -> loaded, the others -> id only
        return EntityGraphUtils.shape(entityManager, entityClass, typedQuery.getResultList(), filter.getFetches());
    }

    /**
     * All rows (no filter) with the given associations loaded, the others hold only their ids.
     */
    public List<ENTITY> findAll(List<String> fetchPaths) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ENTITY> query = cb.createQuery(entityClass);
        query.from(entityClass);

        TypedQuery<ENTITY> typedQuery = entityManager.createQuery(query);
        applyFetchGraph(typedQuery, fetchPaths);

        return EntityGraphUtils.shape(entityManager, entityClass, typedQuery.getResultList(), fetchPaths);
    }

    /**
     * Paged search with the default filter.
     */
    public Page<ENTITY> findAll(Pageable pageable) {
        return findAll(getFilter(), pageable);
    }

    /**
     * Old name, kept so existing callers still work. Same as {@link #findAll(BaseInternalRequestFilter, Pageable)}.
     */
    public Page<ENTITY> findAllSpePageFetch(FILTER filter, Pageable pageable) {
        return findAll(filter, pageable);
    }

    /**
     * Paged search that supports filter.fetches.
     * <ol>
     *     <li>count the rows that match the filter</li>
     *     <li>load the rows of the requested page</li>
     *     <li>shape the associations (loaded or id only)</li>
     * </ol>
     */
    public Page<ENTITY> findAll(FILTER filter, Pageable pageable) {
        if (filter == null) {
            filter = getFilter();
        }

        // No paging asked -> return everything as a single page
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(findAllSpecFetch(filter));
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Specification<ENTITY> specification = getSpecification(filter);
        List<String> fetchPaths = EntityGraphUtils.validPaths(entityManager, entityClass, filter.getFetches());

        // 1. Count. Nothing to show -> return an empty page without loading rows
        long total = count(cb, specification);
        if (total == 0 || pageable.getOffset() >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        // 2. Load the rows of this page
        List<ENTITY> content;
        if (fetchPaths.isEmpty()) {
            content = findPage(cb, specification, pageable);
        } else {
            content = findPageWithFetches(cb, specification, pageable, fetchPaths);
        }

        // A self-referencing row (e.g. an Employee that is another one's lineManager) can be a Hibernate proxy.
        // Replace it with the real entity, otherwise shape() changes the proxy and the real entity keeps
        // its lazy list, which fails with "Cannot lazily initialize collection ... (no session)".
        content = unproxyAll(content);

        // 3. fetched associations -> loaded, the others -> id only
        EntityGraphUtils.shape(entityManager, entityClass, content, filter.getFetches());

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<ENTITY> getAll(FILTER filter) {
        return findAllSpecFetch(filter);
    }

    /**
     * Paged search converted to the view class.
     * If clazz is the entity itself, the entities are returned as they are.
     */
    @Override
    public <T> Page<T> getAllOrView(FILTER filter, Pageable pageable, Class<T> clazz) {
        Page<ENTITY> page = findAll(filter, pageable);

        if (clazz.isAssignableFrom(entityClass)) {
            return page.map(clazz::cast);
        }

        Constructor<T> constructor = getViewConstructor(clazz);
        return page.map(entity -> toView(entity, constructor));
    }

    /**
     * Search without paging converted to the view class.
     * If clazz is the entity itself, the entities are returned as they are.
     */
    @Override
    public <T> List<T> getAllOrView(FILTER filter, Class<T> clazz) {
        List<ENTITY> entities = findAllSpecFetch(filter);
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        if (clazz.isAssignableFrom(entityClass)) {
            return entities.stream().map(clazz::cast).toList();
        }

        Constructor<T> constructor = getViewConstructor(clazz);
        return entities.stream().map(entity -> toView(entity, constructor)).toList();
    }

    // =====================================================================
    // Private query helpers
    // =====================================================================

    /**
     * Page without fetches: one query with WHERE + ORDER BY + LIMIT/OFFSET.
     */
    private List<ENTITY> findPage(CriteriaBuilder cb, Specification<ENTITY> specification, Pageable pageable) {
        CriteriaQuery<ENTITY> query = cb.createQuery(entityClass);
        Root<ENTITY> root = query.from(entityClass);
        applyWhere(cb, query, root, specification);
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        return entityManager.createQuery(query)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    }

    /**
     * Page with fetches, done in two queries.
     * <p>
     * Why: LIMIT/OFFSET on a query that also loads a @OneToMany pages by child rows, not by parent.
     * Hibernate would then load every row and page in memory (slow). So:
     * <ol>
     *     <li>get only the ids of this page</li>
     *     <li>load those ids with the fetched associations</li>
     *     <li>put the rows back in the order of step 1</li>
     * </ol>
     */
    private List<ENTITY> findPageWithFetches(CriteriaBuilder cb, Specification<ENTITY> specification,
                                             Pageable pageable, List<String> fetchPaths) {
        String idName = EntityGraphUtils.idAttribute(entityManager, entityClass).getName();

        // 1. ids of this page (filter + sort + paging, no joins)
        CriteriaQuery<Object> idQuery = cb.createQuery(Object.class);
        Root<ENTITY> idRoot = idQuery.from(entityClass);
        idQuery.select(idRoot.get(idName));
        applyWhere(cb, idQuery, idRoot, specification);
        idQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), idRoot, cb));

        List<Object> ids = entityManager.createQuery(idQuery)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultList();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. load those ids with the fetched associations
        CriteriaQuery<ENTITY> query = cb.createQuery(entityClass);
        Root<ENTITY> root = query.from(entityClass);
        query.where(root.get(idName).in(ids));
        query.distinct(true);

        TypedQuery<ENTITY> typedQuery = entityManager.createQuery(query);
        applyFetchGraph(typedQuery, fetchPaths);
        List<ENTITY> loaded = typedQuery.getResultList();

        // 3. "WHERE id IN (...)" does not keep the order, so sort the rows like the ids of step 1
        PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        Map<Object, ENTITY> entityById = new HashMap<>();
        for (ENTITY entity : loaded) {
            entityById.putIfAbsent(unitUtil.getIdentifier(entity), entity);
        }

        List<ENTITY> ordered = new ArrayList<>(ids.size());
        for (Object id : ids) {
            ENTITY entity = entityById.get(id);
            if (entity != null) {
                ordered.add(entity);
            }
        }
        return ordered;
    }

    /**
     * SELECT COUNT(*) with the same WHERE as the search.
     */
    private long count(CriteriaBuilder cb, Specification<ENTITY> specification) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ENTITY> root = countQuery.from(entityClass);
        applyWhere(cb, countQuery, root, specification);

        // the specification may turn on DISTINCT; then count distinct rows
        if (countQuery.isDistinct()) {
            countQuery.select(cb.countDistinct(root));
        } else {
            countQuery.select(cb.count(root));
        }
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Adds the specification as the WHERE clause of the query.
     */
    private void applyWhere(CriteriaBuilder cb, CriteriaQuery<?> query, Root<ENTITY> root,
                            Specification<ENTITY> specification) {
        if (specification == null) {
            return;
        }
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
    }

    /**
     * Tells Hibernate to load the given associations in the same query.
     */
    private void applyFetchGraph(TypedQuery<ENTITY> query, List<String> fetchPaths) {
        if (fetchPaths == null || fetchPaths.isEmpty()) {
            return;
        }
        EntityGraph<ENTITY> graph = EntityGraphUtils.build(entityManager, entityClass, fetchPaths);
        query.setHint(FETCH_GRAPH_HINT, graph);
    }

    /**
     * Returns a new list where every Hibernate proxy is replaced by the real entity.
     */
    @SuppressWarnings("unchecked")
    private List<ENTITY> unproxyAll(List<ENTITY> entities) {
        List<ENTITY> result = new ArrayList<>(entities.size());
        for (ENTITY entity : entities) {
            result.add((ENTITY) Hibernate.unproxy(entity));
        }
        return result;
    }

    @Override
    public ENTITY getOneOrThrow(ENTITY_TYPE id) {
        return this.findOneOrThrow(id);
    }
    // =====================================================================
    // Private view helpers
    // =====================================================================

    /**
     * Finds the constructor viewClass(ENTITY), e.g. EmployeeViewDTO(Employee employee).
     */
    private <T> Constructor<T> getViewConstructor(Class<T> viewClass) {
        try {
            return viewClass.getConstructor(entityClass);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(viewClass.getName() + " needs a public constructor (" + entityClass.getSimpleName() + " entity)", ex);
        }
    }

    /**
     * Creates the view object from the entity, e.g. new EmployeeViewDTO(employee).
     */
    private <T> T toView(ENTITY entity, Constructor<T> constructor) {
        return BeanUtils.instantiateClass(constructor, entity);
    }
}