package com.example.flowershop.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Native Hibernate persistence operations shared by the hand-written repositories.
 * The EntityManager is only the bridge to Spring's transaction-bound Hibernate Session.
 * Never open/close a second Session or commit a transaction owned by a Service.
 * Page/Sort and Specification remain input/output adapters for existing callers;
 * no Spring Data repository implementation is used here.
 */
@Transactional(readOnly = true)
public abstract class HibernateRepository<T> {
    @PersistenceContext
    private EntityManager entityManager;
    private final Class<T> entityType;
    private final String entityName;

    protected HibernateRepository(Class<T> entityType) {
        this.entityType = entityType;
        this.entityName = entityType.getSimpleName();
    }

    protected Session session() { return entityManager.unwrap(Session.class); }
    protected SessionFactory sessionFactory() { return session().getSessionFactory(); }

    public Optional<T> findById(String id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(session().find(entityType, id));
    }

    public boolean existsById(String id) { return countWhere("e.id = :id", Map.of("id", id)) > 0; }
    public long count() { return countWhere("", Map.of()); }
    public List<T> findAll() { return list("", Map.of()); }
    public List<T> findAll(Sort sort) { return list("", Map.of(), sort); }
    public Page<T> findAll(Pageable pageable) { return page("", Map.of(), pageable); }

    public List<T> findAllById(Iterable<String> ids) {
        List<String> values = new ArrayList<>();
        ids.forEach(values::add);
        return values.isEmpty() ? List.of() : list("e.id in :ids", Map.of("ids", values));
    }

    @Transactional
    public <S extends T> S save(S entity) {
        Objects.requireNonNull(entity, "entity");
        return session().contains(entity) ? entity : session().merge(entity);
    }

    @Transactional
    public <S extends T> S saveAndFlush(S entity) {
        S managed = save(entity);
        session().flush();
        return managed;
    }

    @Transactional
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) saved.add(save(entity));
        return saved;
    }

    @Transactional
    public void flush() { session().flush(); }

    @Transactional
    public void delete(T entity) {
        session().remove(session().contains(entity) ? entity : session().merge(entity));
    }

    @Transactional
    public void deleteById(String id) { findById(id).ifPresent(this::delete); }

    @Transactional
    public void deleteAll(Iterable<? extends T> entities) { entities.forEach(this::delete); }

    @Transactional
    public void deleteAll() { findAll().forEach(this::delete); }

    protected SelectionQuery<T> selection(String predicate, Map<String, ?> parameters, Sort sort) {
        SelectionQuery<T> query = session().createSelectionQuery("from " + entityName + " e" + where(predicate) + orderBy(sort), entityType);
        return bind(query, parameters);
    }

    protected List<T> list(String predicate, Map<String, ?> parameters) { return list(predicate, parameters, Sort.unsorted()); }
    protected List<T> list(String predicate, Map<String, ?> parameters, Sort sort) { return selection(predicate, parameters, sort).getResultList(); }
    protected Optional<T> one(String predicate, Map<String, ?> parameters) { return selection(predicate, parameters, Sort.unsorted()).uniqueResultOptional(); }
    protected Optional<T> first(String predicate, Map<String, ?> parameters, Sort sort) { return selection(predicate, parameters, sort).setMaxResults(1).uniqueResultOptional(); }
    protected Optional<T> locked(String predicate, Map<String, ?> parameters) {
        return selection(predicate, parameters, Sort.unsorted()).setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResultOptional();
    }

    protected long countWhere(String predicate, Map<String, ?> parameters) {
        return bind(session().createSelectionQuery("select count(e) from " + entityName + " e" + where(predicate), Long.class), parameters).getSingleResult();
    }

    protected Page<T> page(String predicate, Map<String, ?> parameters, Pageable pageable) {
        var query = selection(predicate, parameters, pageable.getSort());
        limit(query, pageable);
        return new PageImpl<>(query.getResultList(), pageable, countWhere(predicate, parameters));
    }

    private static String where(String predicate) { return predicate.isBlank() ? "" : " where " + predicate; }
    private static <R> SelectionQuery<R> bind(SelectionQuery<R> query, Map<String, ?> parameters) {
        parameters.forEach((name, value) -> {
            if (value instanceof Collection<?> values) query.setParameterList(name, values);
            else query.setParameter(name, value);
        });
        return query;
    }
    private static void limit(SelectionQuery<?> query, Pageable pageable) {
        if (pageable.isPaged()) query.setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize());
    }
    private static String orderBy(Sort sort) {
        List<String> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            // Only property paths may become HQL identifiers; all values are bound parameters.
            if (!order.getProperty().matches("[A-Za-z_]\\w*(\\.[A-Za-z_]\\w*)*")) throw new IllegalArgumentException("Invalid sort property");
            String path = "e." + order.getProperty();
            if (order.isIgnoreCase()) path = "lower(" + path + ")";
            String nulls = switch (order.getNullHandling()) { case NULLS_FIRST -> " nulls first"; case NULLS_LAST -> " nulls last"; default -> ""; };
            orders.add(path + (order.isAscending() ? " asc" : " desc") + nulls);
        }
        return orders.isEmpty() ? "" : " order by " + String.join(", ", orders);
    }

    /** Reuses existing filter predicates, executing both queries through Hibernate Session. */
    public Page<T> findAll(Specification<T> specification, Pageable pageable) {
        var builder = sessionFactory().getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(entityType);
        Root<T> root = criteria.from(entityType);
        criteria.select(root);
        if (specification != null) {
            var predicate = specification.toPredicate(root, criteria, builder);
            if (predicate != null) criteria.where(predicate);
        }
        List<jakarta.persistence.criteria.Order> ordering = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            Path<?> path = root;
            for (String part : order.getProperty().split("\\.")) path = path.get(part);
            var expression = order.isIgnoreCase() ? builder.lower(path.as(String.class)) : path;
            ordering.add(order.isAscending() ? builder.asc(expression) : builder.desc(expression));
        }
        if (!ordering.isEmpty()) criteria.orderBy(ordering);
        var query = session().createSelectionQuery(criteria);
        limit(query, pageable);

        CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
        Root<T> countRoot = countCriteria.from(entityType);
        if (specification != null) {
            var predicate = specification.toPredicate(countRoot, countCriteria, builder);
            if (predicate != null) countCriteria.where(predicate);
        }
        countCriteria.select(criteria.isDistinct() ? builder.countDistinct(countRoot) : builder.count(countRoot));
        // Count queries must not inherit ordering requested by a filter predicate.
        countCriteria.orderBy(List.of());
        long total = session().createSelectionQuery(countCriteria).getSingleResult();
        return new PageImpl<>(query.getResultList(), pageable, total);
    }
}
