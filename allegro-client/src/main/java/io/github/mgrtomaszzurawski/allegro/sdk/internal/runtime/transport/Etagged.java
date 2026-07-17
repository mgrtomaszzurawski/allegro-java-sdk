/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import org.jspecify.annotations.Nullable;

/**
 * A deserialized resource together with the {@code ETag} the server returned
 * for it. A domain client reads a resource with {@link HttpCall#fetchWithETag},
 * keeps the {@code etag}, and later guards its conditional update with
 * {@link HttpCall#ifMatch(String)} so a concurrent modification is rejected
 * (HTTP 412) instead of silently overwritten.
 *
 * <p>Internal plumbing — the {@code etag} is an optimistic-concurrency token the
 * SDK manages for the consumer, not part of the published surface.
 *
 * @param value the deserialized resource
 * @param etag  the response {@code ETag}, or {@code null} if the server sent none
 * @param <T>   resource type
 * @since 0.2.0
 */
public record Etagged<T>(T value, @Nullable String etag) {
}
