/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.Objects;
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
 * @since 0.2.0
 */
public final class BadgePatch {

    /** Which change a {@link BadgePatch} carries. */
    public enum Kind {

        /** Finish the badge, ending its display. */
        FINISH,

        /** Change the badge's bargain price. */
        CHANGE_BARGAIN_PRICE
    }

    private static final String ERR_PRICE_REQUIRED = "bargainPrice must not be null";

    private final Kind kind;
    private final @Nullable Money bargainPrice;

    private BadgePatch(Kind kind, @Nullable Money bargainPrice) {
        this.kind = kind;
        this.bargainPrice = bargainPrice;
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
        if (bargainPrice == null) {
            throw new IllegalArgumentException(ERR_PRICE_REQUIRED);
        }
        return new BadgePatch(Kind.CHANGE_BARGAIN_PRICE, bargainPrice);
    }

    /** Which change this patch carries. */
    public Kind kind() {
        return kind;
    }

    /** The new bargain price for a {@link Kind#CHANGE_BARGAIN_PRICE} patch, else {@code null}. */
    public @Nullable Money bargainPrice() {
        return bargainPrice;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BadgePatch patch)) {
            return false;
        }
        return kind == patch.kind && Objects.equals(bargainPrice, patch.bargainPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, bargainPrice);
    }

    @Override
    public String toString() {
        return "BadgePatch{kind=" + kind + ", bargainPrice=" + bargainPrice + '}';
    }
}
