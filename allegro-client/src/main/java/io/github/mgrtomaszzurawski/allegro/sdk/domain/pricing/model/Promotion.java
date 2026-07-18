/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A rebate promotion the seller runs: one or more {@link Benefit benefits}
 * granted to buyers of the offers selected by its {@link OfferCriterion
 * criteria}.
 *
 * @param id the promotion id
 * @param status the promotion's lifecycle status
 * @param createdAt when the promotion was created, or {@code null} if absent
 * @param benefits the rewards the promotion grants (at least one)
 * @param offerCriteria which offers the promotion applies to (at least one)
 *
 * @since 0.4.0
 */
public record Promotion(
        String id,
        Status status,
        @Nullable Instant createdAt,
        List<Benefit> benefits,
        List<OfferCriterion> offerCriteria) {

    private static final String ERR_ID = "id must not be null";
    private static final String ERR_STATUS = "status must not be null";

    /** Lifecycle status of a rebate promotion. */
    public enum Status {

        /** The promotion is active. */
        ACTIVE,

        /** The promotion is inactive. */
        INACTIVE,

        /** The promotion is suspended. */
        SUSPENDED,

        /** A status this SDK version does not model yet (read-only). */
        UNKNOWN
    }

    /** Rejects a missing id or status and defensively copies the lists. */
    public Promotion {
        Objects.requireNonNull(id, ERR_ID);
        Objects.requireNonNull(status, ERR_STATUS);
        benefits = List.copyOf(benefits);
        offerCriteria = List.copyOf(offerCriteria);
    }
}
