/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Payments facade (bucket B): payment operations history, refunded payments, and
 * refund initiation, reached via {@code AllegroClient.payments()}.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.domain.payments;

import org.jspecify.annotations.NullMarked;
