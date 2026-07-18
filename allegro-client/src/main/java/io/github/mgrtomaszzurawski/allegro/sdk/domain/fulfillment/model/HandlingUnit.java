/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model;

import io.github.mgrtomaszzurawski.allegro.client.model.HandlingUnitRaw;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * How the goods of an Advance Ship Notice are physically packed for the
 * warehouse: the unit type (e.g. {@code BOX}, {@code PALLET}, {@code CONTAINER}),
 * how many units, and which labels apply. All fields are optional — Allegro does
 * not require them while the notice is a {@code DRAFT}.
 *
 * <p>The wire keeps {@code unitType} and {@code labelsType} as free-form strings
 * with a documented-but-open value set, so they are surfaced verbatim rather
 * than as typed enums.
 *
 * @param unitType   the packing unit type, or {@code null}
 * @param amount     how many handling units, or {@code null}
 * @param labelsType which labels apply (e.g. {@code ONE_FULFILMENT}, {@code NONE}), or {@code null}
 *
 * @since 0.4.0
 */
public record HandlingUnit(
        @Nullable String unitType,
        @Nullable BigDecimal amount,
        @Nullable String labelsType) {

    /** Map the generated Layer-1 DTO to the public record. */
    public static HandlingUnit from(HandlingUnitRaw raw) {
        return new HandlingUnit(raw.getUnitType(), raw.getAmount(), raw.getLabelsType());
    }
}
