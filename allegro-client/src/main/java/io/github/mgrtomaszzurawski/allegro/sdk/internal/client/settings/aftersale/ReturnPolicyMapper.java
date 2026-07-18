/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.settings.aftersale;

import io.github.mgrtomaszzurawski.allegro.client.model.RestrictionCauseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyAvailabilityRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyContactV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyOptionsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyRequestV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyReturnCostRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyUpdateRequestV1Raw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnCostCoveredBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyAvailability;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRestrictionCause;
import org.jspecify.annotations.Nullable;

/**
 * Maps the public return-policy request types onto the generated Layer-1 DTOs.
 * Package-private: request mapping never leaks out of the endpoint-wrapper layer.
 */
final class ReturnPolicyMapper {

    private ReturnPolicyMapper() {
    }

    static ReturnPolicyRequestV1Raw toRaw(ReturnPolicyRequest request) {
        return new ReturnPolicyRequestV1Raw()
                .name(request.name())
                .isFulfillment(request.fulfillment())
                .availability(toRawAvailability(request.availability()))
                .withdrawalPeriod(request.withdrawalPeriod())
                .returnCost(toRawReturnCost(request.returnCost()))
                .address(toRawAddress(request.address()))
                .contact(toRawContact(request.contact()))
                .options(toRawOptions(request.options()));
    }

    static ReturnPolicyUpdateRequestV1Raw toUpdateRaw(ReturnPolicyUpdateRequest request) {
        return new ReturnPolicyUpdateRequestV1Raw()
                .name(request.name())
                .availability(toRawAvailability(request.availability()))
                .withdrawalPeriod(request.withdrawalPeriod())
                .returnCost(toRawReturnCost(request.returnCost()))
                .address(toRawAddress(request.address()))
                .contact(toRawContact(request.contact()))
                .options(toRawOptions(request.options()));
    }

    private static ReturnPolicyAvailabilityRaw toRawAvailability(ReturnPolicyAvailability availability) {
        ReturnPolicyAvailabilityRaw raw = new ReturnPolicyAvailabilityRaw()
                .range(ReturnPolicyAvailabilityRaw.RangeEnum.fromValue(availability.range().name()));
        ReturnRestrictionCause cause = availability.restrictionCause();
        // Domain names equal the wire values; UNKNOWN is a read-only sentinel and
        // has no wire representation, so it is never sent.
        if (cause != null && cause != ReturnRestrictionCause.UNKNOWN) {
            raw.restrictionCause(new RestrictionCauseRaw()
                    .name(RestrictionCauseRaw.NameEnum.fromValue(cause.name())));
        }
        return raw;
    }

    private static @Nullable ReturnPolicyReturnCostRaw toRawReturnCost(@Nullable ReturnCostCoveredBy coveredBy) {
        if (coveredBy == null) {
            return null;
        }
        return new ReturnPolicyReturnCostRaw()
                .coveredBy(ReturnPolicyReturnCostRaw.CoveredByEnum.fromValue(coveredBy.name()));
    }

    private static @Nullable ReturnPolicyAddressRaw toRawAddress(@Nullable AfterSalesAddress address) {
        if (address == null) {
            return null;
        }
        return new ReturnPolicyAddressRaw()
                .name(address.name())
                .street(address.street())
                .postCode(address.postCode())
                .city(address.city())
                .countryCode(address.countryCode());
    }

    private static @Nullable ReturnPolicyContactV1Raw toRawContact(@Nullable ReturnPolicyContact contact) {
        if (contact == null) {
            return null;
        }
        return new ReturnPolicyContactV1Raw()
                .phoneNumber(contact.phoneNumber())
                .email(contact.email());
    }

    private static @Nullable ReturnPolicyOptionsRaw toRawOptions(@Nullable ReturnPolicyOptions options) {
        if (options == null) {
            return null;
        }
        return new ReturnPolicyOptionsRaw()
                .cashOnDeliveryNotAllowed(options.cashOnDeliveryNotAllowed())
                .freeAccessoriesReturnRequired(options.freeAccessoriesReturnRequired())
                .refundLoweredByReceivedDiscount(options.refundLoweredByReceivedDiscount())
                .businessReturnAllowed(options.businessReturnAllowed())
                .collectBySellerOnly(options.collectBySellerOnly());
    }
}
