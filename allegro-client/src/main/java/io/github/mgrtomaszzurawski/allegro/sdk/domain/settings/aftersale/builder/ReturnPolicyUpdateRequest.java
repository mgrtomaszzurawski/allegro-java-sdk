/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnCostCoveredBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyAvailability;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyOptions;
import org.jspecify.annotations.Nullable;

/**
 * Immutable, validated request for updating a return-policy definition. Build one
 * with {@link #builder()}; required fields ({@code name}, {@code availability})
 * are enforced fail-fast at {@link ReturnPolicyUpdateRequestBuilder#build()}.
 *
 * <p>Unlike {@link ReturnPolicyRequest}, there is no {@code fulfillment} flag —
 * the server fixes it at creation and rejects attempts to change it.
 *
 * @since 0.3.0
 */
public final class ReturnPolicyUpdateRequest {

    private final String name;
    private final ReturnPolicyAvailability availability;
    private final @Nullable String withdrawalPeriod;
    private final @Nullable ReturnCostCoveredBy returnCost;
    private final @Nullable AfterSalesAddress address;
    private final @Nullable ReturnPolicyContact contact;
    private final @Nullable ReturnPolicyOptions options;

    ReturnPolicyUpdateRequest(String name, ReturnPolicyAvailability availability,
            @Nullable String withdrawalPeriod, @Nullable ReturnCostCoveredBy returnCost,
            @Nullable AfterSalesAddress address, @Nullable ReturnPolicyContact contact,
            @Nullable ReturnPolicyOptions options) {
        this.name = name;
        this.availability = availability;
        this.withdrawalPeriod = withdrawalPeriod;
        this.returnCost = returnCost;
        this.address = address;
        this.contact = contact;
        this.options = options;
    }

    /** A new, empty builder. */
    public static ReturnPolicyUpdateRequestBuilder builder() {
        return new ReturnPolicyUpdateRequestBuilder();
    }

    /** A builder pre-populated with this request's values. */
    public ReturnPolicyUpdateRequestBuilder toBuilder() {
        return new ReturnPolicyUpdateRequestBuilder()
                .name(name)
                .availability(availability)
                .withdrawalPeriod(withdrawalPeriod)
                .returnCost(returnCost)
                .address(address)
                .contact(contact)
                .options(options);
    }

    /** Return-policy name (dictionary key, visible to buyers). */
    public String name() {
        return name;
    }

    /** Return availability (range and, when restricted, the cause). */
    public ReturnPolicyAvailability availability() {
        return availability;
    }

    /** ISO-8601 withdrawal period (whole days), or {@code null}. */
    public @Nullable String withdrawalPeriod() {
        return withdrawalPeriod;
    }

    /** Who covers the return delivery cost, or {@code null}. */
    public @Nullable ReturnCostCoveredBy returnCost() {
        return returnCost;
    }

    /** Return address, or {@code null}. */
    public @Nullable AfterSalesAddress address() {
        return address;
    }

    /** Seller contact details, or {@code null}. */
    public @Nullable ReturnPolicyContact contact() {
        return contact;
    }

    /** Boolean return-handling options, or {@code null}. */
    public @Nullable ReturnPolicyOptions options() {
        return options;
    }
}
