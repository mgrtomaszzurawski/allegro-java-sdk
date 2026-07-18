/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model;

/**
 * One named bucket of an offer-rating breakdown — either a score bucket (for
 * example {@code "5"}) or a size-feedback bucket (for example {@code "TOO_SMALL"})
 * with the number of responses that fell into it.
 *
 * @param name the bucket name
 * @param count how many responses fell into the bucket
 *
 * @since 0.2.0
 */
public record RatingBucket(String name, int count) {
}
