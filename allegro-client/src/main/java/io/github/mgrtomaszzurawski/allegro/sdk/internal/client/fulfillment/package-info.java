/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Layer-2 endpoint wrappers behind the {@code fulfillment()} facade (bucket I):
 * they build requests through the shared transport and map generated
 * {@code *Raw} DTOs to and from the public domain records. Not exported.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.fulfillment;

import org.jspecify.annotations.NullMarked;
