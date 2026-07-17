/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.fulfillment;

import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRemovalPreferenceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentWithdrawalAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PhoneNumberWithCountryCodeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.Fulfillment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link Fulfillment} facade. Maps the public
 * domain records to and from the generated {@code *Raw} DTOs and drives the
 * shared transport.
 *
 * @since 0.2.0
 */
public final class FulfillmentImpl implements Fulfillment {

    private static final String OP_GET_REMOVAL_PREFERENCE = "get fulfillment removal preference";
    private static final String OP_SET_REMOVAL_PREFERENCE = "set fulfillment removal preference";
    private static final String ERR_PREFERENCE_NULL = "preference must not be null";

    private final HttpSupport http;

    public FulfillmentImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public RemovalPreference removalPreference() {
        return RemovalPreference.from(
                http.getAuthenticated(ApiPaths.FULFILLMENT_REMOVAL_PREFERENCES,
                        FulfillmentRemovalPreferenceRaw.class, OP_GET_REMOVAL_PREFERENCE));
    }

    @Override
    public RemovalPreference setRemovalPreference(RemovalPreference preference) {
        Objects.requireNonNull(preference, ERR_PREFERENCE_NULL);
        return RemovalPreference.from(
                http.putJsonAuthenticated(ApiPaths.FULFILLMENT_REMOVAL_PREFERENCES,
                        toRaw(preference), FulfillmentRemovalPreferenceRaw.class,
                        OP_SET_REMOVAL_PREFERENCE));
    }

    private static FulfillmentRemovalPreferenceRaw toRaw(RemovalPreference preference) {
        FulfillmentRemovalPreferenceRaw raw = new FulfillmentRemovalPreferenceRaw();
        raw.setOperation(FulfillmentRemovalPreferenceRaw.OperationEnum
                .fromValue(preference.operation().wireValue()));
        raw.setAddress(toRaw(preference.withdrawalAddress()));
        return raw;
    }

    private static @Nullable FulfillmentWithdrawalAddressRaw toRaw(@Nullable WithdrawalAddress address) {
        if (address == null) {
            return null;
        }
        FulfillmentWithdrawalAddressRaw raw = new FulfillmentWithdrawalAddressRaw();
        raw.setCompany(address.company());
        raw.setStreet(address.street());
        raw.setPostalCode(address.postalCode());
        raw.setCity(address.city());
        raw.setCountryCode(address.countryCode());
        raw.setPhone(toRaw(address.phone()));
        raw.setAdditionalInfo(address.additionalInfo());
        return raw;
    }

    private static PhoneNumberWithCountryCodeRaw toRaw(PhoneNumber phone) {
        PhoneNumberWithCountryCodeRaw raw = new PhoneNumberWithCountryCodeRaw();
        raw.setCountryCode(phone.countryCode());
        raw.setNumber(phone.number());
        return raw;
    }
}
