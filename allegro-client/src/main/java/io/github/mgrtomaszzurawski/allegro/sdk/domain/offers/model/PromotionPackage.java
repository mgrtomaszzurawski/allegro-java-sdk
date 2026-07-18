/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailablePromotionPackageRaw;
import org.jspecify.annotations.Nullable;

/**
 * A promotion package a seller can apply to an offer (e.g. a bold title or a
 * highlight), as offered in {@link AvailablePromotionPackages}.
 *
 * @param id            the package identifier
 * @param name          human-readable package name, or {@code null}
 * @param cycleDuration ISO-8601 duration of one billing cycle, or {@code null}
 * @since 0.2.0
 */
public record PromotionPackage(
        @Nullable String id,
        @Nullable String name,
        @Nullable String cycleDuration) {

    /** Project a generated available-package entry onto the consumer record. */
    public static PromotionPackage from(AvailablePromotionPackageRaw raw) {
        return new PromotionPackage(raw.getId(), raw.getName(), raw.getCycleDuration());
    }
}
