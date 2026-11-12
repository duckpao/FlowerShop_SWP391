# Hand-written Hibernate repositories

The custom-flower and shared order workflow uses concrete `@Repository` classes.
These classes do not extend `JpaRepository` and do not rely on method-name query generation.
Every domain query is implemented explicitly using Hibernate HQL or Criteria queries.

## Converted repositories

`OrderRepository`, `OrderDetailRepository`, `CustomOrderRequestRepository`,
`PaymentRepository`, `DeliveryRepository`, `AddressRepository`, `ShopRepository`,
`UserRepository`, `ProductRepository`, `CategoryRepository`,
`ProductImageRepository` and `ProductVideoRepository`.

Other features such as authentication sessions and shop-follow notifications retain
their existing Spring Data repositories. They share the same persistence unit and
transaction manager with these repositories.

## Session and transaction ownership

`HibernateRepository<T>` centralizes common CRUD, binding, paging and sorting.
`@PersistenceContext EntityManager` supplies Spring's transaction-bound persistence
context; `unwrap(Session.class)` obtains its native Hibernate Session.
The SessionFactory is obtained from that Session for the Criteria builder.

Do not use `openSession()`, `close()` or manual `beginTransaction()/commit()` in
these repositories: that would create another persistence context and break the
service transaction. Keep business operations annotated with `@Transactional`.
Repository methods also define transaction behavior so direct repository calls work.

`save()` returns the managed entity. It leaves already-managed entities attached;
otherwise it uses `Session.merge()`, preserving the previous behavior for assigned
UUID identifiers and detached objects. Callers should use the returned instance
when they need to mutate a newly attached entity. Hibernate dirty checking persists
managed updates when the service transaction flushes or commits.

## Queries

For example, `OrderRepository.findByIdAndCustomerId(...)` explicitly executes an HQL
predicate on both the order ID and customer ID. It is not an automatically derived query.
`OrderDetailRepository.findCustomDesignByOrderId(...)` selects the design line itself,
rather than loading all lines and assuming the first one is a custom design.
`countByOrderIds(...)` counts all lines for a customer order page in one grouped query.

All query values use named parameters. A write-lock query uses `PESSIMISTIC_WRITE`
inside the existing transaction. Sort identifiers are validated before becoming HQL.
Paged queries include a separate total-count query.

Existing `Page`, `Pageable`, `Sort` and `Specification` types remain compatibility
adapters for service filters and API pagination. Specification predicates are applied
to Criteria queries executed by Hibernate Session; no Spring Data repository proxy
executes these queries.

## Validation

Run `gradlew.bat test -x buildFrontend -x installFrontend`.
The ordinary workflow tests cover the customer/shop APIs, pricing, payment gates
and GHN transitions. `HibernateRepositoryTests` additionally checks database state
after flush/clear, write locks, Criteria pagination and counts, literal wildcard
searches, ownership, empty collections, delete operations, direct repository
transactions and rollback shared with an unchanged Spring Data repository.

This refactoring does not require a database schema change.

References:
- https://docs.hibernate.org/orm/7.1/userguide/html_single/
- https://docs.spring.io/spring-framework/reference/data-access/orm/jpa.html
