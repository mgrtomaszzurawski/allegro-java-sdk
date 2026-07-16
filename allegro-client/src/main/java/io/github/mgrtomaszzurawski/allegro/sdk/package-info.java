/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Public entry point of the Allegro Java SDK.
 *
 * <p>{@link io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient} is the single
 * {@link java.lang.AutoCloseable} entry point; domain accessors (offers, orders,
 * fulfillment, billing, …) are added per the accepted task-division plan. All
 * exported packages are null-marked (JSpecify): references are non-null unless
 * annotated {@link org.jspecify.annotations.Nullable}.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk;

import org.jspecify.annotations.NullMarked;
