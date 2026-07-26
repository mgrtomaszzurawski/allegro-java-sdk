/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Filter for the seller offer-event stream: optionally restrict to one event
 * {@link #type() type} (e.g. {@code OFFER_PRICE_CHANGED}). {@link #all()} streams every type.
 *
 * @param type the event type to restrict to, or {@code null} for all types
 * @since 0.4.0
 */
public record OfferEventFilter(@Nullable String type) {

    /** Stream events of every type. */
    public static OfferEventFilter all() {
        return new OfferEventFilter(null);
    }

    /** Stream only events of the given type (Allegro's wire value, e.g. {@code OFFER_ENDED}). */
    public static OfferEventFilter ofType(String type) {
        return new OfferEventFilter(Objects.requireNonNull(type, "type"));
    }
}
