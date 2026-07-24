/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1PublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1StockRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

/**
 * One offer as it appears in a seller's offer listing
 * ({@code offers().streamOffers(...)}).
 *
 * <p>A lighter projection than {@link Offer}: the fields a seller scans a list
 * by. The listing payload marks none of its nested objects {@code required}, so
 * mapping tolerates their absence — {@code buyNowPrice} is {@code null} for an
 * auction or when no price is present, {@code availableStock}/{@code soldCount}
 * are {@code null} when the offer tracks no quantity, {@code categoryId} and
 * {@code primaryImageUrl} are {@code null} when the payload omits them.
 *
 * @param id              offer identifier
 * @param name            offer title
 * @param categoryId      Allegro category the offer is listed in, or {@code null}
 * @param format          how the offer is sold
 * @param status          publication status
 * @param buyNowPrice     fixed Buy Now price, or {@code null} for an auction
 * @param availableStock  available quantity, or {@code null} when not tracked
 * @param soldCount       units sold, or {@code null} when not tracked
 * @param primaryImageUrl URL of the primary image, or {@code null}
 * @param afterSalesServices after-sales conditions (implied warranty, return policy,
 *                        warranty), or {@code null} if the payload omits them
 * @param fulfillment     {@code true} if the offer is handled by One Fulfillment by Allegro,
 *                        or {@code null} if the payload omits the flag
 * @param publishedAt     when the offer's publication started, or {@code null}
 * @param endedAt         when the offer's publication ended, or {@code null} if still running
 * @since 0.2.0
 */
public record OfferSummary(
        String id,
        String name,
        @Nullable String categoryId,
        OfferFormat format,
        OfferStatus status,
        @Nullable Money buyNowPrice,
        @Nullable Integer availableStock,
        @Nullable Integer soldCount,
        @Nullable String primaryImageUrl,
        @Nullable AfterSalesServices afterSalesServices,
        @Nullable Boolean fulfillment,
        @Nullable OffsetDateTime publishedAt,
        @Nullable OffsetDateTime endedAt) {

    /** Project a generated listing item onto the consumer record. */
    public static OfferSummary from(OfferListingDtoRaw raw) {
        OfferListingDtoV1SellingModeRaw sellingMode = raw.getSellingMode();
        OfferCategoryRaw category = raw.getCategory();
        OfferListingDtoV1PublicationRaw publication = raw.getPublication();
        OfferListingDtoV1StockRaw stock = raw.getStock();
        OfferListingDtoImageRaw primaryImage = raw.getPrimaryImage();
        return new OfferSummary(
                raw.getId(),
                raw.getName(),
                category == null ? null : category.getId(),
                OfferFormat.from(sellingMode == null ? null : sellingMode.getFormat()),
                OfferStatus.from(publication == null ? null : publication.getStatus()),
                buyNowPriceOf(sellingMode),
                stock == null ? null : stock.getAvailable(),
                stock == null ? null : stock.getSold(),
                primaryImage == null ? null : primaryImage.getUrl(),
                AfterSalesServices.from(raw.getAfterSalesServices()),
                raw.getIsFulfillment(),
                publication == null ? null : parseDateTime(publication.getStartedAt()),
                publication == null ? null : parseDateTime(publication.getEndedAt()));
    }

    /** Parse an ISO-8601 timestamp string the listing carries as text, tolerating absence/format. */
    private static @Nullable OffsetDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static @Nullable Money buyNowPriceOf(@Nullable OfferListingDtoV1SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        BuyNowPriceRaw price = sellingMode.getPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }
}
