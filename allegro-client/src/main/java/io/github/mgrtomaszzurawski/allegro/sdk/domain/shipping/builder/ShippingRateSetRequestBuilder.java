/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for a {@link ShippingRateSetRequest}. {@code name}, {@code type}
 * and a non-empty {@code rates} list are required; {@code dispatchCountry} is
 * required for a {@link RateSetType#PHYSICAL} set (Allegro rejects a physical set
 * without one) and optional otherwise. The name follows the server rule — at most
 * {@value #NAME_MAX_LENGTH} characters of letters, numbers, hyphens, dots, commas
 * and spaces (anything else is refused on the wire with a {@code VALIDATION_ERROR}
 * on {@code name}).
 *
 * @since 0.3.0
 */
public final class ShippingRateSetRequestBuilder {

    /** Server-side limit on the set name (spec {@code maxLength}). */
    public static final int NAME_MAX_LENGTH = 64;

    /**
     * The regex character-class body of the characters a set name may contain —
     * letters, numbers, hyphens, dots, commas and spaces (the spec rule). Shared so a
     * caller normalising a name applies the same rule the builder enforces.
     */
    public static final String NAME_ALLOWED_CHARACTERS = "\\p{L}\\p{N} .,-";

    /** Server-side limit on {@code dispatchCountry} — an ISO 3166-1 alpha-2 code (spec {@code maxLength}). */
    public static final int DISPATCH_COUNTRY_MAX_LENGTH = 2;

    private static final String FIELD_NAME = "ShippingRateSetRequest.name";
    private static final String FIELD_TYPE = "ShippingRateSetRequest.type";
    private static final String FIELD_DISPATCH_COUNTRY = "ShippingRateSetRequest.dispatchCountry";
    private static final String FIELD_RATES = "ShippingRateSetRequest.rates";
    private static final Pattern NAME_ALLOWED = Pattern.compile("[" + NAME_ALLOWED_CHARACTERS + "]+");
    private static final String ERR_NAME_CHARSET =
            "ShippingRateSetRequest.name may only contain letters, numbers, hyphens, dots, commas and spaces";
    private static final String ERR_UNKNOWN_TYPE =
            "ShippingRateSetRequest.type must be PHYSICAL or ELECTRONIC (UNKNOWN is a read-only sentinel)";
    private static final String ERR_PHYSICAL_WITHOUT_DISPATCH_COUNTRY =
            "ShippingRateSetRequest.dispatchCountry is required for a PHYSICAL rate set";

    private @Nullable String name;
    private @Nullable RateSetType type;
    private @Nullable String dispatchCountry;
    private List<ShippingRate> rates = List.of();

    /**
     * The seller's name for the set (required; at most {@value #NAME_MAX_LENGTH}
     * characters of letters, numbers, hyphens, dots, commas and spaces).
     */
    public ShippingRateSetRequestBuilder name(@Nullable String value) {
        this.name = value;
        return this;
    }

    /** What kind of goods the set prices (required; fixed once the set exists). */
    public ShippingRateSetRequestBuilder type(@Nullable RateSetType value) {
        this.type = value;
        return this;
    }

    /**
     * ISO 3166-1 alpha-2 country the rates dispatch from (required for a
     * {@link RateSetType#PHYSICAL} set, optional otherwise; fixed once the set exists).
     */
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
     * @throws IllegalStateException if {@code name} is missing, too long or carries a
     *     character outside the allowed set, {@code type} is missing or the read-only
     *     {@link RateSetType#UNKNOWN}, a physical set has no {@code dispatchCountry},
     *     {@code dispatchCountry} is longer than an alpha-2 code, or {@code rates} is empty
     */
    public ShippingRateSetRequest build() {
        String validName = BuilderValidation.requireText(name, FIELD_NAME);
        BuilderValidation.requireMaxLength(validName, NAME_MAX_LENGTH, FIELD_NAME);
        if (!NAME_ALLOWED.matcher(validName).matches()) {
            throw new IllegalStateException(ERR_NAME_CHARSET);
        }
        RateSetType validType = BuilderValidation.requirePresent(type, FIELD_TYPE);
        if (validType == RateSetType.UNKNOWN) {
            throw new IllegalStateException(ERR_UNKNOWN_TYPE);
        }
        if (validType == RateSetType.PHYSICAL
                && (dispatchCountry == null || dispatchCountry.isBlank())) {
            throw new IllegalStateException(ERR_PHYSICAL_WITHOUT_DISPATCH_COUNTRY);
        }
        BuilderValidation.requireMaxLength(dispatchCountry, DISPATCH_COUNTRY_MAX_LENGTH, FIELD_DISPATCH_COUNTRY);
        List<ShippingRate> validRates = BuilderValidation.requireNonEmpty(rates, FIELD_RATES);
        return new ShippingRateSetRequest(validName, validType, dispatchCountry, validRates);
    }
}
