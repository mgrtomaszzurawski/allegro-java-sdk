/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * How an offer should be published when it is created or edited: whether it goes live
 * immediately or stays a draft, when a scheduled publication starts, whether it
 * auto-relists when it sells out, and (for auctions) how long the listing runs.
 *
 * <p>Every field is optional — an offer created without publication settings starts as a
 * draft you publish later with {@code offers().batch().publish(...)}. Set {@code status}
 * to {@link OfferStatus#ACTIVE} to request immediate publication, or schedule it with
 * {@code startingAt}.
 *
 * @param status     the publication status to request ({@link OfferStatus#ACTIVE} to
 *                   publish, {@link OfferStatus#INACTIVE} to keep as a draft,
 *                   {@link OfferStatus#ENDED} to end the offer), or {@code null} to leave it
 * @param startingAt when a scheduled publication should start, or {@code null} for no schedule
 * @param republish  {@code true} to auto-relist the offer when it sells out, or {@code null}
 * @param duration   how long the listing runs (mapped to an ISO-8601 duration, e.g.
 *                   {@code Duration.ofHours(72)} → {@code "PT72H"}), or {@code null} for the default
 * @since 0.5.0
 */
public record PublicationSettings(
        @Nullable OfferStatus status,
        @Nullable OffsetDateTime startingAt,
        @Nullable Boolean republish,
        @Nullable Duration duration) {

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder()
                .status(status)
                .startingAt(startingAt)
                .republish(republish)
                .duration(duration);
    }

    /** Fluent builder for {@link PublicationSettings}. */
    public static final class Builder {

        private @Nullable OfferStatus status;
        private @Nullable OffsetDateTime startingAt;
        private @Nullable Boolean republish;
        private @Nullable Duration duration;

        /** Request a publication status ({@code ACTIVE} to publish, {@code INACTIVE} for a draft). */
        public Builder status(@Nullable OfferStatus status) {
            this.status = status;
            return this;
        }

        /** Schedule when the publication should start. */
        public Builder startingAt(@Nullable OffsetDateTime startingAt) {
            this.startingAt = startingAt;
            return this;
        }

        /** Set whether the offer auto-relists when it sells out. */
        public Builder republish(@Nullable Boolean republish) {
            this.republish = republish;
            return this;
        }

        /** Set how long the listing runs (mapped to an ISO-8601 duration). */
        public Builder duration(@Nullable Duration duration) {
            this.duration = duration;
            return this;
        }

        /** Build the publication settings. */
        public PublicationSettings build() {
            return new PublicationSettings(status, startingAt, republish, duration);
        }
    }
}
