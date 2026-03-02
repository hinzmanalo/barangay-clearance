package com.barangay.clearance.shared.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic fluent builder for constructing JPA {@link Specification} instances.
 *
 * <p>
 * Avoids repeating predicate-list boilerplate across service classes.
 * Each {@code .equal()}, {@code .greaterThanOrEqualTo()}, and
 * {@code .lessThanOrEqualTo()} call is a no-op when the supplied value is
 * {@code null}, so callers never need to null-check before adding a filter.
 * </p>
 *
 * <p>
 * Using {@link Specification} instead of JPQL
 * {@code :param IS NULL OR col = :param}
 * also sidesteps a Hibernate 6 type-inference bug with nullable enum
 * parameters.
 * </p>
 *
 * <p>
 * <b>Usage example:</b>
 * </p>
 * 
 * <pre>{@code
 * Specification<ClearanceRequest> spec = SpecificationBuilder.<ClearanceRequest>of()
 *         .equal("status", status)
 *         .equal("paymentStatus", paymentStatus)
 *         .greaterThanOrEqualTo("createdAt", from)
 *         .lessThanOrEqualTo("createdAt", to)
 *         .build();
 * }</pre>
 *
 * @param <T> the JPA entity type the specification targets
 */
public final class SpecificationBuilder<T> {

    /**
     * Internal steps accumulated by the builder. Each step is a lambda that
     * appends zero or one predicate to the supplied list.
     */
    private final List<SpecStep<T>> steps = new ArrayList<>();

    private SpecificationBuilder() {
    }

    /**
     * Creates a new, empty builder for entity type {@code T}.
     *
     * @param <T> the JPA entity type
     * @return a fresh builder instance
     */
    public static <T> SpecificationBuilder<T> of() {
        return new SpecificationBuilder<>();
    }

    /**
     * Adds an equality predicate ({@code field = value}) if {@code value} is
     * non-null. Supports any type including enums, strings, and UUIDs.
     *
     * @param field the entity attribute name (must match the Java field name)
     * @param value the value to compare against; {@code null} skips this predicate
     * @return this builder, for chaining
     */
    public SpecificationBuilder<T> equal(String field, Object value) {
        if (value != null) {
            steps.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * Adds a {@code field >= value} predicate if {@code value} is non-null.
     *
     * @param field the entity attribute name
     * @param value the lower bound (inclusive); {@code null} skips this predicate
     * @param <C>   a {@link Comparable} type (e.g. {@link java.time.Instant},
     *              {@link java.math.BigDecimal})
     * @return this builder, for chaining
     */
    public <C extends Comparable<? super C>> SpecificationBuilder<T> greaterThanOrEqualTo(String field, C value) {
        if (value != null) {
            steps.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * Adds a {@code field <= value} predicate if {@code value} is non-null.
     *
     * @param field the entity attribute name
     * @param value the upper bound (inclusive); {@code null} skips this predicate
     * @param <C>   a {@link Comparable} type (e.g. {@link java.time.Instant},
     *              {@link java.math.BigDecimal})
     * @return this builder, for chaining
     */
    public <C extends Comparable<? super C>> SpecificationBuilder<T> lessThanOrEqualTo(String field, C value) {
        if (value != null) {
            steps.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * Adds a case-insensitive {@code LIKE %value%} predicate if {@code value} is
     * non-null and non-blank. Useful for name/keyword search filters.
     *
     * @param field the entity attribute name (must be a {@code String} column)
     * @param value the substring to search for; {@code null} or blank skips this
     *              predicate
     * @return this builder, for chaining
     */
    public SpecificationBuilder<T> like(String field, String value) {
        if (value != null && !value.isBlank()) {
            steps.add((root, query, cb) -> cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
        }
        return this;
    }

    /**
     * Builds and returns the composed {@link Specification}.
     * If no predicates were added, returns a match-all specification.
     *
     * @return the composed specification
     */
    public Specification<T> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = steps.stream()
                    .map(step -> step.toPredicate(root, query, cb))
                    .toList();
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Functional interface mirroring {@link Specification#toPredicate} so each
     * builder step can be stored as a lambda.
     */
    @FunctionalInterface
    private interface SpecStep<T> {
        Predicate toPredicate(
                jakarta.persistence.criteria.Root<T> root,
                jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb);
    }
}
