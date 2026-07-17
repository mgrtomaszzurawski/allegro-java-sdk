/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config.credentials;

import java.time.Duration;
import java.util.Objects;

/**
 * Data the user needs to complete the OAuth2 device flow — handed to the
 * {@link DeviceCodeCredentials#userPrompt()} callback. Present
 * {@link #verificationUriComplete()} (or {@link #verificationUri()} +
 * {@link #userCode()}) to the user; the SDK polls in the background and
 * resumes automatically once they confirm.
 *
 * @param userCode short code the user types at the verification page
 * @param verificationUri page where the user enters the code
 * @param verificationUriComplete one-click variant with the code pre-filled
 * @param expiresIn how long the codes stay valid
 *
 * @since 0.1.0
 */
public record DeviceAuthorization(
        String userCode,
        String verificationUri,
        String verificationUriComplete,
        Duration expiresIn) {

    private static final String ERR_USER_CODE_NULL = "userCode must not be null";
    private static final String ERR_VERIFICATION_URI_NULL = "verificationUri must not be null";
    private static final String ERR_VERIFICATION_URI_COMPLETE_NULL =
            "verificationUriComplete must not be null";
    private static final String ERR_EXPIRES_IN_NULL = "expiresIn must not be null";

    public DeviceAuthorization {
        Objects.requireNonNull(userCode, ERR_USER_CODE_NULL);
        Objects.requireNonNull(verificationUri, ERR_VERIFICATION_URI_NULL);
        Objects.requireNonNull(verificationUriComplete, ERR_VERIFICATION_URI_COMPLETE_NULL);
        Objects.requireNonNull(expiresIn, ERR_EXPIRES_IN_NULL);
    }
}
