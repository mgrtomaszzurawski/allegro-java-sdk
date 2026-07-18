/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

/**
 * How a flexible bundle's discount is structured.
 *
 * @since 0.2.0
 */
public enum FlexibleBundleDiscountType {

    /** A single discount that applies to the whole bundle. */
    WHOLE_BUNDLE_DISCOUNT,

    /** A separate discount per slot. */
    SLOT_DISCOUNT,

    /** A value Allegro introduced that this SDK version does not model yet. */
    UNKNOWN
}
