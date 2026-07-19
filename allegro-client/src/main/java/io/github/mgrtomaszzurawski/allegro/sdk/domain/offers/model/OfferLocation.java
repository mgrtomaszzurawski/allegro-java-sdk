/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.LocationRaw;
import org.jspecify.annotations.Nullable;

/**
 * The physical location an offer ships from.
 *
 * <p>The same immutable value is used both ways: build one to set an offer's
 * location on {@code CreateOfferRequest}, or read one back from an {@link Offer}.
 * Every field is optional.
 *
 * @param city        the town/city
 * @param countryCode the ISO country code
 * @param postCode    the postal code
 * @param province    the province/region
 * @since 0.3.0
 */
public record OfferLocation(
        @Nullable String city,
        @Nullable String countryCode,
        @Nullable String postCode,
        @Nullable String province) {

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder().city(city).countryCode(countryCode).postCode(postCode).province(province);
    }

    /**
     * Project a generated location onto the consumer value.
     *
     * @param raw the generated location (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    public static @Nullable OfferLocation from(@Nullable LocationRaw raw) {
        if (raw == null) {
            return null;
        }
        return new OfferLocation(raw.getCity(), raw.getCountryCode(), raw.getPostCode(), raw.getProvince());
    }

    /** The generated location for this value. */
    public LocationRaw toRaw() {
        return new LocationRaw().city(city).countryCode(countryCode).postCode(postCode).province(province);
    }

    /** Fluent builder for {@link OfferLocation}. */
    public static final class Builder {

        private @Nullable String city;
        private @Nullable String countryCode;
        private @Nullable String postCode;
        private @Nullable String province;

        /** Set the town/city. */
        public Builder city(@Nullable String city) {
            this.city = city;
            return this;
        }

        /** Set the ISO country code. */
        public Builder countryCode(@Nullable String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        /** Set the postal code. */
        public Builder postCode(@Nullable String postCode) {
            this.postCode = postCode;
            return this;
        }

        /** Set the province/region. */
        public Builder province(@Nullable String province) {
            this.province = province;
            return this;
        }

        /** Build the location. */
        public OfferLocation build() {
            return new OfferLocation(city, countryCode, postCode, province);
        }
    }
}
