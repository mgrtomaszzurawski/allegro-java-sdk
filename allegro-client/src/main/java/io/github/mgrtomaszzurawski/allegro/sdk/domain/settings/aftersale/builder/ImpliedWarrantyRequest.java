/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import org.jspecify.annotations.Nullable;

/**
 * Immutable, validated request for creating or updating an implied-warranty
 * (rękojmia) definition. Build one with {@link #builder()}; required fields
 * ({@code name}, {@code individual}) are enforced fail-fast at
 * {@link ImpliedWarrantyRequestBuilder#build()}.
 *
 * @since 0.3.0
 */
public final class ImpliedWarrantyRequest {

    private final String name;
    private final ImpliedWarrantyPeriod individual;
    private final @Nullable ImpliedWarrantyPeriod corporate;
    private final @Nullable AfterSalesAddress address;
    private final @Nullable String description;

    ImpliedWarrantyRequest(String name, ImpliedWarrantyPeriod individual,
            @Nullable ImpliedWarrantyPeriod corporate, @Nullable AfterSalesAddress address,
            @Nullable String description) {
        this.name = name;
        this.individual = individual;
        this.corporate = corporate;
        this.address = address;
        this.description = description;
    }

    /** A new, empty builder. */
    public static ImpliedWarrantyRequestBuilder builder() {
        return new ImpliedWarrantyRequestBuilder();
    }

    /** A builder pre-populated with this request's values. */
    public ImpliedWarrantyRequestBuilder toBuilder() {
        return new ImpliedWarrantyRequestBuilder()
                .name(name)
                .individual(individual)
                .corporate(corporate)
                .address(address)
                .description(description);
    }

    /** Implied-warranty name (dictionary key, visible to buyers). */
    public String name() {
        return name;
    }

    /** Claim period for individual buyers. */
    public ImpliedWarrantyPeriod individual() {
        return individual;
    }

    /** Claim period for corporate buyers, or {@code null}. */
    public @Nullable ImpliedWarrantyPeriod corporate() {
        return corporate;
    }

    /** Address for buyer claims, or {@code null}. */
    public @Nullable AfterSalesAddress address() {
        return address;
    }

    /** Free-text implied-warranty description, or {@code null}. */
    public @Nullable String description() {
        return description;
    }
}
