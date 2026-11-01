/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceDefinitionRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceRequestRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One additional service in a group create/update request: the service
 * {@code definitionId} (from {@code additionalServices().categoryDefinitions()}), an
 * optional seller {@code description}, and at least one priced {@code configuration}
 * — Allegro refuses a service without one ({@code VALIDATION_ERROR "must not be
 * empty"}, live-verified 2026-11-01), so the record fails fast instead.
 *
 * @param definitionId the service-definition id
 * @param description the seller's description of the service, or {@code null}
 * @param configurations the priced configurations (at least one)
 * @since 0.3.0
 */
public record AdditionalServiceRequest(
        String definitionId,
        @Nullable String description,
        List<ServiceConfigurationRequest> configurations) {

    private static final String ERR_CONFIGURATIONS_REQUIRED =
            "configurations must hold at least one priced configuration";

    /** Rejects a null definition id or an empty configuration list; defensively copies the list. */
    public AdditionalServiceRequest {
        Objects.requireNonNull(definitionId, "definitionId");
        configurations = configurations == null ? List.of() : List.copyOf(configurations);
        if (configurations.isEmpty()) {
            throw new IllegalArgumentException(ERR_CONFIGURATIONS_REQUIRED);
        }
    }

    /** A service with a description and at least one priced configuration. */
    public static AdditionalServiceRequest of(
            String definitionId, @Nullable String description, ServiceConfigurationRequest... configurations) {
        return new AdditionalServiceRequest(definitionId, description, List.of(configurations));
    }

    /** Project onto the generated Layer-1 request DTO. */
    public AdditionalServiceRequestRaw toRaw() {
        AdditionalServiceDefinitionRequestRaw definition = new AdditionalServiceDefinitionRequestRaw();
        definition.setId(definitionId);
        AdditionalServiceRequestRaw raw = new AdditionalServiceRequestRaw();
        raw.setDefinition(definition);
        if (description != null) {
            raw.setDescription(description);
        }
        raw.setConfigurations(configurations.stream().map(ServiceConfigurationRequest::toRaw).toList());
        return raw;
    }
}
