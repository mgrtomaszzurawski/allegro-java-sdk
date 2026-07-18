/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.util.List;
import java.util.Objects;

/**
 * Which offers a rebate {@link Promotion} applies to. Either an explicit set of
 * offers ({@link Type#CONTAINS_OFFERS}), every offer the seller has
 * ({@link Type#ALL_OFFERS}), or offers assigned through another channel
 * ({@link Type#OFFERS_ASSIGNED_EXTERNALLY}); {@code offerIds} is populated only
 * for {@link Type#CONTAINS_OFFERS}.
 *
 * @param type how the offers are selected
 * @param offerIds the explicit offer ids (empty unless {@code type} is
 *     {@link Type#CONTAINS_OFFERS})
 *
 * @since 0.4.0
 */
public record OfferCriterion(Type type, List<String> offerIds) {

    private static final String ERR_TYPE = "type must not be null";
    private static final String ERR_OFFER_IDS = "offerIds must not be null";

    /** How the offers included in a promotion are selected. */
    public enum Type {

        /** Only the offers explicitly listed in {@code offerIds}. */
        CONTAINS_OFFERS,

        /** Offers assigned to the promotion through another channel. */
        OFFERS_ASSIGNED_EXTERNALLY,

        /** Every offer the seller has. */
        ALL_OFFERS,

        /** A selection type this SDK version does not model yet (read-only). */
        UNKNOWN
    }

    /** Rejects a missing type and defensively copies the offer ids. */
    public OfferCriterion {
        Objects.requireNonNull(type, ERR_TYPE);
        Objects.requireNonNull(offerIds, ERR_OFFER_IDS);
        offerIds = List.copyOf(offerIds);
    }

    /**
     * A criterion covering an explicit set of offers.
     *
     * @param offerIds the offers the promotion applies to
     * @return a {@link Type#CONTAINS_OFFERS} criterion
     */
    public static OfferCriterion containing(List<String> offerIds) {
        return new OfferCriterion(Type.CONTAINS_OFFERS, offerIds);
    }

    /**
     * A criterion covering every offer the seller has.
     *
     * @return an {@link Type#ALL_OFFERS} criterion
     */
    public static OfferCriterion allOffers() {
        return new OfferCriterion(Type.ALL_OFFERS, List.of());
    }

    /**
     * A criterion for offers assigned to the promotion through another channel.
     *
     * @return an {@link Type#OFFERS_ASSIGNED_EXTERNALLY} criterion
     */
    public static OfferCriterion assignedExternally() {
        return new OfferCriterion(Type.OFFERS_ASSIGNED_EXTERNALLY, List.of());
    }
}
