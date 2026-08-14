/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerContactRaw;
import org.jspecify.annotations.Nullable;

/**
 * The contact details of a product-safety {@link ResponsibleProducer} (GPSR).
 *
 * @param email the contact e-mail, or {@code null}
 * @param phoneNumber the contact phone number, or {@code null}
 * @param formUrl the contact-form URL, or {@code null}
 *
 * @since 0.4.0
 */
public record ProducerContact(
        @Nullable String email,
        @Nullable String phoneNumber,
        @Nullable String formUrl) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ProducerContact from(@Nullable ResponsibleProducerContactRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ProducerContact(raw.getEmail(), raw.getPhoneNumber(), raw.getFormUrl());
    }
}
