/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 device grant — a user-context token for console and headless
 * applications. On the first token acquisition the SDK starts the device flow
 * and hands a {@link DeviceAuthorization} to {@link #userPrompt()}; the
 * application shows the verification link to the user, the user confirms once
 * in any browser, and the SDK — which polls the token endpoint in the
 * background — resumes automatically. Subsequent sessions can skip the prompt
 * entirely by passing the previously issued {@code refreshToken}.
 *
 * @param clientId OAuth2 client id of the registered application
 * @param clientSecret OAuth2 client secret; never logged
 * @param userPrompt callback that presents the verification link to the user
 * @param refreshToken refresh token from a previous session (skips the
 *     interactive prompt), or {@code null} on first use
 *
 * @since 0.1.0
 */
public record DeviceCodeCredentials(
        String clientId,
        String clientSecret,
        Consumer<DeviceAuthorization> userPrompt,
        @Nullable String refreshToken) implements AllegroCredentials {

    private static final String ERR_CLIENT_ID_NULL = "clientId must not be null";
    private static final String ERR_CLIENT_SECRET_NULL = "clientSecret must not be null";
    private static final String ERR_USER_PROMPT_NULL = "userPrompt must not be null";

    public DeviceCodeCredentials {
        Objects.requireNonNull(clientId, ERR_CLIENT_ID_NULL);
        Objects.requireNonNull(clientSecret, ERR_CLIENT_SECRET_NULL);
        Objects.requireNonNull(userPrompt, ERR_USER_PROMPT_NULL);
    }

    /** First-time flow: the user will be prompted at a verification URI. */
    public static DeviceCodeCredentials of(
            String clientId, String clientSecret, Consumer<DeviceAuthorization> userPrompt) {
        return new DeviceCodeCredentials(clientId, clientSecret, userPrompt, null);
    }

    /**
     * Returning user: restore from a stored refresh token; {@code userPrompt}
     * fires only if the stored token turns out to be revoked or expired.
     */
    public static DeviceCodeCredentials ofRefreshToken(
            String clientId, String clientSecret, Consumer<DeviceAuthorization> userPrompt,
            String refreshToken) {
        return new DeviceCodeCredentials(clientId, clientSecret, userPrompt,
                Objects.requireNonNull(refreshToken, "refreshToken must not be null"));
    }

    /** Redacts credential material — only the client id is safe to print. */
    @Override
    public String toString() {
        return "DeviceCodeCredentials[clientId=" + clientId + ", clientSecret=***"
                + (refreshToken != null ? ", refreshToken=***" : "") + "]";
    }
}
