/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import org.jspecify.annotations.Nullable;

/**
 * A change to an offer's active badge, passed to {@code badges().update(...)}.
 * The badge update endpoint accepts exactly one kind of change per call, so a
 * {@code BadgePatch} is created through one of the intent factories rather than a
 * multi-field builder:
 *
 * <ul>
 *   <li>{@link #finish()} — end the badge's display;</li>
 *   <li>{@link #changeBargainPrice(Money)} — set a new bargain price.</li>
 * </ul>
 *
 * @param kind         which change this patch carries
 * @param bargainPrice the new bargain price for a {@link Kind#CHANGE_BARGAIN_PRICE}
 *                     patch; {@code null} for {@link Kind#FINISH}
 *
 * @since 0.2.0
 */
public record BadgePatch(Kind kind, @Nullable Money bargainPrice) {

    /** Which change a {@link BadgePatch} carries. */
    public enum Kind {

        /** Finish the badge, ending its display. */
        FINISH,

        /** Change the badge's bargain price. */
        CHANGE_BARGAIN_PRICE
    }

    private static final String ERR_PRICE_REQUIRED = "bargainPrice must not be null";

    /** Enforces that a price-change patch always carries a price. */
    public BadgePatch {
        if (kind == Kind.CHANGE_BARGAIN_PRICE && bargainPrice == null) {
            throw new IllegalArgumentException(ERR_PRICE_REQUIRED);
        }
    }

    /** Finish the badge, ending its display. */
    public static BadgePatch finish() {
        return new BadgePatch(Kind.FINISH, null);
    }

    /**
     * Change the badge's bargain price.
     *
     * @param bargainPrice the new bargain price; must not be {@code null}
     * @return the patch
     * @throws IllegalArgumentException if {@code bargainPrice} is {@code null}
     */
    public static BadgePatch changeBargainPrice(Money bargainPrice) {
        return new BadgePatch(Kind.CHANGE_BARGAIN_PRICE, bargainPrice);
    }
}
