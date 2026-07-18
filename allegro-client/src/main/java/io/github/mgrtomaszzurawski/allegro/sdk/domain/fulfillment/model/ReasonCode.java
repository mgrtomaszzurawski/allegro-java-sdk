/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import org.jspecify.annotations.Nullable;

/**
 * Why the warehouse assigned a received quantity to a particular
 * {@link ReceivedType} — the fine-grained reason behind a non-sellable receipt.
 * Allegro documents this as an open value set that may grow without notice, so
 * an unrecognized token maps to {@link #UNKNOWN} rather than failing the read.
 *
 * @since 0.4.0
 */
public enum ReasonCode {

    /** Received in sellable condition. */
    SELLABLE("SELLABLE"),

    /** Damaged; a carrier claim applies. */
    DAMAGED_CARRIER_CLAIM("DAMAGED_CARRIER_CLAIM"),

    /** Damaged in transport. */
    DAMAGED_IN_TRANSPORT("DAMAGED_IN_TRANSPORT"),

    /** No barcode present. */
    NO_BARCODE("NO_BARCODE"),

    /** The product is not acceptable in One Fulfillment. */
    PRODUCT_NOT_ACCEPTABLE_IN_FULFILLMENT("PRODUCT_NOT_ACCEPTABLE_IN_FULFILLMENT"),

    /** The product's expiry date is too close. */
    SHORT_EXPIRY_DATE("SHORT_EXPIRY_DATE"),

    /** Hazardous material that cannot be accepted. */
    UNACCEPTABLE_HAZMAT("UNACCEPTABLE_HAZMAT"),

    /** The product's size cannot be accepted. */
    UNACCEPTABLE_PRODUCT_SIZE("UNACCEPTABLE_PRODUCT_SIZE"),

    /** The product variant cannot be accepted. */
    UNACCEPTABLE_PRODUCT_VARIANT("UNACCEPTABLE_PRODUCT_VARIANT"),

    /** The barcode could not be scanned. */
    UNSCANNABLE_BARCODE("UNSCANNABLE_BARCODE"),

    /** A reason Allegro introduced after this SDK build (open value set). */
    UNKNOWN(null);

    private final @Nullable String wireValue;

    ReasonCode(@Nullable String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact token Allegro uses on the wire, or {@code null} for {@link #UNKNOWN}. */
    public @Nullable String wireValue() {
        return wireValue;
    }

    /**
     * Resolve the enum from the wire token. The set is open, so an unrecognized
     * token maps to {@link #UNKNOWN} instead of throwing.
     */
    public static ReasonCode fromWire(String wireValue) {
        for (ReasonCode reason : values()) {
            if (wireValue.equals(reason.wireValue)) {
                return reason;
            }
        }
        return UNKNOWN;
    }
}
