/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupResponseRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A seller's additional-services group: a merchant-named (buyer-invisible) set of
 * additional services offered on a marketplace language.
 *
 * @param id server-assigned identifier, or {@code null}
 * @param name merchant name (invisible to buyers), or {@code null}
 * @param language IETF language tag of the group, or {@code null}
 * @param managedByAllegro {@code true} when Allegro auto-created and manages the group
 * @param sellerId owning seller id, or {@code null}
 * @param services the services in the group
 * @param createdAt when the group was created, or {@code null}
 * @param updatedAt when the group was last updated, or {@code null}
 *
 * @since 0.3.0
 */
public record AdditionalServicesGroup(
        @Nullable String id,
        @Nullable String name,
        @Nullable String language,
        boolean managedByAllegro,
        @Nullable String sellerId,
        List<AdditionalService> services,
        @Nullable OffsetDateTime createdAt,
        @Nullable OffsetDateTime updatedAt) {

    /** Canonical constructor — defensively copies the services. */
    public AdditionalServicesGroup {
        services = services == null ? List.of() : List.copyOf(services);
    }

    /** Map the generated Layer-1 DTO. */
    public static AdditionalServicesGroup from(AdditionalServicesGroupResponseRaw raw) {
        List<AdditionalServiceResponseRaw> rawServices =
                raw.getAdditionalServices() == null ? List.of() : raw.getAdditionalServices();
        List<AdditionalService> services = rawServices.stream().map(AdditionalService::from).toList();
        String sellerId = raw.getSeller() == null ? null : raw.getSeller().getId();
        return new AdditionalServicesGroup(
                raw.getId(), raw.getName(), raw.getLanguage(),
                Boolean.TRUE.equals(raw.getManagedByAllegro()), sellerId, services,
                raw.getCreatedAt(), raw.getUpdatedAt());
    }
}
