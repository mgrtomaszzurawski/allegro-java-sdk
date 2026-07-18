/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyContactV1Raw;
import org.jspecify.annotations.Nullable;

/**
 * Seller contact details published with a return policy.
 *
 * @param phoneNumber seller phone number, or {@code null} when not provided
 * @param email seller email address, or {@code null} when not provided
 *
 * @since 0.3.0
 */
public record ReturnPolicyContact(@Nullable String phoneNumber, @Nullable String email) {

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ReturnPolicyContact from(@Nullable ReturnPolicyContactV1Raw raw) {
        if (raw == null) {
            return null;
        }
        return new ReturnPolicyContact(raw.getPhoneNumber(), raw.getEmail());
    }
}
