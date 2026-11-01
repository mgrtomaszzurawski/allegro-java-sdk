/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRatesSetRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShippingRateSetRequestBuilder;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A request to create or replace a shipping-rate set, assembled with
 * {@link #builder()}. {@code name}, {@code type} and a non-empty {@code rates}
 * list are required; {@code dispatchCountry} is required for a
 * {@link RateSetType#PHYSICAL} set and otherwise optional. Allegro fixes a set's
 * type and dispatch country when the set is created, so an update repeats the
 * values the set was created with. {@code rates().update(...)} has PUT
 * semantics — send the full desired state.
 *
 * @param name the seller's name for the set (required)
 * @param type what kind of goods the set prices (required)
 * @param dispatchCountry ISO 3166-1 alpha-2 country the rates dispatch from
 *     (required for a physical set), or {@code null}
 * @param rates the per-method rate rows (required, non-empty)
 *
 * @since 0.3.0
 */
public record ShippingRateSetRequest(
        String name,
        RateSetType type,
        @Nullable String dispatchCountry,
        List<ShippingRate> rates) {

    /** Rejects a null name/type (the builder validates them fully) and defensively copies the rows. */
    public ShippingRateSetRequest {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        rates = List.copyOf(rates);
    }

    /** A fresh builder for a {@link ShippingRateSetRequest}. */
    public static ShippingRateSetRequestBuilder builder() {
        return new ShippingRateSetRequestBuilder();
    }

    /** A builder pre-loaded with this request's fields. */
    public ShippingRateSetRequestBuilder toBuilder() {
        return new ShippingRateSetRequestBuilder()
                .name(name)
                .type(type)
                .dispatchCountry(dispatchCountry)
                .rates(rates);
    }

    /** Build the {@code POST /sale/shipping-rates} request DTO (no id — the server assigns one). */
    public ShippingRatesSetRaw toCreateRaw() {
        return toRaw(null);
    }

    /**
     * Build the {@code PUT /sale/shipping-rates/{id}} request DTO, carrying the
     * path id in the body (Allegro expects it there, as on a point of service).
     */
    public ShippingRatesSetRaw toUpdateRaw(String rateSetId) {
        return toRaw(rateSetId);
    }

    // Create and update share one wire schema (the spec folded the former per-operation
    // request bodies into ShippingRatesSet); only the echoed body id differs.
    private ShippingRatesSetRaw toRaw(@Nullable String rateSetId) {
        ShippingRatesSetRaw raw = new ShippingRatesSetRaw();
        if (rateSetId != null) {
            raw.setId(rateSetId);
        }
        raw.setName(name);
        raw.setType(ShippingRatesSetRaw.TypeEnum.fromValue(type.wireValue()));
        if (dispatchCountry != null) {
            raw.setDispatchCountry(dispatchCountry);
        }
        raw.setRates(rates.stream().map(ShippingRate::toRaw).toList());
        return raw;
    }
}
