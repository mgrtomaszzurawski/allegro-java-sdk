/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import org.jspecify.annotations.Nullable;

/**
 * Immutable, validated request for creating or updating a seller warranty
 * definition. Build one with {@link #builder()}; required fields ({@code name},
 * {@code type}) are enforced fail-fast at {@link WarrantyRequestBuilder#build()}.
 *
 * @since 0.2.0
 */
public final class WarrantyRequest {

    private final String name;
    private final WarrantyType type;
    private final @Nullable WarrantyPeriod individual;
    private final @Nullable WarrantyPeriod corporate;
    private final @Nullable String attachmentId;
    private final @Nullable String attachmentName;
    private final @Nullable String description;

    WarrantyRequest(String name, WarrantyType type, @Nullable WarrantyPeriod individual,
            @Nullable WarrantyPeriod corporate, @Nullable String attachmentId,
            @Nullable String attachmentName, @Nullable String description) {
        this.name = name;
        this.type = type;
        this.individual = individual;
        this.corporate = corporate;
        this.attachmentId = attachmentId;
        this.attachmentName = attachmentName;
        this.description = description;
    }

    /** A new, empty builder. */
    public static WarrantyRequestBuilder builder() {
        return new WarrantyRequestBuilder();
    }

    /** A builder pre-populated with this request's values. */
    public WarrantyRequestBuilder toBuilder() {
        return new WarrantyRequestBuilder()
                .name(name)
                .type(type)
                .individual(individual)
                .corporate(corporate)
                .attachment(attachmentId, attachmentName)
                .description(description);
    }

    /** Warranty name (dictionary key, visible to buyers). */
    public String name() {
        return name;
    }

    /** Who is the warrantor. */
    public WarrantyType type() {
        return type;
    }

    /** Warranty duration for individual buyers, or {@code null}. */
    public @Nullable WarrantyPeriod individual() {
        return individual;
    }

    /** Warranty duration for corporate buyers, or {@code null}. */
    public @Nullable WarrantyPeriod corporate() {
        return corporate;
    }

    /** Identifier of a previously uploaded warranty document, or {@code null}. */
    public @Nullable String attachmentId() {
        return attachmentId;
    }

    /** File name of the attached warranty document, or {@code null}. */
    public @Nullable String attachmentName() {
        return attachmentName;
    }

    /** Free-text warranty description, or {@code null}. */
    public @Nullable String description() {
        return description;
    }
}
