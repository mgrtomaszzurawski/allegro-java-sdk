/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Fluent builders for the pricing facade's request records. Each builder
 * validates required fields and replicated server constraints fail-fast in
 * {@code build()}, so an invalid request never reaches the wire.
 *
 * @since 0.2.0
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.builder;
