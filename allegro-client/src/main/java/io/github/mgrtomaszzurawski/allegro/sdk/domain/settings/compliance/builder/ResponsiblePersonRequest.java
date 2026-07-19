/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.jspecify.annotations.Nullable;

/**
 * Immutable create/update request for a compliance {@code ResponsiblePerson}.
 * Build via {@link #builder()}; the builder validates required fields fail-fast.
 *
 * @param name internal dictionary label (required, at most 50 characters)
 * @param personName the responsible person's name (at most 200 characters)
 * @param address the responsible person's address
 * @param contact the responsible person's contact
 *
 * @since 0.3.0
 */
public record ResponsiblePersonRequest(
        String name,
        @Nullable String personName,
        @Nullable ResponsiblePartyAddress address,
        @Nullable ResponsiblePartyContact contact) {

    /** Start a new request builder. */
    public static ResponsiblePersonRequestBuilder builder() {
        return new ResponsiblePersonRequestBuilder();
    }

    /** A builder pre-filled from this request. */
    public ResponsiblePersonRequestBuilder toBuilder() {
        return new ResponsiblePersonRequestBuilder()
                .name(name)
                .personName(personName)
                .address(address)
                .contact(contact);
    }
}
