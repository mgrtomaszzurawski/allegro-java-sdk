/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk;

import org.apiguardian.api.API;

/**
 * Single entry point of the Allegro Java SDK.
 *
 * <p><strong>Bootstrap scaffold.</strong> This is a placeholder entry point: it
 * establishes the module surface and version constant so downstream modules and
 * consumers compile against a stable type. The builder, OAuth2 credential
 * configuration, transport runtime, and domain accessors (offers, orders,
 * fulfillment, billing, …) are added per the accepted task-division plan — see
 * the shared {@code BACKLOG.md}. The public method shape here is intentionally
 * minimal and will grow; it is not yet a usable client.
 *
 * <p>Marked {@link org.apiguardian.api.API.Status#EXPERIMENTAL EXPERIMENTAL}:
 * the surface may break between {@code 0.x} releases until the {@code 1.0.0}
 * cut locks the contract.
 *
 * @since 0.1.0
 */
@API(status = API.Status.EXPERIMENTAL, since = "0.1.0")
public final class AllegroClient implements AutoCloseable {

    /** Current SDK artefact version — mirrors {@code gradle.properties}. */
    public static final String SDK_VERSION = "0.1.0-preview";

    private AllegroClient() {
        // Instances are created via a builder introduced with the transport/auth
        // core PR; the scaffold exposes no construction path yet.
    }

    /**
     * The SDK artefact version string.
     *
     * @return the semantic version of this SDK build (e.g. {@code 0.1.0-preview})
     */
    public static String sdkVersion() {
        return SDK_VERSION;
    }

    /**
     * No-op in the scaffold. Once the transport runtime lands, this releases the
     * underlying HTTP resources and invalidates cached OAuth2 tokens.
     */
    @Override
    public void close() {
        // Nothing to release yet — no transport runtime is held in the scaffold.
    }
}
