/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model;

/**
 * The dispatch-time range a shipping-rate row promises — the minimum and maximum
 * handling time before a parcel leaves, in the string form Allegro uses (maps to
 * the wire {@code from}/{@code to} pair).
 *
 * @param fromTime the shortest dispatch time (server string form)
 * @param toTime the longest dispatch time (server string form)
 *
 * @since 0.3.0
 */
public record ShippingTime(String fromTime, String toTime) {
}
