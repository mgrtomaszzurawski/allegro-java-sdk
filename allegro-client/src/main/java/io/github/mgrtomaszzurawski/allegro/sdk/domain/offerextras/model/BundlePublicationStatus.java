/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

/**
 * An offer bundle's publication status on one marketplace.
 *
 * @since 0.2.0
 */
public enum BundlePublicationStatus {

    /** The bundle is active on the marketplace. */
    ACTIVE,

    /** The bundle is suspended on the marketplace. */
    SUSPENDED,

    /** A value Allegro introduced that this SDK version does not model yet. */
    UNKNOWN
}
