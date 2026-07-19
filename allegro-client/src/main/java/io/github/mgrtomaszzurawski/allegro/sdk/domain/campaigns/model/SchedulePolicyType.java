/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model;

/**
 * How a {@link CampaignSchedule} window is bounded in time. The constant names
 * mirror the Allegro wire values exactly, so a campaign whose application period
 * is open-ended reads as {@link #ALWAYS} and one that never opens as {@link #NEVER}.
 *
 * @since 0.2.0
 */
public enum SchedulePolicyType {

    /** The window is always open — {@code from} and {@code to} are absent. */
    ALWAYS,

    /** Open from a start instant onward — only {@code from} is set. */
    SINCE,

    /** Open within a bounded window — both {@code from} and {@code to} are set. */
    WITHIN,

    /** Open until an end instant — only {@code to} is set. */
    UNTIL,

    /** The window never opens — {@code from} and {@code to} are absent. */
    NEVER,

    /** A value Allegro introduced that this SDK version does not model yet (read-only forward-compat sentinel). */
    UNKNOWN;

    /**
     * Map the Allegro wire value (identical to the constant name) to the enum,
     * degrading a value Allegro added after this SDK version to {@link #UNKNOWN}
     * rather than failing the read.
     */
    static SchedulePolicyType from(String wireValue) {
        try {
            return SchedulePolicyType.valueOf(wireValue);
        } catch (IllegalArgumentException unmodelledValue) {
            return UNKNOWN;
        }
    }
}
