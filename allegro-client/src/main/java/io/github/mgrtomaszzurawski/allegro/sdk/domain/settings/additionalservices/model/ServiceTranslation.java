/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceTranslationRaw;
import org.jspecify.annotations.Nullable;

/**
 * A translated description of one additional service within a group translation.
 *
 * @param definitionId id of the additional-service definition, or {@code null}
 * @param description the translated, buyer-visible description, or {@code null}
 *
 * @since 0.3.0
 */
public record ServiceTranslation(
        @Nullable String definitionId,
        @Nullable String description) {

    /** Map the generated Layer-1 DTO. */
    public static ServiceTranslation from(AdditionalServiceTranslationRaw raw) {
        String definitionId = raw.getDefinition() == null ? null : raw.getDefinition().getId();
        return new ServiceTranslation(definitionId, raw.getDescription());
    }
}
