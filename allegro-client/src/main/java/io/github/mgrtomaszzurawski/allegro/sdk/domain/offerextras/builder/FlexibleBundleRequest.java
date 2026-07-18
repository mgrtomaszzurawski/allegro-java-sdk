/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscount;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A flexible bundle to create ({@code FlexibleBundles.create}) or update
 * ({@code FlexibleBundles.update}): its slots and an optional discount. Both
 * operations take the same request. Build it with {@link #builder()}.
 *
 * @param slots the bundle's slots; never {@code null}, never empty
 * @param discount the bundle's discount configuration, or {@code null} for a
 *     bundle sold without a bundle discount
 *
 * @since 0.2.0
 */
public record FlexibleBundleRequest(
        List<FlexibleBundleSlotRequest> slots,
        @Nullable FlexibleBundleDiscount discount) {

    public FlexibleBundleRequest {
        slots = List.copyOf(slots);
    }

    /**
     * A new, empty builder.
     *
     * @return a fresh {@link FlexibleBundleRequestBuilder}
     */
    public static FlexibleBundleRequestBuilder builder() {
        return new FlexibleBundleRequestBuilder();
    }

    /**
     * A builder pre-populated with this request's fields.
     *
     * @return a builder holding this request's values
     */
    public FlexibleBundleRequestBuilder toBuilder() {
        return new FlexibleBundleRequestBuilder().slots(slots).discount(discount);
    }
}
