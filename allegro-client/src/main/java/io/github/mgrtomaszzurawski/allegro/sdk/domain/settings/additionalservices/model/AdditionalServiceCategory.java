/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CategoryDefinitionResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CategoryResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A category of additional-service definitions available to the seller (e.g.
 * "Installation/assembly services") and the definitions it groups.
 *
 * @param name category name, or {@code null}
 * @param definitions the definitions in this category
 *
 * @since 0.3.0
 */
public record AdditionalServiceCategory(
        @Nullable String name,
        List<AdditionalServiceDefinition> definitions) {

    /** Canonical constructor — defensively copies the definitions. */
    public AdditionalServiceCategory {
        definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    /** Map the generated Layer-1 DTO. */
    public static AdditionalServiceCategory from(CategoryResponseRaw raw) {
        List<CategoryDefinitionResponseRaw> rawDefinitions =
                raw.getDefinitions() == null ? List.of() : raw.getDefinitions();
        List<AdditionalServiceDefinition> definitions =
                rawDefinitions.stream().map(AdditionalServiceDefinition::from).toList();
        return new AdditionalServiceCategory(raw.getName(), definitions);
    }
}
