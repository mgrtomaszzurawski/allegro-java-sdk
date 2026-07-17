/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

/**
 * Pricing tools for the authenticated seller — reached via
 * {@code AllegroClient.pricing()}.
 *
 * <p>The starter slice exposes only {@link #automation()}; fee preview, quotes,
 * promotions, turnover discounts and deposit types are added by the bucket's
 * volume PR.
 *
 * @since 0.2.0
 */
public interface Pricing {

    /**
     * Automatic pricing rules — follow-the-market price strategies the seller
     * defines once and assigns to offers.
     *
     * @return the automatic-pricing sub-facade
     */
    PricingAutomation automation();
}
