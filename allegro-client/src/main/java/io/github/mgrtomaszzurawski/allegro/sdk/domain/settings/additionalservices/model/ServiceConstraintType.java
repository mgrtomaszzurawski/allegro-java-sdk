/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ConstraintCriteriaRaw;

/**
 * How an additional-service configuration is constrained.
 *
 * <p>{@code COUNTRY_SAME_QUANTITY} applies to services realised before shipping
 * (e.g. {@code GIFT_WRAP}); {@code COUNTRY_DELIVERY_SAME_QUANTITY} applies to
 * services realised in delivery (e.g. {@code CARRY_IN}), which carry the delivery
 * methods that can realise them.
 *
 * @since 0.3.0
 */
public enum ServiceConstraintType {

    /** Realised before shipping; constrained by country. */
    COUNTRY_SAME_QUANTITY,

    /** Realised in delivery; constrained by country and delivery methods. */
    COUNTRY_DELIVERY_SAME_QUANTITY;

    /**
     * Map the generated Layer-1 enum. The wire value and constant name coincide,
     * so an unmapped server value fails loudly via {@link #valueOf(String)}.
     */
    public static ServiceConstraintType from(ConstraintCriteriaRaw.TypeEnum raw) {
        return valueOf(raw.name());
    }

    /**
     * Project onto the generated Layer-1 enum for a create/update request. The
     * constant name and wire value coincide.
     *
     * @return the matching generated enum value
     */
    public ConstraintCriteriaRaw.TypeEnum toRaw() {
        return ConstraintCriteriaRaw.TypeEnum.valueOf(name());
    }
}
