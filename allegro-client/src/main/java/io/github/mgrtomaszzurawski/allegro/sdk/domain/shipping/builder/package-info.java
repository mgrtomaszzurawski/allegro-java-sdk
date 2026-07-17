/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Fluent builders for the shipping facade request types (bucket C). Required
 * fields are validated fail-fast at {@code build()} with a message naming the
 * missing field; server-side length constraints are replicated here so an
 * invalid request fails locally instead of on the wire.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder;

import org.jspecify.annotations.NullMarked;
