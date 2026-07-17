/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Basic profile of the authenticated user, as returned by
 * {@code UserAccount.me()}.
 *
 * @param id numeric user identifier (as a string, per the Allegro contract)
 * @param login public login name
 * @param firstName first name, or {@code null} when the account has none set
 * @param lastName last name, or {@code null} when the account has none set
 * @param email account e-mail address
 * @param features account feature flags; never {@code null}, possibly empty
 *
 * @since 0.1.0
 */
public record CurrentUser(
        String id,
        String login,
        @Nullable String firstName,
        @Nullable String lastName,
        String email,
        List<String> features) {

    public CurrentUser {
        features = List.copyOf(features);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static CurrentUser from(MeResponseRaw raw) {
        List<String> rawFeatures = raw.getFeatures();
        return new CurrentUser(
                raw.getId(),
                raw.getLogin(),
                raw.getFirstName(),
                raw.getLastName(),
                raw.getEmail(),
                rawFeatures == null ? List.of() : rawFeatures);
    }
}
