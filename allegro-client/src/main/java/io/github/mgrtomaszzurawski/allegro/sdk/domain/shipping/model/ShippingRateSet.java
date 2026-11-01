/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.GetShippingRatesSetUsingGET200ResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRateRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRatesSetRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A full shipping-rate set: its per-delivery-method {@link ShippingRate} rows and
 * the metadata Allegro keeps for it. Returned by {@code shipping.rates().get(id)},
 * {@code create(...)} and {@code update(...)}.
 *
 * @param id the rate-set identifier
 * @param name the seller's name for the set
 * @param type what kind of goods the set prices; {@link RateSetType#UNKNOWN} for a
 *     value this release does not model (or a legacy set the server returns
 *     without one — older physical sets predate the field)
 * @param dispatchCountry ISO country the rates dispatch from, or {@code null}
 *     (Allegro returns it for physical sets only)
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
        RateSetType type,
        @Nullable String dispatchCountry,
        @Nullable String lastModified,
        List<ShippingRate> rates,
        @Nullable RateSetFeatures features) {

    public ShippingRateSet {
        rates = List.copyOf(rates);
    }

    /**
     * Map the {@code GET /sale/shipping-rates/{id}} DTO — the only response that
     * carries the management {@code features} — to the public record.
     */
    public static ShippingRateSet from(GetShippingRatesSetUsingGET200ResponseRaw raw) {
        return assemble(raw.getId(), raw.getName(),
                raw.getType() == null ? null : raw.getType().getValue(),
                raw.getDispatchCountry(), raw.getLastModified(), raw.getRates(),
                raw.getFeatures() == null ? null : RateSetFeatures.from(raw.getFeatures()));
    }

    /**
     * Map the DTO the {@code POST /sale/shipping-rates} and
     * {@code PUT /sale/shipping-rates/{id}} responses share (no {@code features})
     * to the public record.
     */
    public static ShippingRateSet from(ShippingRatesSetRaw raw) {
        return assemble(raw.getId(), raw.getName(),
                raw.getType() == null ? null : raw.getType().getValue(),
                raw.getDispatchCountry(), raw.getLastModified(), raw.getRates(), null);
    }

    // The GET response DTO repeats the shared set shape plus `features` without a
    // supertype, so each factory extracts its own fields and delegates the common
    // assembly here. `type` is spec-required, but legacy physical sets predate the
    // field, so an absent value reads as UNKNOWN instead of failing the read.
    private static ShippingRateSet assemble(String id, String name, @Nullable String typeWire,
            @Nullable String dispatchCountry, @Nullable String lastModified,
            @Nullable List<ShippingRateRaw> ratesRaw, @Nullable RateSetFeatures features) {
        return new ShippingRateSet(id, name, RateSetType.fromWire(typeWire),
                dispatchCountry, lastModified, rates(ratesRaw), features);
    }

    private static List<ShippingRate> rates(@Nullable List<ShippingRateRaw> raw) {
        return raw == null ? List.of() : raw.stream().map(ShippingRate::from).toList();
    }
}
