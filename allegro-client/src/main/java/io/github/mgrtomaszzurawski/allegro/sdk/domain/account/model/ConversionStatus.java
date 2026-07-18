/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionRaw;
import org.jspecify.annotations.Nullable;

/**
 * Lifecycle status of an affiliate CPS conversion. The constant names match the
 * values Allegro accepts as the {@code status} filter and returns on the wire.
 *
 * @since 0.2.0
 */
public enum ConversionStatus {

    /** The conversion has been created but not yet settled. */
    CREATED,
    /** The conversion was rejected (e.g. the order was returned). */
    REJECTED,
    /** The conversion was confirmed and is payable. */
    CONFIRMED;

    /** Map the generated Layer-1 enum to this public enum. */
    public static @Nullable ConversionStatus from(CpsConversionRaw.@Nullable StatusEnum raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case CREATED -> CREATED;
            case REJECTED -> REJECTED;
            case CONFIRMED -> CONFIRMED;
        };
    }
}
