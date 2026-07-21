/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A batch offer-settings change — the request passed to
 * {@code offers().batch().modify(...)}. It applies one or more of the supported
 * field changes to every target offer (up to {@value #MAX_OFFERS}) in one
 * command: the listing duration (a fixed {@link OfferDuration} or unlimited) and
 * the {@link HandlingTime dispatch time}. At least one change is required.
 *
 * <p>The listing duration is either a fixed length or unlimited — never both
 * (the wire accepts only one). This is the first slice of Allegro's broad offer
 * modification command; the id-based assignments (responsible person/producer,
 * wholesale price list) will follow separately, since their unassign semantics
 * need the wire's explicit-null, which this command's partial body omits.
 *
 * @since 0.5.0
 */
public final class BatchModificationRequest {

    /** Allegro accepts up to 1000 offers in one criterion. */
    public static final int MAX_OFFERS = 1000;

    private static final String ERR_OFFERS_EMPTY = "at least one offer id is required";
    private static final String ERR_OFFERS_TOO_MANY = "at most " + MAX_OFFERS + " offers per command";
    private static final String ERR_OFFER_ID = "offer id must not be null or blank";
    private static final String ERR_DURATION = "listing duration must not be null";
    private static final String ERR_HANDLING_TIME = "handling time must not be null";
    private static final String ERR_DURATION_CONFLICT =
            "a listing duration and an unlimited listing are mutually exclusive";
    private static final String ERR_NO_CHANGE = "a modification must change at least one field";

    private final List<String> offerIds;
    private final @Nullable OfferDuration listingDuration;
    private final boolean unlimitedListing;
    private final @Nullable HandlingTime handlingTime;

    private BatchModificationRequest(Builder builder) {
        this.offerIds = List.copyOf(builder.offerIds);
        this.listingDuration = builder.listingDuration;
        this.unlimitedListing = builder.unlimitedListing;
        this.handlingTime = builder.handlingTime;
    }

    /** Start building a modification for {@code offerIds}. */
    public static Builder forOffers(List<String> offerIds) {
        return new Builder(offerIds);
    }

    /** The offers this modification targets. */
    public List<String> offerIds() {
        return offerIds;
    }

    /** The fixed listing duration to set, or {@code null} (unlimited, or duration unchanged). */
    public @Nullable OfferDuration listingDuration() {
        return listingDuration;
    }

    /** Whether the listing should be made unlimited. */
    public boolean unlimitedListing() {
        return unlimitedListing;
    }

    /** The dispatch time to set, or {@code null} if the handling time is unchanged. */
    public @Nullable HandlingTime handlingTime() {
        return handlingTime;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final List<String> offerIds;
        private @Nullable OfferDuration listingDuration;
        private boolean unlimitedListing;
        private @Nullable HandlingTime handlingTime;

        private Builder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Set a fixed listing duration (mutually exclusive with {@link #unlimitedListing()}). */
        public Builder listingDuration(OfferDuration duration) {
            if (unlimitedListing) {
                throw new IllegalStateException(ERR_DURATION_CONFLICT);
            }
            this.listingDuration = Objects.requireNonNull(duration, ERR_DURATION);
            return this;
        }

        /** Make the listing unlimited (mutually exclusive with {@link #listingDuration(OfferDuration)}). */
        public Builder unlimitedListing() {
            if (listingDuration != null) {
                throw new IllegalStateException(ERR_DURATION_CONFLICT);
            }
            this.unlimitedListing = true;
            return this;
        }

        /** Set the dispatch (handling) time. */
        public Builder handlingTime(HandlingTime dispatchTime) {
            this.handlingTime = Objects.requireNonNull(dispatchTime, ERR_HANDLING_TIME);
            return this;
        }

        /** Build, requiring at least one field change. */
        public BatchModificationRequest build() {
            if (listingDuration == null && !unlimitedListing && handlingTime == null) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new BatchModificationRequest(this);
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
                if (offerId == null || offerId.isBlank()) {
                    throw new IllegalArgumentException(ERR_OFFER_ID);
                }
            }
            return offerIds;
        }
    }
}
