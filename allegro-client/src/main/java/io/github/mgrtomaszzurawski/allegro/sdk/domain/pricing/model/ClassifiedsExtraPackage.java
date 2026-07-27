/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import java.util.Objects;

/**
 * An additional classifieds package attached to an offer, on top of its base
 * package. Each extra package adds to the offer's fees.
 *
 * @param id the extra package id (required)
 * @param republish whether the package republishes the offer at the end of its
 *     run
 *
 * @since 0.1.0
 */
public record ClassifiedsExtraPackage(String id, boolean republish) {

    /** Compact constructor validating the required id. */
    public ClassifiedsExtraPackage {
        Objects.requireNonNull(id, "id");
    }
}
