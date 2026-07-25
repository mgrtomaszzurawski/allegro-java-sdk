/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

/**
 * When a batch promotion-package change takes effect — immediately or at the end
 * of the current promotion cycle.
 *
 * @since 0.5.0
 */
public enum PromoModificationTiming {
    /** Apply the change immediately. */
    NOW,
    /** Apply the change when the current promotion cycle ends. */
    END_OF_CYCLE
}
