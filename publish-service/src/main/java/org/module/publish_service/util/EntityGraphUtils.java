package org.module.publish_service.util;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Subgraph;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.*;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/8/2026 10:53 PM
 */
@Component
@RequiredArgsConstructor
public class EntityGraphUtils {
    private static final String ID_ONLY_STUBS = EntityGraphUtils.class.getName() + ".ID_ONLY_STUBS";

    /**
     * Builds a fetch graph from dot paths (e.g. "userAddressList", "userAddressList.user").
     * Paths that don't match the entity model are skipped instead of failing the request.
     */
    public static <T> EntityGraph<T> build(EntityManager em, Class<T> rootClass, List<String> paths) {
        EntityGraph<T> graph = em.createEntityGraph(rootClass);
        for (String path : validPaths(em, rootClass, paths)) {
            addPath(graph, path);
        }
        return graph;
    }

    /**
     * Keeps only the paths whose every segment exists on the entity model.
     */
    public static List<String> validPaths(EntityManager em, Class<?> rootClass, List<String> paths) {
        if (paths == null) return Collections.emptyList();
        Metamodel metamodel = em.getMetamodel();
        return paths.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(path -> !path.isEmpty() && isValidPath(metamodel, rootClass, path))
                .distinct()
                .toList();
    }

    /**
     * Shapes the associations of the loaded entities for the response:
     * - association matched by a path : fully loaded (recursively shaped)
     * - any other association         : holds only its id(s), e.g. userAddressList = [{id: 1}, {id: 2}]
     * Paths that match no field are ignored, so no / unknown paths both give id-only associations.
     * Entities are detached first, so these changes are never flushed to the database.
     */
    public static <T> List<T> shape(EntityManager em, Class<T> rootClass, List<T> entities, List<String> paths) {
        if (entities == null || entities.isEmpty()) return entities;

        List<String> valid = validPaths(em, rootClass, paths);
        if (valid.isEmpty()) {
            idOnly(em, rootClass, entities, associations(em, rootClass));
        } else {
            prune(em, rootClass, entities, valid, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        return entities;
    }

    private static void addPath(EntityGraph<?> graph, String path) {
        String[] parts = path.split("\\.");
        Subgraph<?> subgraph = null;

        for (int i = 0; i < parts.length; i++) {
            String attr = parts[i];
            boolean last = (i == parts.length - 1);

            if (i == 0) {
                if (last) {
                    graph.addAttributeNodes(attr);
                } else {
                    subgraph = graph.addSubgraph(attr);
                }
            } else if (last) {
                subgraph.addAttributeNodes(attr);
            } else {
                subgraph = subgraph.addSubgraph(attr);
            }
        }
    }

    private static boolean isValidPath(Metamodel metamodel, Class<?> rootClass, String path) {
        String[] parts = path.split("\\.");
        Class<?> current = rootClass;
        for (int i = 0; i < parts.length; i++) {
            Attribute<?, ?> attribute;
            try {
                attribute = metamodel.managedType(current).getAttribute(parts[i]);
            } catch (IllegalArgumentException ex) {
                return false;
            }
            if (i < parts.length - 1) {
                if (!attribute.isAssociation()) return false;
                current = targetType(attribute);
            }
        }
        return true;
    }

    /**
     * Replaces the given associations with id-only stubs, loaded by one id query per association.
     */
    private static <T> void idOnly(EntityManager em, Class<?> rootClass, Collection<T> entities,
                                   List<Attribute<?, ?>> associations) {
        PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
        if (associations.isEmpty() || entities.isEmpty()) return;

        Map<T, Object> rootIds = new IdentityHashMap<>();
        entities.forEach(entity -> rootIds.put(entity, unitUtil.getIdentifier(entity)));

        Map<String, Map<Object, List<Object>>> childIdsByAttribute = new HashMap<>();
        for (Attribute<?, ?> attribute : associations) {
            childIdsByAttribute.put(attribute.getName(),
                    childIds(em, rootClass, attribute, new ArrayList<>(rootIds.values())));
        }

        entities.forEach(em::detach);

        for (T entity : entities) {
            Object rootId = rootIds.get(entity);
            for (Attribute<?, ?> attribute : associations) {
                Class<?> target = targetType(attribute);
                String targetIdName = idAttribute(em, target).getName();
                List<Object> ids = childIdsByAttribute.get(attribute.getName())
                        .getOrDefault(rootId, Collections.emptyList());

                Object value;
                if (attribute instanceof PluralAttribute<?, ?, ?> plural) {
                    value = newCollection(plural);
                    if (value instanceof Collection<?>) {
                        @SuppressWarnings("unchecked")
                        Collection<Object> stubs = (Collection<Object>) value;
                        ids.forEach(id -> stubs.add(stub(target, targetIdName, id)));
                    }
                } else {
                    value = ids.isEmpty() ? null : stub(target, targetIdName, ids.get(0));
                }
                write(entity, attribute.getName(), value);
            }
        }
    }

    /**
     * Fetch paths sent: keep the fetched associations (recursively shaped), the others become id-only stubs.
     */
    private static void prune(EntityManager em, Class<?> type, Collection<?> entities,
                              List<String> paths, Set<Object> visited) {
        List<Object> todo = entities.stream().filter(entity -> entity != null && visited.add(entity))
                .map(entity -> (Object) entity).toList();
        if (todo.isEmpty()) return;

        Map<String, List<String>> fetched = groupByFirstSegment(paths);
        List<Attribute<?, ?>> associations = associations(em, type);

        // initialize while still managed, so nothing is lazily loaded after detach
        for (Attribute<?, ?> attribute : associations) {
            if (!fetched.containsKey(attribute.getName())) continue;
            todo.forEach(entity -> Hibernate.initialize(read(entity, attribute.getName())));
        }

        todo.forEach(em::detach);

        List<Attribute<?, ?>> notFetched = new ArrayList<>();
        for (Attribute<?, ?> attribute : associations) {
            List<String> subPaths = fetched.get(attribute.getName());
            if (subPaths == null) {
                notFetched.add(attribute);
                continue;
            }

            List<Object> children = new ArrayList<>();
            for (Object entity : todo) {
                Object value = read(entity, attribute.getName());
                if (value instanceof Collection<?> collection) {
                    children.addAll(collection);
                } else if (value instanceof Map<?, ?> map) {
                    children.addAll(map.values());
                } else if (value != null) {
                    Object unproxied = Hibernate.unproxy(value);
                    write(entity, attribute.getName(), unproxied);
                    children.add(unproxied);
                }
            }
            prune(em, targetType(attribute), children, subPaths, visited);
        }

        // associations not in the paths: {id} only, never null
        idOnly(em, type, todo, notFetched);
    }

    private static Map<Object, List<Object>> childIds(EntityManager em, Class<?> rootClass,
                                                      Attribute<?, ?> attribute, List<Object> rootIds) {
        String rootIdName = idAttribute(em, rootClass).getName();
        String childIdName = idAttribute(em, targetType(attribute)).getName();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<?> root = query.from(rootClass);
        Join<?, ?> join = root.join(attribute.getName());
        query.multiselect(root.get(rootIdName), join.get(childIdName));
        query.where(root.get(rootIdName).in(rootIds));

        Map<Object, List<Object>> result = new HashMap<>();
        for (Object[] row : em.createQuery(query).getResultList()) {
            result.computeIfAbsent(row[0], key -> new ArrayList<>()).add(row[1]);
        }
        return result;
    }

    private static Map<String, List<String>> groupByFirstSegment(List<String> paths) {
        Map<String, List<String>> grouped = new HashMap<>();
        for (String path : paths) {
            int dot = path.indexOf('.');
            String head = dot < 0 ? path : path.substring(0, dot);
            List<String> rest = grouped.computeIfAbsent(head, key -> new ArrayList<>());
            if (dot >= 0) rest.add(path.substring(dot + 1));
        }
        return grouped;
    }

    private static List<Attribute<?, ?>> associations(EntityManager em, Class<?> type) {
        ManagedType<?> managedType = em.getMetamodel().managedType(type);
        List<Attribute<?, ?>> associations = new ArrayList<>();
        for (Attribute<?, ?> attribute : managedType.getAttributes()) {
            if (attribute.isAssociation()) associations.add(attribute);
        }
        return associations;
    }

    public static SingularAttribute<?, ?> idAttribute(EntityManager em, Class<?> type) {
        return em.getMetamodel().entity(type).getSingularAttributes().stream()
                .filter(SingularAttribute::isId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(type.getName() + " has no single @Id attribute"));
    }

    private static Class<?> targetType(Attribute<?, ?> attribute) {
        if (attribute instanceof PluralAttribute<?, ?, ?> plural) {
            return plural.getElementType().getJavaType();
        }
        return attribute.getJavaType();
    }

    private static Object newCollection(PluralAttribute<?, ?, ?> plural) {
        return switch (plural.getCollectionType()) {
            case SET -> new LinkedHashSet<>();
            case MAP -> null;
            default -> new ArrayList<>();
        };
    }

    /**
     * Returns the id property name when the bean is an id-only stub of the current request, otherwise null.
     * Used by IdOnlyJacksonModule to write only {"id": ...} for stubs.
     */
    public static String idOnlyProperty(Object bean) {
        Map<Object, String> stubs = requestStubs(false);
        return stubs == null ? null : stubs.get(bean);
    }

    private static Object stub(Class<?> type, String idName, Object id) {
        Object stub = BeanUtils.instantiateClass(type);
        write(stub, idName, id);
        Map<Object, String> stubs = requestStubs(true);
        if (stubs != null) stubs.put(stub, idName);
        return stub;
    }

    /**
     * Stubs are tracked by identity per request, so a real entity is never mistaken for a stub.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, String> requestStubs(boolean create) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;
        Map<Object, String> stubs = (Map<Object, String>) attributes.getAttribute(ID_ONLY_STUBS, RequestAttributes.SCOPE_REQUEST);
        if (stubs == null && create) {
            stubs = Collections.synchronizedMap(new IdentityHashMap<>());
            attributes.setAttribute(ID_ONLY_STUBS, stubs, RequestAttributes.SCOPE_REQUEST);
        }
        return stubs;
    }

    private static Object read(Object entity, String property) {
        return PropertyAccessorFactory.forDirectFieldAccess(entity).getPropertyValue(property);
    }

    private static void write(Object entity, String property, Object value) {
        PropertyAccessorFactory.forDirectFieldAccess(entity).setPropertyValue(property, value);
    }
}
