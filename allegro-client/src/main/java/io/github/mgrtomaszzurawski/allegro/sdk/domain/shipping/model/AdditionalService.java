/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceDtoRaw;
import org.jspecify.annotations.Nullable;

/**
 * An optional carrier service a delivery option offers (for example a Saturday
 * delivery or a phone-call-before-delivery). Read-only: it appears only in a
 * delivery proposal's options; its {@code id} is what a shipment request would
 * reference.
 *
 * @param id the service identifier, or {@code null}
 * @param name the human-readable service name, or {@code null}
 * @param description the service description, or {@code null}
 *
 * @since 0.5.0
 */
public record AdditionalService(
        @Nullable String id,
        @Nullable String name,
        @Nullable String description) {

    /** Map the generated DTO. */
    public static AdditionalService from(AdditionalServiceDtoRaw raw) {
        return new AdditionalService(raw.getId(), raw.getName(), raw.getDescription());
    }
}
