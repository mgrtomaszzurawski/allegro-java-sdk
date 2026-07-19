/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.jspecify.annotations.Nullable;

/**
 * Immutable create/update request for a compliance {@code ResponsibleProducer}.
 * Build via {@link #builder()}; the builder validates required fields fail-fast.
 *
 * @param name internal dictionary label (required, at most 50 characters)
 * @param tradeName the producing company's name or trade name (at most 200 characters)
 * @param address the producer's address
 * @param contact the producer's contact
 *
 * @since 0.3.0
 */
public record ResponsibleProducerRequest(
        String name,
        @Nullable String tradeName,
        @Nullable ResponsiblePartyAddress address,
        @Nullable ResponsiblePartyContact contact) {

    /** Start a new request builder. */
    public static ResponsibleProducerRequestBuilder builder() {
        return new ResponsibleProducerRequestBuilder();
    }

    /** A builder pre-filled from this request. */
    public ResponsibleProducerRequestBuilder toBuilder() {
        return new ResponsibleProducerRequestBuilder()
                .name(name)
                .tradeName(tradeName)
                .address(address)
                .contact(contact);
    }
}
