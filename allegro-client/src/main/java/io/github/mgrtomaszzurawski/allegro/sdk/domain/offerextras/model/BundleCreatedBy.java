/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

/**
 * Who created an offer bundle.
 *
 * @since 0.2.0
 */
public enum BundleCreatedBy {

    /** Created by the seller. */
    USER,

    /** Created by Allegro. */
    ALLEGRO,

    /** A value Allegro introduced that this SDK version does not model yet. */
    UNKNOWN
}
