/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Pricing facade (bucket G) — reached via {@code AllegroClient.pricing()}.
 *
 * <p>Groups the seller's pricing tools: automatic pricing rules, offer fee
 * preview and quotes, loyalty promotions and turnover discounts, and deposit
 * types. The starter slice ships {@link
 * io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.PricingAutomation}
 * rule create/get/delete; the remaining operations land with the bucket's
 * volume PR.
 *
 * @since 0.2.0
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;
