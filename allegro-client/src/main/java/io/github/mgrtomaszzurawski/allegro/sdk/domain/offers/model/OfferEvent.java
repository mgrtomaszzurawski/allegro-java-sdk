/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferActivatedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferArchivedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBidCanceledEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferBidPlacedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferChangedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferDeliveryCountriesChangedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEndedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEventBaseOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEventEndedOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEventExternalOfferForPriceChangesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEventExternalOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferEventExternalOfferWithPublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferPriceChangedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferStockChangedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferTranslationUpdatedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferVisibilityChangedEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerOfferBaseEventRaw;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * One event about a seller's offer — the offer was activated, ended, its price or stock
 * changed, a bid was placed, and so on. The {@link #type() type} is Allegro's event
 * discriminator (e.g. {@code OFFER_PRICE_CHANGED}); a type Allegro adds after this SDK release
 * still reads back with its wire {@code type} and a {@code null} {@link #offerId()} rather than
 * failing the stream.
 *
 * @param id         the event id (also the cursor to resume a stream from)
 * @param type       the event type discriminator
 * @param occurredAt when the event occurred, or {@code null} if the payload omits it
 * @param offerId    the affected offer's id, or {@code null} for an unrecognised event type
 * @since 0.4.0
 */
public record OfferEvent(
        String id,
        String type,
        @Nullable OffsetDateTime occurredAt,
        @Nullable String offerId) {

    /** Project a generated (polymorphic) offer event onto the consumer record. */
    public static OfferEvent from(SellerOfferBaseEventRaw raw) {
        return new OfferEvent(raw.getId(), raw.getType(), raw.getOccurredAt(), offerIdOf(raw));
    }

    private static @Nullable String offerIdOf(SellerOfferBaseEventRaw raw) {
        // Each generated event subtype carries the affected offer under its own offer type;
        // an unrecognised type (deserialized to the base, forward-compat) yields null.
        if (raw instanceof OfferActivatedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferArchivedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferChangedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferDeliveryCountriesChangedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferStockChangedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferTranslationUpdatedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferBidCanceledEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferBidPlacedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferEndedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferPriceChangedEventRaw event) {
            return idOf(event.getOffer());
        }
        if (raw instanceof OfferVisibilityChangedEventRaw event) {
            return idOf(event.getOffer());
        }
        return null;
    }

    private static @Nullable String idOf(@Nullable OfferEventExternalOfferRaw offer) {
        return offer == null ? null : offer.getId();
    }

    private static @Nullable String idOf(@Nullable OfferEventBaseOfferRaw offer) {
        return offer == null ? null : offer.getId();
    }

    private static @Nullable String idOf(@Nullable OfferEventEndedOfferRaw offer) {
        return offer == null ? null : offer.getId();
    }

    private static @Nullable String idOf(@Nullable OfferEventExternalOfferForPriceChangesRaw offer) {
        return offer == null ? null : offer.getId();
    }

    private static @Nullable String idOf(@Nullable OfferEventExternalOfferWithPublicationRaw offer) {
        return offer == null ? null : offer.getId();
    }
}
