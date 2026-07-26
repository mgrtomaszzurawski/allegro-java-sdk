/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

/**
 * The paid listing-promotion options an offer can carry, which add to its fees.
 * Each option is an independent flag; {@link #NONE} selects none of them.
 *
 * @param emphasized1d highlight the offer for one day
 * @param emphasized10d highlight the offer for ten days
 * @param departmentPage show the offer on the category department page
 *
 * @since 0.1.0
 */
public record PromotionOptions(boolean emphasized1d, boolean emphasized10d, boolean departmentPage) {

    /** No promotion options selected. */
    public static final PromotionOptions NONE = new PromotionOptions(false, false, false);

    /**
     * Whether any option is selected — {@code false} for {@link #NONE}. Lets the
     * request mapper omit the promotion block entirely when nothing is set.
     *
     * @return {@code true} if at least one option is enabled
     */
    public boolean any() {
        return emphasized1d || emphasized10d || departmentPage;
    }
}
