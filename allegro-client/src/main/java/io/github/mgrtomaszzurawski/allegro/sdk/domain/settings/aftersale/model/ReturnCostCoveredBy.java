/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyReturnCostRaw;

/**
 * Who covers the cost of a return delivery.
 *
 * @since 0.3.0
 */
public enum ReturnCostCoveredBy {

    /** The seller covers the return delivery cost. */
    SELLER,

    /** The buyer covers the return delivery cost. */
    BUYER;

    /**
     * Map the generated Layer-1 enum. The wire value and the constant name
     * coincide, so an unmapped server value fails loudly via
     * {@link #valueOf(String)}.
     */
    public static ReturnCostCoveredBy from(ReturnPolicyReturnCostRaw.CoveredByEnum raw) {
        return valueOf(raw.name());
    }
}
