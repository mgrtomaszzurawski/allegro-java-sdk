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
 * optional seller {@code description}, and its priced {@code configurations}.
 *
 * @param definitionId the service-definition id
 * @param description the seller's description of the service, or {@code null}
 * @param configurations the priced configurations (possibly empty)
 * @since 0.3.0
 */
public record AdditionalServiceRequest(
        String definitionId,
        @Nullable String description,
        List<ServiceConfigurationRequest> configurations) {

    /** Rejects a null definition id and defensively copies the configurations. */
    public AdditionalServiceRequest {
        Objects.requireNonNull(definitionId, "definitionId");
        configurations = configurations == null ? List.of() : List.copyOf(configurations);
    }

    /** A service referencing a definition, with no description or configurations. */
    public static AdditionalServiceRequest of(String definitionId) {
        return new AdditionalServiceRequest(definitionId, null, List.of());
    }

    /** A service with a description and priced configurations. */
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
        if (!configurations.isEmpty()) {
            raw.setConfigurations(configurations.stream().map(ServiceConfigurationRequest::toRaw).toList());
        }
        return raw;
    }
}
