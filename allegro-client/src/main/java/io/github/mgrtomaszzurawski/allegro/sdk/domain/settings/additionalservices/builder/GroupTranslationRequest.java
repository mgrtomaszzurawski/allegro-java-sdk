/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceDefinitionRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServiceTranslationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupTranslationRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupTranslationWrapperRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A translation upsert for an additional-services group in one language
 * ({@code settings().additionalServices().upsertTranslation(groupId, language, ...)}):
 * per-service translated descriptions, each keyed by the service
 * {@code definitionId}.
 *
 * @since 0.3.0
 */
public final class GroupTranslationRequest {

    private static final String ERR_EMPTY = "a translation must carry at least one service description";

    private final List<ServiceTranslation> translations;

    private GroupTranslationRequest(Builder builder) {
        this.translations = List.copyOf(builder.translations);
    }

    /** The per-service translated descriptions. */
    public List<ServiceTranslation> translations() {
        return translations;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Project onto the generated Layer-1 request DTO. */
    public AdditionalServicesGroupTranslationRequestRaw toRaw() {
        AdditionalServicesGroupTranslationWrapperRaw wrapper =
                new AdditionalServicesGroupTranslationWrapperRaw();
        wrapper.setTranslation(translations.stream().map(ServiceTranslation::toRaw).toList());
        AdditionalServicesGroupTranslationRequestRaw raw =
                new AdditionalServicesGroupTranslationRequestRaw();
        raw.setAdditionalServices(wrapper);
        return raw;
    }

    /**
     * One service's translated description, keyed by the service definition id.
     *
     * @param definitionId the service-definition id
     * @param description the translated description
     * @since 0.3.0
     */
    public record ServiceTranslation(String definitionId, String description) {

        /** Rejects null components. */
        public ServiceTranslation {
            Objects.requireNonNull(definitionId, "definitionId");
            Objects.requireNonNull(description, "description");
        }

        private AdditionalServiceTranslationRaw toRaw() {
            AdditionalServiceDefinitionRequestRaw definition = new AdditionalServiceDefinitionRequestRaw();
            definition.setId(definitionId);
            AdditionalServiceTranslationRaw raw = new AdditionalServiceTranslationRaw();
            raw.setDefinition(definition);
            raw.setDescription(description);
            return raw;
        }
    }

    /** Fluent fail-fast builder for {@link GroupTranslationRequest}. */
    public static final class Builder {

        private final List<ServiceTranslation> translations = new ArrayList<>();

        /** Add one service's translated description. */
        public Builder addTranslation(String definitionId, String description) {
            this.translations.add(new ServiceTranslation(definitionId, description));
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if no service description is added
         */
        public GroupTranslationRequest build() {
            if (translations.isEmpty()) {
                throw new IllegalStateException(ERR_EMPTY);
            }
            return new GroupTranslationRequest(this);
        }
    }
}
