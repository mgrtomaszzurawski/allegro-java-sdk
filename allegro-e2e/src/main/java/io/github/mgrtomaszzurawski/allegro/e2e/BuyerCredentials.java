/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import java.nio.file.Path;

/**
 * Sandbox buyer web-UI credentials and the storage-state location, all sourced
 * from the environment — never hardcoded, never logged.
 *
 * @param login    buyer account login (from {@code ALLEGRO_SANDBOX_BUYER_LOGIN})
 * @param password buyer account password (from {@code ALLEGRO_SANDBOX_BUYER_PASSWORD})
 */
public record BuyerCredentials(String login, String password) {

    private static final String LOGIN_ENV = "ALLEGRO_SANDBOX_BUYER_LOGIN";
    private static final String PASSWORD_ENV = "ALLEGRO_SANDBOX_BUYER_PASSWORD";
    private static final String STORAGE_STATE_ENV = "ALLEGRO_BUYER_STORAGE_STATE";
    /** Default storage-state path — under the shared secrets dir, OUTSIDE any git repo. */
    private static final String DEFAULT_STORAGE_STATE =
            "/workspace/shared/secrets/allegro-buyer-storage-state.json";

    /** Read the buyer credentials from the environment, or throw if absent. */
    public static BuyerCredentials fromEnv() {
        String login = System.getenv(LOGIN_ENV);
        String password = System.getenv(PASSWORD_ENV);
        if (login == null || password == null) {
            throw new IllegalStateException("Missing " + LOGIN_ENV + " / " + PASSWORD_ENV
                    + " - source /workspace/shared/secrets/allegro-sandbox.env first");
        }
        return new BuyerCredentials(login, password);
    }

    /** Configured storage-state path, or the shared-secrets default. */
    public static Path storageStatePath() {
        String configured = System.getenv(STORAGE_STATE_ENV);
        return Path.of(configured != null ? configured : DEFAULT_STORAGE_STATE);
    }
}
