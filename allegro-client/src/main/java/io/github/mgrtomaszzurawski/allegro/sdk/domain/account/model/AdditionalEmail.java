/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AdditionalEmailRaw;

/**
 * One additional e-mail address registered on the authenticated account, as
 * returned by {@code UserAccount.additionalEmails()}.
 *
 * @param id server-assigned identifier of the entry
 * @param email the e-mail address
 * @param createdAt creation timestamp, ISO-8601 as returned by Allegro
 *
 * @since 0.2.0
 */
public record AdditionalEmail(String id, String email, String createdAt) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static AdditionalEmail from(AdditionalEmailRaw raw) {
        return new AdditionalEmail(raw.getId(), raw.getEmail(), raw.getCreatedAt());
    }
}
