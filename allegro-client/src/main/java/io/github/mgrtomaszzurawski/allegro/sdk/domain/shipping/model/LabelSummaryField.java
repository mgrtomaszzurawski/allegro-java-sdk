/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import org.jspecify.annotations.Nullable;

/**
 * A column the label summary report can carry for each shipment.
 *
 * <p>Fail-soft on read (an unmodelled server value maps to {@link #UNKNOWN}) and
 * strict on write ({@link #UNKNOWN} cannot be serialized).
 *
 * @since 0.4.0
 */
public enum LabelSummaryField {

    /** The carrier waybill number (spec value {@code WAYBILL}). */
    WAYBILL,

    /** The Allegro order identifier (spec value {@code ORDER_ID}). */
    ORDER_ID,

    /** The buyer login (spec value {@code BUYER_LOGIN}). */
    BUYER_LOGIN,

    /** The ordered items (spec value {@code ITEMS}). */
    ITEMS,

    /** Package dimensions and weight (spec value {@code DIMS_AND_WEIGHT}). */
    DIMS_AND_WEIGHT,

    /** Free text added to the label (spec value {@code ADD_LABEL_TEXT}). */
    ADD_LABEL_TEXT,

    /** Notes attached to the order (spec value {@code NOTES_FOR_ORDER}). */
    NOTES_FOR_ORDER,

    /** The seller reference number (spec value {@code REF_NUMBER}). */
    REF_NUMBER,

    /** The cash-on-delivery amount (spec value {@code COD}). */
    COD,

    /** The declared insurance amount (spec value {@code INSURANCE}). */
    INSURANCE,

    /** A value returned by the server that this SDK release does not model. */
    UNKNOWN;

    private static final String ERR_UNKNOWN =
            "UNKNOWN is a read-only sentinel and cannot be sent to Allegro";

    /**
     * Wire representation to send to Allegro.
     *
     * @throws IllegalStateException if called on {@link #UNKNOWN}, which never
     *     originates from consumer input on a write path
     */
    public String wireValue() {
        if (this == UNKNOWN) {
            throw new IllegalStateException(ERR_UNKNOWN);
        }
        return name();
    }

    /** Map a wire value to the enum, falling back to {@link #UNKNOWN}. */
    public static LabelSummaryField fromWire(@Nullable String raw) {
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
