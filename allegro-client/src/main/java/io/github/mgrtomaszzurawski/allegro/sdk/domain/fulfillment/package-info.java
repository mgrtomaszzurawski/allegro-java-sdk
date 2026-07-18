/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * One Fulfillment by Allegro facade (bucket I), reached via
 * {@code AllegroClient.fulfillment()}. Sellers enrolled in One Fulfillment ship
 * goods to Allegro's warehouse (advance ship notices), and Allegro stores stock
 * and fulfils their orders; this package exposes those operations as
 * intent-named methods over immutable records.
 */
@NullMarked
package io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment;

import org.jspecify.annotations.NullMarked;
