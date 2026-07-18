/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw.PaymentPolicyEnum;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A delivery method Allegro offers to a seller, as returned by
 * {@code shipping.deliveryMethods()}. The {@link #id} is the value a shipping
 * rate row references to price that method.
 *
 * <p>The spec marks every field optional, but on the wire {@code id} and
 * {@code name} arrive (wire-verified 2026-07-18 against 571 live sandbox
 * methods; see {@code KNOWN-SERVER-BEHAVIORS.md}), so they are modelled
 * non-null.
 *
 * @param id method identifier
 * @param name human-readable method name
 * @param marketplaces marketplaces the method serves; never {@code null}, possibly empty
 * @param paymentPolicy when the buyer pays ({@link PaymentPolicy#UNKNOWN} for a
 *     value this release does not model), or {@code null} when the server omits it
 * @param allegroEndorsed whether Allegro endorses (recommends) this method
 * @param dispatchCountry ISO country the parcel is dispatched from, or {@code null}
 * @param destinationCountry ISO destination country, or {@code null}
 *
 * @since 0.2.0
 */
public record DeliveryMethod(
        String id,
        String name,
        List<String> marketplaces,
        @Nullable PaymentPolicy paymentPolicy,
        boolean allegroEndorsed,
        @Nullable String dispatchCountry,
        @Nullable String destinationCountry) {

    public DeliveryMethod {
        marketplaces = List.copyOf(marketplaces);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static DeliveryMethod from(
            GetListOfDeliveryMethodsUsingGET200ResponseDeliveryMethodsInnerRaw raw) {
        return new DeliveryMethod(
                raw.getId(),
                raw.getName(),
                marketplaces(raw.getMarketplaces()),
                paymentPolicy(raw.getPaymentPolicy()),
                Boolean.TRUE.equals(raw.getAllegroEndorsed()),
                raw.getDispatchCountry(),
                raw.getDestinationCountry());
    }

    private static List<String> marketplaces(@Nullable List<String> raw) {
        return raw == null ? List.of() : List.copyOf(raw);
    }

    // A null policy stays null (the server omitted the field); a present value is
    // mapped fail-soft — an unmodelled value or the generator's forward-compat
    // sentinel lands on UNKNOWN instead of throwing, so a new server payment
    // policy never breaks the whole delivery-methods read. A ShippingEnumsTest
    // parity check guards that the modelled names still line up with the raw enum.
    private static @Nullable PaymentPolicy paymentPolicy(@Nullable PaymentPolicyEnum raw) {
        return raw == null ? null : PaymentPolicy.fromWire(raw.name());
    }
}
