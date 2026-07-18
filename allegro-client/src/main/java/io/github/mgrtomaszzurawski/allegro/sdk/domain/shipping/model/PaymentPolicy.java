/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

/**
 * When the buyer pays for an order shipped with a given delivery method.
 *
 * <p>This is a closed server enum: unlike the free-form string enums elsewhere in
 * this bucket (which fall back to an {@code UNKNOWN} sentinel), the underlying
 * Allegro field is a typed enumeration, so a value the SDK does not model is
 * rejected during response deserialization rather than surfacing here. See
 * {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * @since 0.2.0
 */
public enum PaymentPolicy {

    /** The buyer pays before dispatch. */
    IN_ADVANCE,

    /** The buyer pays on delivery (cash on delivery). */
    CASH_ON_DELIVERY
}
