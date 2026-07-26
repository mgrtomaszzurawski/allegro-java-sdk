/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import org.jspecify.annotations.Nullable;

/**
 * The outcome of a bulk price/stock modification for one offer field — reached
 * from {@link PriceStockBatchReport#tasks()}. Each task identifies the offer and
 * the modified {@code field} (e.g. a marketplace price or the stock) it acted on.
 *
 * @param offerId the offer this task acted on, or {@code null} if unreported
 * @param field   the modified field (price marketplace or stock), or {@code null}
 * @param status  the task status token from Allegro ({@code NEW}/{@code SUCCESS}/
 *     {@code FAIL}), or {@code null}
 * @param message a human-readable detail for a failed task, or {@code null}
 * @since 0.5.0
 */
public record PriceStockTaskResult(
        @Nullable String offerId,
        @Nullable String field,
        @Nullable String status,
        @Nullable String message) {
}
