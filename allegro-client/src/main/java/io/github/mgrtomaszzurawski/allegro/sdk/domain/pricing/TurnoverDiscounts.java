/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import java.util.List;

/**
 * Marketplace turnover discounts: the seller rewards a buyer's cumulated
 * turnover with a percentage discount, configured per marketplace.
 *
 * <p>Turnover discounts require a company seller account and a base-currency
 * marketplace; the server rejects configurations that violate those constraints.
 *
 * @since 0.3.0
 */
public interface TurnoverDiscounts {

    /**
     * List the turnover discounts across all of the seller's marketplaces.
     *
     * @return one entry per marketplace that carries turnover-discount data
     */
    List<TurnoverDiscount> list();

    /**
     * List the turnover discount for a single marketplace.
     *
     * @param marketplaceId the marketplace to filter to
     * @return the matching turnover discounts (usually one, possibly empty)
     */
    List<TurnoverDiscount> list(String marketplaceId);

    /**
     * Create or modify the turnover discount for a marketplace.
     *
     * @param marketplaceId the marketplace to configure
     * @param request the threshold ladder, built with
     *     {@link TurnoverDiscountRequest#builder()}
     * @return the resulting turnover discount
     */
    TurnoverDiscount set(String marketplaceId, TurnoverDiscountRequest request);

    /**
     * Deactivate the turnover discount for a marketplace.
     *
     * @param marketplaceId the marketplace to deactivate
     * @return the turnover discount in its (deactivating) state
     */
    TurnoverDiscount deactivate(String marketplaceId);
}
