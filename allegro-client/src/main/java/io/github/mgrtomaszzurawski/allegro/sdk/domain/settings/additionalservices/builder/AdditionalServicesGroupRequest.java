/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalServicesGroupRequestRaw;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A create/update request for an additional-services group
 * ({@code settings().additionalServices().createGroup(...)} /
 * {@code updateGroup(groupId, ...)}). A group needs a {@code name} and at least one
 * {@link AdditionalServiceRequest service}; the {@code language} localizes it.
 *
 * <pre>{@code
 * AdditionalServicesGroup group = settings.additionalServices().createGroup(
 *         AdditionalServicesGroupRequest.builder()
 *                 .name("Assembly & delivery")
 *                 .language("pl-PL")
 *                 .addService(AdditionalServiceRequest.of("ASSEMBLY", "On-site assembly",
 *                         ServiceConfigurationRequest.of(Money.of("49.00", "PLN"))))
 *                 .build());
 * }</pre>
 *
 * @since 0.3.0
 */
public final class AdditionalServicesGroupRequest {

    private static final String ERR_NAME = "name must not be blank";
    private static final String ERR_NO_SERVICES = "a group needs at least one additional service";

    private final String name;
    private final @Nullable String language;
    private final List<AdditionalServiceRequest> services;

    private AdditionalServicesGroupRequest(Builder builder) {
        this.name = builder.name;
        this.language = builder.language;
        this.services = List.copyOf(builder.services);
    }

    /** The group name. */
    public String name() {
        return name;
    }

    /** The group language, or {@code null} for the account default. */
    public @Nullable String language() {
        return language;
    }

    /** The services in the group. */
    public List<AdditionalServiceRequest> services() {
        return services;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Project onto the generated Layer-1 request DTO. */
    public AdditionalServicesGroupRequestRaw toRaw() {
        AdditionalServicesGroupRequestRaw raw = new AdditionalServicesGroupRequestRaw();
        raw.setName(name);
        if (language != null) {
            raw.setLanguage(language);
        }
        raw.setAdditionalServices(services.stream().map(AdditionalServiceRequest::toRaw).toList());
        return raw;
    }

    /** Fluent fail-fast builder for {@link AdditionalServicesGroupRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private @Nullable String language;
        private final List<AdditionalServiceRequest> services = new ArrayList<>();

        /** The group name (required). */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Localize the group to this language (e.g. {@code pl-PL}). */
        public Builder language(@Nullable String language) {
            this.language = language;
            return this;
        }

        /** Add one service to the group. */
        public Builder addService(AdditionalServiceRequest service) {
            this.services.add(service);
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if {@code name} is blank or no service is added
         */
        public AdditionalServicesGroupRequest build() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException(ERR_NAME);
            }
            if (services.isEmpty()) {
                throw new IllegalStateException(ERR_NO_SERVICES);
            }
            return new AdditionalServicesGroupRequest(this);
        }
    }
}
