/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CreateShippingRatesSetUsingPOST201ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetShippingRatesSetUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ModifyShippingRatesSetUsingPUT200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A full shipping-rate set: its per-delivery-method {@link ShippingRate} rows and
 * the metadata Allegro keeps for it. Returned by {@code shipping.rates().get(id)},
 * {@code create(...)} and {@code update(...)}.
 *
 * @param id the rate-set identifier
 * @param name the seller's name for the set
 * @param type what kind of goods the set prices, or {@code null} when the server
 *     omits it
 * @param dispatchCountry ISO country the rates dispatch from, or {@code null}
 * @param lastModified when the set was last changed (ISO-8601 string), or {@code null}
 * @param rates the per-method rate rows; never {@code null}, possibly empty
 * @param features management flags for the set, or {@code null} (absent on the
 *     create/update responses)
 *
 * @since 0.3.0
 */
public record ShippingRateSet(
        String id,
        String name,
        @Nullable RateSetType type,
        @Nullable String dispatchCountry,
        @Nullable String lastModified,
        List<ShippingRate> rates,
        @Nullable RateSetFeatures features) {

    public ShippingRateSet {
        rates = List.copyOf(rates);
    }

    /** Map the {@code GET /sale/shipping-rates/{id}} DTO to the public record. */
    public static ShippingRateSet from(GetShippingRatesSetUsingGET200ResponseRaw raw) {
        return new ShippingRateSet(
                raw.getId(),
                raw.getName(),
                type(raw.getType() == null ? null : raw.getType().getValue()),
                raw.getDispatchCountry(),
                raw.getLastModified(),
                rates(raw.getRates()),
                raw.getFeatures() == null ? null : RateSetFeatures.from(raw.getFeatures()));
    }

    /** Map the {@code POST /sale/shipping-rates} response DTO to the public record. */
    public static ShippingRateSet from(CreateShippingRatesSetUsingPOST201ResponseRaw raw) {
        return new ShippingRateSet(
                raw.getId(),
                raw.getName(),
                type(raw.getType() == null ? null : raw.getType().getValue()),
                raw.getDispatchCountry(),
                raw.getLastModified(),
                rates(raw.getRates()),
                null);
    }

    /** Map the {@code PUT /sale/shipping-rates/{id}} response DTO to the public record. */
    public static ShippingRateSet from(ModifyShippingRatesSetUsingPUT200ResponseRaw raw) {
        return new ShippingRateSet(
                raw.getId(),
                raw.getName(),
                type(raw.getType() == null ? null : raw.getType().getValue()),
                raw.getDispatchCountry(),
                raw.getLastModified(),
                rates(raw.getRates()),
                null);
    }

    private static @Nullable RateSetType type(@Nullable String wire) {
        return wire == null ? null : RateSetType.fromWire(wire);
    }

    private static List<ShippingRate> rates(@Nullable List<ShippingRateRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(ShippingRate::from).toList();
    }
}
