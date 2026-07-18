/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import java.util.List;

/**
 * Optional filter for listing Allegro pickup / drop-off points. {@link #all()}
 * lists points for every carrier; otherwise only points served by the listed
 * carrier codes (e.g. {@code UPS}, {@code DPD}) are returned.
 *
 * @since 0.4.0
 */
public final class PointsFilter {

    private final List<String> carrierCodes;

    private PointsFilter(Builder builder) {
        this.carrierCodes = List.copyOf(builder.carrierCodes);
    }

    /** Carrier codes to match (any of); empty lists points for all carriers. */
    public List<String> carrierCodes() {
        return carrierCodes;
    }

    /** A filter that lists points for every carrier. */
    public static PointsFilter all() {
        return builder().build();
    }

    /** A filter restricted to the given carrier codes. */
    public static PointsFilter ofCarriers(String... codes) {
        return builder().carrierCodes(codes).build();
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        return new Builder().carrierCodes(carrierCodes);
    }

    /** Fluent builder for {@link PointsFilter}. */
    public static final class Builder {

        private List<String> carrierCodes = List.of();

        /** Keep only points served by these carrier codes. */
        public Builder carrierCodes(List<String> codes) {
            this.carrierCodes = List.copyOf(codes);
            return this;
        }

        /** Keep only points served by these carrier codes. */
        public Builder carrierCodes(String... codes) {
            return carrierCodes(List.of(codes));
        }

        /** Build the filter. */
        public PointsFilter build() {
            return new PointsFilter(this);
        }
    }
}
