/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

/**
 * A package-weight limit on a shipping-rate row — the maximum weight of a single
 * package priced by that row.
 *
 * @param value the weight amount as the exact string Allegro uses (e.g. {@code "30.0"})
 * @param unit the weight unit (e.g. {@code "KILOGRAMS"})
 *
 * @since 0.3.0
 */
public record Weight(String value, String unit) {
}
