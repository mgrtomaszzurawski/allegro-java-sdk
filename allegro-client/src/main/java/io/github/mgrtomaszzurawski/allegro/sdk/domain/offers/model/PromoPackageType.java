/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

/**
 * Which promotion-package slot a promo-options change targets: the single {@link #BASE} package
 * or one of the {@link #EXTRA} packages.
 *
 * @since 0.4.0
 */
public enum PromoPackageType {
    /** The base promotion package. */
    BASE,
    /** An extra promotion package. */
    EXTRA
}
