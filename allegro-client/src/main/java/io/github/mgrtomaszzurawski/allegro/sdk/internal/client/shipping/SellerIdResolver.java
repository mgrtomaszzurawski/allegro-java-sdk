/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.shipping;

import io.github.mgrtomaszzurawski.allegro.client.model.MeResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;

/**
 * Resolves and caches the authenticated seller's id (from {@code GET /me}).
 *
 * <p>Allegro requires the seller id in the points-of-service request body
 * ({@code seller.id} on create/update) and query ({@code seller.id} on list),
 * even though the bearer token already identifies the seller — a spec-vs-wire
 * divergence confirmed live (see {@code KNOWN-SERVER-BEHAVIORS.md}). Rather than
 * make the consumer pass their own id, the SDK looks it up once and caches it
 * for the client's lifetime; the id is stable per token.
 *
 * <p>Thread-safe: the first caller resolves under a lock, later callers read the
 * cached value without locking.
 *
 * @since 0.2.0
 */
final class SellerIdResolver {

    private static final String OP_RESOLVE = "resolve seller id";

    private final HttpSupport http;
    private volatile String cachedSellerId;

    SellerIdResolver(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /** The authenticated seller's id, resolved once and cached. */
    String sellerId() {
        String resolved = cachedSellerId;
        if (resolved == null) {
            synchronized (this) {
                resolved = cachedSellerId;
                if (resolved == null) {
                    resolved = http.getAuthenticated(
                            ApiPaths.CURRENT_USER, MeResponseRaw.class, OP_RESOLVE).getId();
                    cachedSellerId = resolved;
                }
            }
        }
        return resolved;
    }
}
