/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Shipping facade (bucket C), reached via {@code AllegroClient.shipping()}:
 * shipment management, delivery configuration, and points of service. This
 * starter slice ships the points-of-service sub-facade; the remaining shipping
 * operations land in the bucket's volume PR.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping;

import org.jspecify.annotations.NullMarked;
