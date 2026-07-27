/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.DeliveryOptionDtoLimitsDimensionsRaw;
import org.jspecify.annotations.Nullable;

/**
 * The maximum package dimensions a delivery option accepts. Each side is a
 * {@link Measure} (value plus unit); any side the server omits is {@code null}.
 * Read-only: it appears only in a delivery proposal's limits.
 *
 * @param length the maximum length, or {@code null}
 * @param width the maximum width, or {@code null}
 * @param height the maximum height, or {@code null}
 *
 * @since 0.5.0
 */
public record PackageDimensions(
        @Nullable Measure length,
        @Nullable Measure width,
        @Nullable Measure height) {

    /** Map the generated limits-dimensions DTO, or {@code null} when absent. */
    public static @Nullable PackageDimensions from(@Nullable DeliveryOptionDtoLimitsDimensionsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new PackageDimensions(
                Measure.from(raw.getLength()),
                Measure.from(raw.getWidth()),
                Measure.from(raw.getHeight()));
    }
}
