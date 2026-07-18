/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link ShippingRateSetRequest}. {@code name} and a
 * non-empty {@code rates} list are required; {@code type} and
 * {@code dispatchCountry} are optional.
 *
 * @since 0.3.0
 */
public final class ShippingRateSetRequestBuilder {

    private static final String FIELD_NAME = "ShippingRateSetRequest.name";
    private static final String FIELD_RATES = "ShippingRateSetRequest.rates";

    private @Nullable String name;
    private @Nullable RateSetType type;
    private @Nullable String dispatchCountry;
    private List<ShippingRate> rates = List.of();

    /** The seller's name for the set (required). */
    public ShippingRateSetRequestBuilder name(@Nullable String value) {
        this.name = value;
        return this;
    }

    /** What kind of goods the set prices (optional). */
    public ShippingRateSetRequestBuilder type(@Nullable RateSetType value) {
        this.type = value;
        return this;
    }

    /** ISO country the rates dispatch from (optional). */
    public ShippingRateSetRequestBuilder dispatchCountry(@Nullable String value) {
        this.dispatchCountry = value;
        return this;
    }

    /** The per-method rate rows (required, non-empty). */
    public ShippingRateSetRequestBuilder rates(@Nullable List<ShippingRate> value) {
        this.rates = value == null ? List.of() : List.copyOf(value);
        return this;
    }

    /**
     * Validate and assemble the immutable {@link ShippingRateSetRequest}.
     *
     * @throws IllegalStateException if {@code name} is missing or {@code rates}
     *     is empty
     */
    public ShippingRateSetRequest build() {
        String validName = BuilderValidation.requireText(name, FIELD_NAME);
        List<ShippingRate> validRates = BuilderValidation.requireNonEmpty(rates, FIELD_RATES);
        return new ShippingRateSetRequest(validName, type, dispatchCountry, validRates);
    }
}
