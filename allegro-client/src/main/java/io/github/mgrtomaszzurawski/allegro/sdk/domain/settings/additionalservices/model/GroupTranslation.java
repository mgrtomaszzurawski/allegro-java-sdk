/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceGroupTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupTranslationWrapperWithTypeRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A group's translations for one language: whether they are {@code MANUAL} or
 * {@code AUTO} and the per-service translated descriptions.
 *
 * @param language IETF language tag, or {@code null}
 * @param type whether the translations are merchant-provided or Allegro-generated, or {@code null}
 * @param services the per-service translated descriptions
 *
 * @since 0.3.0
 */
public record GroupTranslation(
        @Nullable String language,
        @Nullable TranslationType type,
        List<ServiceTranslation> services) {

    /** Canonical constructor — defensively copies the service translations. */
    public GroupTranslation {
        services = services == null ? List.of() : List.copyOf(services);
    }

    /** Map the generated Layer-1 DTO. */
    public static GroupTranslation from(AdditionalServiceGroupTranslationRaw raw) {
        AdditionalServicesGroupTranslationWrapperWithTypeRaw wrapper = raw.getAdditionalServices();
        TranslationType type = null;
        List<ServiceTranslation> services = List.of();
        if (wrapper != null) {
            type = wrapper.getType() == null ? null : TranslationType.from(wrapper.getType());
            List<AdditionalServiceTranslationRaw> rawServices =
                    wrapper.getTranslation() == null ? List.of() : wrapper.getTranslation();
            services = rawServices.stream().map(ServiceTranslation::from).toList();
        }
        return new GroupTranslation(raw.getLanguage(), type, services);
    }
}
