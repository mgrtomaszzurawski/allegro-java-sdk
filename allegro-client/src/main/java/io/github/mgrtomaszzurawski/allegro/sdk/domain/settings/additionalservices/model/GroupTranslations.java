/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceGroupTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceGroupTranslationResponseRaw;
import java.util.List;

/**
 * All translations defined for an additional-services group, one
 * {@link GroupTranslation} per language.
 *
 * @param translations the per-language translations
 *
 * @since 0.3.0
 */
public record GroupTranslations(List<GroupTranslation> translations) {

    /** Canonical constructor — defensively copies the translations. */
    public GroupTranslations {
        translations = translations == null ? List.of() : List.copyOf(translations);
    }

    /** Map the generated Layer-1 DTO. */
    public static GroupTranslations from(AdditionalServiceGroupTranslationResponseRaw raw) {
        List<AdditionalServiceGroupTranslationRaw> rawTranslations =
                raw.getTranslations() == null ? List.of() : raw.getTranslations();
        return new GroupTranslations(rawTranslations.stream().map(GroupTranslation::from).toList());
    }
}
