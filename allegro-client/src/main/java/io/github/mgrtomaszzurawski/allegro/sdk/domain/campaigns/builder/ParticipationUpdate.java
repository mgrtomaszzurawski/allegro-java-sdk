/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.MarketplaceParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.ParticipationStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * The per-marketplace participation change passed to
 * {@code allegroPrices().updateParticipation(...)}. Built by allowing or denying
 * Allegro Prices on one or more marketplaces; at least one marketplace is required.
 *
 * <pre>{@code
 * ParticipationUpdate update = ParticipationUpdate.builder()
 *         .allow("allegro-pl")
 *         .deny("allegro-cz")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ParticipationUpdate {

    private final List<MarketplaceParticipation> marketplaces;

    private ParticipationUpdate(Builder builder) {
        this.marketplaces = List.copyOf(builder.marketplaces);
    }

    /** The requested per-marketplace participation states; never empty. */
    public List<MarketplaceParticipation> marketplaces() {
        return marketplaces;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this update. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        for (MarketplaceParticipation entry : marketplaces) {
            builder.set(entry.marketplaceId(), entry.status());
        }
        return builder;
    }

    /** Fluent builder for {@link ParticipationUpdate}; requires at least one marketplace. */
    public static final class Builder {

        private static final String ERR_EMPTY = "at least one marketplace is required";

        private final List<MarketplaceParticipation> marketplaces = new ArrayList<>();

        private Builder() {
        }

        /** Take part in Allegro Prices on the given marketplace. */
        public Builder allow(String marketplaceId) {
            return set(marketplaceId, ParticipationStatus.ALLOWED);
        }

        /** Do not take part in Allegro Prices on the given marketplace. */
        public Builder deny(String marketplaceId) {
            return set(marketplaceId, ParticipationStatus.DENIED);
        }

        private Builder set(String marketplaceId, ParticipationStatus status) {
            marketplaces.add(new MarketplaceParticipation(marketplaceId, status));
            return this;
        }

        /**
         * Validate and build the update.
         *
         * @return the immutable update
         * @throws IllegalStateException if no marketplace was set
         */
        public ParticipationUpdate build() {
            if (marketplaces.isEmpty()) {
                throw new IllegalStateException(ERR_EMPTY);
            }
            return new ParticipationUpdate(this);
        }
    }
}
