/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationMarketplacesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferPublicationResponseRaw;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The publication details of an offer, reached from {@link Offer#publication()}. The
 * offer's {@linkplain Offer#status() status} lives on the offer itself; this value carries
 * the surrounding lifecycle data — whether it auto-republishes, when it started and ends,
 * what ended it, and the base marketplace it is published on.
 *
 * @param republish         {@code true} if the offer re-lists automatically when it sells out
 *                          or ends, {@code false}/{@code null} otherwise
 * @param startingAt        when the (scheduled) publication starts, or {@code null}
 * @param endingAt          when the publication ends, or {@code null}
 * @param endedBy           what ended the publication (e.g. {@code USER}), or {@code null}
 *                          if it has not ended
 * @param baseMarketplaceId the id of the base marketplace the offer is published on (e.g.
 *                          {@code allegro-pl}), or {@code null} if the payload omits it
 * @param duration          the ISO-8601 duration the offer is published for (e.g. {@code PT720H}),
 *                          or {@code null} for an unlimited/unset duration
 * @param additionalMarketplaceIds the ids of the additional marketplaces the offer is published on,
 *                          in order (possibly empty)
 * @since 0.6.0
 */
public record OfferPublication(
        @Nullable Boolean republish,
        @Nullable OffsetDateTime startingAt,
        @Nullable OffsetDateTime endingAt,
        @Nullable String endedBy,
        @Nullable String baseMarketplaceId,
        @Nullable String duration,
        List<String> additionalMarketplaceIds) {

    /** Canonical constructor: defensively copies {@code additionalMarketplaceIds} to an immutable list. */
    public OfferPublication {
        additionalMarketplaceIds =
                additionalMarketplaceIds == null ? List.of() : List.copyOf(additionalMarketplaceIds);
    }

    /** Project a generated publication response onto the consumer value, or {@code null}. */
    public static @Nullable OfferPublication from(@Nullable SaleProductOfferPublicationResponseRaw raw) {
        if (raw == null) {
            return null;
        }
        SaleProductOfferPublicationMarketplacesResponseRaw marketplaces = raw.getMarketplaces();
        JustIdRaw base = marketplaces == null ? null : marketplaces.getBase();
        return new OfferPublication(
                raw.getRepublish(),
                raw.getStartingAt(),
                raw.getEndingAt(),
                raw.getEndedBy() == null ? null : raw.getEndedBy().getValue(),
                base == null ? null : base.getId(),
                raw.getDuration(),
                additionalMarketplaceIdsOf(marketplaces));
    }

    private static List<String> additionalMarketplaceIdsOf(
            @Nullable SaleProductOfferPublicationMarketplacesResponseRaw marketplaces) {
        List<JustIdRaw> additional = marketplaces == null ? null : marketplaces.getAdditional();
        if (additional == null) {
            return List.of();
        }
        return additional.stream().map(JustIdRaw::getId).filter(Objects::nonNull).toList();
    }
}
