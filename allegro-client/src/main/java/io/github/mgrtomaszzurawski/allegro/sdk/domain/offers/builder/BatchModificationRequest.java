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
 * {@code offers().batch().modify(...)}. It applies <em>exactly one</em> supported
 * field change to every target offer (up to {@value #MAX_OFFERS}) in one command:
 * the listing duration (a fixed {@link OfferDuration} or unlimited) or the
 * {@link HandlingTime dispatch time}. Exactly one is required — Allegro rejects a
 * command whose modification carries more than one element (live-verified:
 * {@code VALIDATION_ERROR}, <em>"modification should contain exactly 1 element"</em>),
 * so to change two aspects submit two commands.
 *
 * <p>This is the first slice of Allegro's broad offer modification command; the
 * id-based assignments (responsible person/producer, wholesale price list) will
 * follow separately, since their unassign semantics need the wire's explicit-null,
 * which this command's partial body omits.
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
    private static final String ERR_SINGLE_CHANGE =
            "a modification changes exactly one field — Allegro rejects a command with more than one";
    private static final String ERR_NO_CHANGE = "a modification must change exactly one field";

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

        /** Set a fixed listing duration (the request's single change). */
        public Builder listingDuration(OfferDuration duration) {
            requireNoChangeYet();
            this.listingDuration = Objects.requireNonNull(duration, ERR_DURATION);
            return this;
        }

        /** Make the listing unlimited (the request's single change). */
        public Builder unlimitedListing() {
            requireNoChangeYet();
            this.unlimitedListing = true;
            return this;
        }

        /** Set the dispatch (handling) time (the request's single change). */
        public Builder handlingTime(HandlingTime dispatchTime) {
            requireNoChangeYet();
            this.handlingTime = Objects.requireNonNull(dispatchTime, ERR_HANDLING_TIME);
            return this;
        }

        /** Build, requiring exactly one field change. */
        public BatchModificationRequest build() {
            if (listingDuration == null && !unlimitedListing && handlingTime == null) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new BatchModificationRequest(this);
        }

        /** Guard the single-change rule: a second change is rejected fail-fast. */
        private void requireNoChangeYet() {
            if (listingDuration != null || unlimitedListing || handlingTime != null) {
                throw new IllegalStateException(ERR_SINGLE_CHANGE);
            }
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
