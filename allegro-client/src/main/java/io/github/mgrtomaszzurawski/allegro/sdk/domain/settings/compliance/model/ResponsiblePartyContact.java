/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ResponsiblePersonContactRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ResponsibleProducerContactRaw;
import org.jspecify.annotations.Nullable;

/**
 * Contact channel of a product-compliance responsible party.
 *
 * <p>The wire contract requires <strong>at least one</strong> of {@code email} or
 * {@code formUrl}; that write-side rule is enforced fail-fast by the request
 * builders (not here) so response mapping never rejects a server payload.
 * {@code phoneNumber} is optional.
 *
 * @param email contact e-mail (or {@code null} when only a form URL is given)
 * @param phoneNumber optional phone number
 * @param formUrl URL of a contact form (or {@code null} when only an e-mail is given)
 *
 * @since 0.3.0
 */
public record ResponsiblePartyContact(
        @Nullable String email,
        @Nullable String phoneNumber,
        @Nullable String formUrl) {

    /** Map the generated person-contact DTO, or {@code null} when absent. */
    public static @Nullable ResponsiblePartyContact from(@Nullable ResponsiblePersonContactRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ResponsiblePartyContact(raw.getEmail(), raw.getPhoneNumber(), raw.getFormUrl());
    }

    /** Map the generated producer-contact DTO, or {@code null} when absent. */
    public static @Nullable ResponsiblePartyContact from(@Nullable ResponsibleProducerContactRaw raw) {
        if (raw == null) {
            return null;
        }
        return new ResponsiblePartyContact(raw.getEmail(), raw.getPhoneNumber(), raw.getFormUrl());
    }
}
