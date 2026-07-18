/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link ImpliedWarrantyRequest}. Replicates the server
 * constraints the SDK can check cheaply (required {@code name}/{@code individual},
 * name and description length caps) and fails fast at {@link #build()}.
 *
 * <p>The implied-warranty period accepts <strong>whole-year</strong> ISO-8601
 * durations only (e.g. {@code P2Y}); the server owns that check and rejects a
 * sub-year value.
 *
 * @since 0.3.0
 */
public final class ImpliedWarrantyRequestBuilder {

    /** Server cap on the implied-warranty name length. */
    private static final int MAX_NAME_LENGTH = 200;
    /** Server cap on the implied-warranty description length. */
    private static final int MAX_DESCRIPTION_LENGTH = 10_240;

    private static final String ERR_NAME_REQUIRED = "Implied warranty name is required";
    private static final String ERR_NAME_TOO_LONG =
            "Implied warranty name exceeds the " + MAX_NAME_LENGTH + "-character limit";
    private static final String ERR_INDIVIDUAL_REQUIRED =
            "Implied warranty individual period is required";
    private static final String ERR_DESCRIPTION_TOO_LONG =
            "Implied warranty description exceeds the " + MAX_DESCRIPTION_LENGTH + "-character limit";

    private @Nullable String name;
    private @Nullable ImpliedWarrantyPeriod individual;
    private @Nullable ImpliedWarrantyPeriod corporate;
    private @Nullable AfterSalesAddress address;
    private @Nullable String description;

    ImpliedWarrantyRequestBuilder() {
    }

    /** Set the implied-warranty name (required, at most 200 characters). */
    public ImpliedWarrantyRequestBuilder name(@Nullable String warrantyName) {
        this.name = warrantyName;
        return this;
    }

    /** Set the claim period for individual buyers (required, whole years). */
    public ImpliedWarrantyRequestBuilder individual(@Nullable ImpliedWarrantyPeriod period) {
        this.individual = period;
        return this;
    }

    /** Set the claim period for corporate buyers (whole years). */
    public ImpliedWarrantyRequestBuilder corporate(@Nullable ImpliedWarrantyPeriod period) {
        this.corporate = period;
        return this;
    }

    /** Set the address for buyer claims. */
    public ImpliedWarrantyRequestBuilder address(@Nullable AfterSalesAddress claimAddress) {
        this.address = claimAddress;
        return this;
    }

    /** Set the free-text description (at most 10240 characters). */
    public ImpliedWarrantyRequestBuilder description(@Nullable String warrantyDescription) {
        this.description = warrantyDescription;
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if a required field is missing or a length
     *     constraint is violated
     */
    public ImpliedWarrantyRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (individual == null) {
            throw new IllegalStateException(ERR_INDIVIDUAL_REQUIRED);
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_TOO_LONG);
        }
        return new ImpliedWarrantyRequest(name, individual, corporate, address, description);
    }
}
