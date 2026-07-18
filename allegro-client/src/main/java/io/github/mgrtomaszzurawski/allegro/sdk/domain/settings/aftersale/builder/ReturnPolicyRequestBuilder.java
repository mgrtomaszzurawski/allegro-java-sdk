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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRange;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link ReturnPolicyRequest}. Enforces the required
 * {@code name} (with the length cap), {@code fulfillment} flag and
 * {@code availability} fail-fast at {@link #build()}; {@code options} is
 * required whenever the availability range is not {@code DISABLED}
 * (live-verified — the server rejects an enabled policy without options). The
 * remaining fields are optional and the server owns their finer validation.
 *
 * @since 0.3.0
 */
public final class ReturnPolicyRequestBuilder {

    /** Server cap on the return-policy name length. */
    private static final int MAX_NAME_LENGTH = 200;

    private static final String ERR_NAME_REQUIRED = "Return policy name is required";
    private static final String ERR_NAME_TOO_LONG =
            "Return policy name exceeds the " + MAX_NAME_LENGTH + "-character limit";
    private static final String ERR_FULFILLMENT_REQUIRED =
            "Return policy fulfillment flag is required (fixed at creation)";
    private static final String ERR_AVAILABILITY_REQUIRED = "Return policy availability is required";
    private static final String ERR_OPTIONS_REQUIRED =
            "Return policy options are required when the availability range is not DISABLED";

    private @Nullable String name;
    private @Nullable Boolean fulfillment;
    private @Nullable ReturnPolicyAvailability availability;
    private @Nullable String withdrawalPeriod;
    private @Nullable ReturnCostCoveredBy returnCost;
    private @Nullable AfterSalesAddress address;
    private @Nullable ReturnPolicyContact contact;
    private @Nullable ReturnPolicyOptions options;

    ReturnPolicyRequestBuilder() {
    }

    /** Set the return-policy name (required, at most 200 characters). */
    public ReturnPolicyRequestBuilder name(@Nullable String policyName) {
        this.name = policyName;
        return this;
    }

    /** Set whether the policy is for One Fulfillment offers (required, fixed at creation). */
    public ReturnPolicyRequestBuilder fulfillment(@Nullable Boolean forFulfillment) {
        this.fulfillment = forFulfillment;
        return this;
    }

    /** Set the return availability (required). */
    public ReturnPolicyRequestBuilder availability(@Nullable ReturnPolicyAvailability policyAvailability) {
        this.availability = policyAvailability;
        return this;
    }

    /** Set the ISO-8601 withdrawal period (whole days, e.g. {@code P14D}). */
    public ReturnPolicyRequestBuilder withdrawalPeriod(@Nullable String period) {
        this.withdrawalPeriod = period;
        return this;
    }

    /** Set who covers the return delivery cost. */
    public ReturnPolicyRequestBuilder returnCost(@Nullable ReturnCostCoveredBy coveredBy) {
        this.returnCost = coveredBy;
        return this;
    }

    /** Set the return address. */
    public ReturnPolicyRequestBuilder address(@Nullable AfterSalesAddress returnAddress) {
        this.address = returnAddress;
        return this;
    }

    /** Set the seller contact details. */
    public ReturnPolicyRequestBuilder contact(@Nullable ReturnPolicyContact sellerContact) {
        this.contact = sellerContact;
        return this;
    }

    /** Set the boolean return-handling options. */
    public ReturnPolicyRequestBuilder options(@Nullable ReturnPolicyOptions returnOptions) {
        this.options = returnOptions;
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if a required field is missing or the name
     *     length constraint is violated
     */
    public ReturnPolicyRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (fulfillment == null) {
            throw new IllegalStateException(ERR_FULFILLMENT_REQUIRED);
        }
        if (availability == null) {
            throw new IllegalStateException(ERR_AVAILABILITY_REQUIRED);
        }
        if (availability.range() != ReturnRange.DISABLED && options == null) {
            throw new IllegalStateException(ERR_OPTIONS_REQUIRED);
        }
        return new ReturnPolicyRequest(name, fulfillment, availability, withdrawalPeriod,
                returnCost, address, contact, options);
    }
}
