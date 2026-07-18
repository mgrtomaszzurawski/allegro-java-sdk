/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CreateShippingRatesSetUsingPOSTRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModifyShippingRatesSetUsingPUTRequestRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.ShippingRateSetRequestBuilder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A request to create or replace a shipping-rate set, assembled with
 * {@link #builder()}. {@code name} and a non-empty {@code rates} list are
 * required; {@code type} and {@code dispatchCountry} are optional.
 * {@code rates().update(...)} has PUT semantics — send the full desired state.
 *
 * @param name the seller's name for the set (required)
 * @param type what kind of goods the set prices, or {@code null}
 * @param dispatchCountry ISO country the rates dispatch from, or {@code null}
 * @param rates the per-method rate rows (required, non-empty)
 *
 * @since 0.3.0
 */
public record ShippingRateSetRequest(
        String name,
        @Nullable RateSetType type,
        @Nullable String dispatchCountry,
        List<ShippingRate> rates) {

    public ShippingRateSetRequest {
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

    /** Build the {@code POST /sale/shipping-rates} request DTO. */
    public CreateShippingRatesSetUsingPOSTRequestRaw toCreateRaw() {
        CreateShippingRatesSetUsingPOSTRequestRaw raw =
                new CreateShippingRatesSetUsingPOSTRequestRaw();
        raw.setName(name);
        if (type != null) {
            raw.setType(CreateShippingRatesSetUsingPOSTRequestRaw.TypeEnum.fromValue(type.wireValue()));
        }
        if (dispatchCountry != null) {
            raw.setDispatchCountry(dispatchCountry);
        }
        raw.setRates(rates.stream().map(ShippingRate::toRaw).toList());
        return raw;
    }

    /**
     * Build the {@code PUT /sale/shipping-rates/{id}} request DTO, carrying the
     * path id in the body (Allegro expects it there, as on a point of service).
     */
    public ModifyShippingRatesSetUsingPUTRequestRaw toUpdateRaw(String rateSetId) {
        ModifyShippingRatesSetUsingPUTRequestRaw raw =
                new ModifyShippingRatesSetUsingPUTRequestRaw();
        raw.setId(rateSetId);
        raw.setName(name);
        if (type != null) {
            raw.setType(ModifyShippingRatesSetUsingPUTRequestRaw.TypeEnum.fromValue(type.wireValue()));
        }
        if (dispatchCountry != null) {
            raw.setDispatchCountry(dispatchCountry);
        }
        raw.setRates(rates.stream().map(ShippingRate::toRaw).toList());
        return raw;
    }
}
