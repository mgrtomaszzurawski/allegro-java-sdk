/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import org.jspecify.annotations.Nullable;

/**
 * A deserialized resource together with the {@code Location} header the server
 * returned for it. Allegro's attachment flow declares an attachment
 * ({@code POST .../attachments}) and returns, in {@code Location}, an ABSOLUTE
 * upload URL on a different host ({@code upload.allegro.pl}); the binary is then
 * PUT to that URL via {@link HttpCall#putAbsolute(String)}.
 *
 * <p>Internal plumbing — the upload URL is transport detail the SDK manages for
 * the consumer, not part of the published surface.
 *
 * @param value    the deserialized resource
 * @param location the response {@code Location}, or {@code null} if the server sent none
 * @param <T>      resource type
 * @since 0.2.0
 */
public record Located<T>(T value, @Nullable String location) {
}
