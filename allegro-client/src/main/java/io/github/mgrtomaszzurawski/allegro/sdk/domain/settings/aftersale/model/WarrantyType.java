/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyTypeRaw;

/**
 * Who stands behind a seller warranty.
 *
 * @since 0.2.0
 */
public enum WarrantyType {

    /** The product's manufacturer is the warrantor. */
    MANUFACTURER,

    /** The seller is the warrantor. */
    SELLER;

    /**
     * Map the generated Layer-1 enum to the public domain enum. The wire value
     * and the constant name coincide for this enum, so an unmapped server value
     * fails loudly via {@link #valueOf(String)} rather than being swallowed.
     */
    public static WarrantyType from(WarrantyTypeRaw raw) {
        return valueOf(raw.name());
    }
}
