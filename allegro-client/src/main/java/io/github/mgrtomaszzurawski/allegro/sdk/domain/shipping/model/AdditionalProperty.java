/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalPropertyDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * A carrier-specific property a delivery option exposes (for example a required
 * reference field). Read-only: it appears only in a delivery proposal's options.
 *
 * @param id the property identifier, or {@code null}
 * @param name the human-readable property name, or {@code null}
 * @param description the property description, or {@code null}
 * @param required whether the carrier requires the property, or {@code null} if unstated
 * @param readOnly whether the property is read-only, or {@code null} if unstated
 *
 * @since 0.5.0
 */
public record AdditionalProperty(
        @Nullable String id,
        @Nullable String name,
        @Nullable String description,
        @Nullable Boolean required,
        @Nullable Boolean readOnly) {

    /** Map the generated DTO. */
    public static AdditionalProperty from(AdditionalPropertyDtoRaw raw) {
        return new AdditionalProperty(
                raw.getId(),
                raw.getName(),
                raw.getDescription(),
                raw.getRequired(),
                raw.getReadOnly());
    }
}
