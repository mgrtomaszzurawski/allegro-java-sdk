/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * When the buyer pays for an order shipped with a given delivery method.
 *
 * <p>Read-only: this value is only ever surfaced by {@code shipping.deliveryMethods()},
 * never sent by a consumer. Like the other shipping read enums it is fail-soft —
 * a value this SDK release does not model (or the generator's forward-compat
 * sentinel) maps to {@link #UNKNOWN} rather than breaking the response, so a new
 * Allegro payment policy never fails the whole delivery-methods read. See
 * {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * @since 0.2.0
 */
public enum PaymentPolicy {

    /** The buyer pays before dispatch. */
    IN_ADVANCE,

    /** The buyer pays on delivery (cash on delivery). */
    CASH_ON_DELIVERY,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    /**
     * Map a wire value to the enum, falling back to {@link #UNKNOWN} for a missing
     * or unmodelled value (including the generated sentinel).
     */
    public static PaymentPolicy fromWire(@Nullable String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
