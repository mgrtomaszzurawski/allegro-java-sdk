/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A batch promotion-package change — the request passed to
 * {@code offers().promoOptions().modifyBatch(...)}. It sets the base promotion
 * package and/or the extra packages on every target offer (up to
 * {@value #MAX_OFFERS}) in one command, optionally timed to take effect now or at
 * the end of the current cycle.
 *
 * <p>At least one of the base package or the extra packages must be given (an
 * empty change would do nothing). Omitting the extra packages preserves whatever
 * the offers already have; passing an empty list is not how you clear them — the
 * builder rejects an empty change. Available package ids come from
 * {@code promoOptions().availablePackages()}.
 *
 * @since 0.5.0
 */
public final class BatchPromoOptionsRequest {

    /** Allegro accepts up to 1000 offers in one criterion. */
    public static final int MAX_OFFERS = 1000;

    private static final String ERR_OFFERS_EMPTY = "at least one offer id is required";
    private static final String ERR_OFFERS_TOO_MANY = "at most " + MAX_OFFERS + " offers per command";
    private static final String ERR_OFFER_ID = "offer id must not be null or blank";
    private static final String ERR_PACKAGE_ID = "package id must not be null or blank";
    private static final String ERR_TIMING = "timing must not be null";
    private static final String ERR_NO_CHANGE = "set a base package or at least one extra package";

    private final List<String> offerIds;
    private final @Nullable String basePackageId;
    private final List<String> extraPackageIds;
    private final @Nullable PromoModificationTiming timing;

    private BatchPromoOptionsRequest(Builder builder) {
        this.offerIds = List.copyOf(builder.offerIds);
        this.basePackageId = builder.basePackageId;
        this.extraPackageIds = List.copyOf(builder.extraPackageIds);
        this.timing = builder.timing;
    }

    /** Start building a promotion-package change for {@code offerIds}. */
    public static Builder forOffers(List<String> offerIds) {
        return new Builder(offerIds);
    }

    /** The offers this change targets. */
    public List<String> offerIds() {
        return offerIds;
    }

    /** The base package id to set, or {@code null} if the base package is unchanged. */
    public @Nullable String basePackageId() {
        return basePackageId;
    }

    /** The extra package ids to set; empty if the extra packages are unchanged. */
    public List<String> extraPackageIds() {
        return extraPackageIds;
    }

    /** When the change takes effect, or {@code null} to leave it to Allegro's default. */
    public @Nullable PromoModificationTiming timing() {
        return timing;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final List<String> offerIds;
        private @Nullable String basePackageId;
        private final List<String> extraPackageIds = new ArrayList<>();
        private @Nullable PromoModificationTiming timing;

        private Builder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Set the base promotion package. */
        public Builder basePackage(String packageId) {
            this.basePackageId = requireText(packageId, ERR_PACKAGE_ID);
            return this;
        }

        /** Add one extra promotion package. */
        public Builder addExtraPackage(String packageId) {
            this.extraPackageIds.add(requireText(packageId, ERR_PACKAGE_ID));
            return this;
        }

        /** Set the extra promotion packages (replaces any added so far). */
        public Builder extraPackages(List<String> packageIds) {
            Objects.requireNonNull(packageIds, ERR_PACKAGE_ID);
            this.extraPackageIds.clear();
            for (String packageId : packageIds) {
                this.extraPackageIds.add(requireText(packageId, ERR_PACKAGE_ID));
            }
            return this;
        }

        /** Choose when the change takes effect. */
        public Builder timing(PromoModificationTiming timing) {
            this.timing = Objects.requireNonNull(timing, ERR_TIMING);
            return this;
        }

        /** Build, requiring a base package or at least one extra package. */
        public BatchPromoOptionsRequest build() {
            if (basePackageId == null && extraPackageIds.isEmpty()) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new BatchPromoOptionsRequest(this);
        }

        private static List<String> validatedOfferIds(List<String> offerIds) {
            Objects.requireNonNull(offerIds, ERR_OFFERS_EMPTY);
            if (offerIds.isEmpty()) {
                throw new IllegalArgumentException(ERR_OFFERS_EMPTY);
            }
            if (offerIds.size() > MAX_OFFERS) {
                throw new IllegalArgumentException(ERR_OFFERS_TOO_MANY);
            }
            for (String offerId : offerIds) {
                requireText(offerId, ERR_OFFER_ID);
            }
            return offerIds;
        }

        private static String requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(message);
            }
            return value;
        }
    }
}
