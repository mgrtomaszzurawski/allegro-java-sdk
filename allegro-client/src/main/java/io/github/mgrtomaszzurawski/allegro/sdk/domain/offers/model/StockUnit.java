/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOffersRequestStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockRaw;
import org.jspecify.annotations.Nullable;

/**
 * The unit an offer's available quantity is counted in.
 *
 * @since 0.3.0
 */
public enum StockUnit {

    /** A single item (the default). */
    UNIT,
    /** A pair. */
    PAIR,
    /** A set. */
    SET,
    /** A unit this SDK release does not model yet (read-only, never written). */
    UNKNOWN;

    private static final String NOT_WRITABLE = "StockUnit.UNKNOWN is a read-only sentinel and cannot be sent";

    /** Map the generated stock unit, tolerating unknown future values. */
    public static StockUnit from(StockRaw.@Nullable UnitEnum raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw) {
            case UNIT -> UNIT;
            case PAIR -> PAIR;
            case SET -> SET;
            default -> UNKNOWN;
        };
    }

    /**
     * The generated request enum for this unit.
     *
     * @return the wire value
     * @throws IllegalStateException if called on {@link #UNKNOWN} (not writable)
     */
    public SaleProductOffersRequestStockRaw.UnitEnum toRaw() {
        return switch (this) {
            case UNIT -> SaleProductOffersRequestStockRaw.UnitEnum.UNIT;
            case PAIR -> SaleProductOffersRequestStockRaw.UnitEnum.PAIR;
            case SET -> SaleProductOffersRequestStockRaw.UnitEnum.SET;
            case UNKNOWN -> throw new IllegalStateException(NOT_WRITABLE);
        };
    }
}
