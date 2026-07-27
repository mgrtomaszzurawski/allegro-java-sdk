/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ConfigurationRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One additional service inside a group: the definition it instantiates (e.g.
 * {@code GIFT_WRAP}), a merchant description shown to buyers, and its priced
 * configurations.
 *
 * @param definitionId id of the additional-service definition, or {@code null}
 * @param description merchant description shown to buyers, or {@code null}
 * @param configurations the priced configurations of this service
 *
 * @since 0.3.0
 */
public record AdditionalService(
        @Nullable String definitionId,
        @Nullable String description,
        List<ServiceConfiguration> configurations) {

    /** Canonical constructor — defensively copies the configurations. */
    public AdditionalService {
        configurations = configurations == null ? List.of() : List.copyOf(configurations);
    }

    /** Map the generated Layer-1 DTO. */
    public static AdditionalService from(AdditionalServiceResponseRaw raw) {
        String definitionId = raw.getDefinition() == null ? null : raw.getDefinition().getId();
        List<ConfigurationRaw> rawConfigurations =
                raw.getConfigurations() == null ? List.of() : raw.getConfigurations();
        List<ServiceConfiguration> configurations =
                rawConfigurations.stream().map(ServiceConfiguration::from).toList();
        return new AdditionalService(definitionId, raw.getDescription(), configurations);
    }
}
