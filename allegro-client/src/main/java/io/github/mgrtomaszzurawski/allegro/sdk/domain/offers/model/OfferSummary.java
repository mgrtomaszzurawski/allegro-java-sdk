/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CurrentPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ExternalIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MinimalPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAdditionalServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoImageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1B2bRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1DeliveryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1PublicationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1SaleInfoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1StatsRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoV1StockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceAutomationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceAutomationRuleRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ShippingRatesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StartingPriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
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
 * @param watchersCount   how many buyers watch the offer, or {@code null} when the payload omits it
 * @param visitsCount     how many times the offer was visited, or {@code null} when the payload omits it
 * @param externalId      the seller's own external identifier for the offer, or {@code null}
 * @param businessOnly    {@code true} if the offer is buyable only by business buyers, or {@code null}
 * @param shippingRatesId the id of the offer's shipping-rates table, or {@code null}
 * @param additionalServicesGroupId the id of the offer's additional-services group, or {@code null}
 * @param fundraisingCampaignId the id of the offer's fundraising campaign, or {@code null}
 * @param currentPrice    the offer's current price (the auction/live price), or {@code null}
 * @param biddersCount    how many buyers have bid, or {@code null} when the payload omits it
 * @param minimalPrice    the auction minimal (reserve) price, or {@code null}
 * @param startingPrice   the auction starting price, or {@code null}
 * @param priceAutomationRuleId the id of the automatic-pricing rule applied to the offer, or {@code null}
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
        @Nullable OffsetDateTime endedAt,
        @Nullable Integer watchersCount,
        @Nullable Integer visitsCount,
        @Nullable String externalId,
        @Nullable Boolean businessOnly,
        @Nullable String shippingRatesId,
        @Nullable String additionalServicesGroupId,
        @Nullable String fundraisingCampaignId,
        @Nullable Money currentPrice,
        @Nullable Integer biddersCount,
        @Nullable Money minimalPrice,
        @Nullable Money startingPrice,
        @Nullable String priceAutomationRuleId) {

    /** Project a generated listing item onto the consumer record. */
    public static OfferSummary from(OfferListingDtoRaw raw) {
        OfferListingDtoV1SellingModeRaw sellingMode = raw.getSellingMode();
        OfferCategoryRaw category = raw.getCategory();
        OfferListingDtoV1PublicationRaw publication = raw.getPublication();
        OfferListingDtoV1StockRaw stock = raw.getStock();
        OfferListingDtoImageRaw primaryImage = raw.getPrimaryImage();
        OfferListingDtoV1StatsRaw stats = raw.getStats();
        OfferListingDtoV1SaleInfoRaw saleInfo = raw.getSaleInfo();
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
                publication == null ? null : parseDateTime(publication.getEndedAt()),
                stats == null ? null : stats.getWatchersCount(),
                stats == null ? null : stats.getVisitsCount(),
                externalIdOf(raw),
                businessOnlyOf(raw),
                shippingRatesIdOf(raw),
                additionalServicesGroupIdOf(raw),
                fundraisingCampaignIdOf(raw),
                currentPriceOf(saleInfo),
                saleInfo == null ? null : saleInfo.getBiddersCount(),
                minimalPriceOf(sellingMode),
                startingPriceOf(sellingMode),
                priceAutomationRuleIdOf(sellingMode));
    }

    private static @Nullable Money currentPriceOf(@Nullable OfferListingDtoV1SaleInfoRaw saleInfo) {
        if (saleInfo == null) {
            return null;
        }
        CurrentPriceRaw price = saleInfo.getCurrentPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money minimalPriceOf(@Nullable OfferListingDtoV1SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        MinimalPriceRaw price = sellingMode.getMinimalPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable Money startingPriceOf(@Nullable OfferListingDtoV1SellingModeRaw sellingMode) {
        if (sellingMode == null) {
            return null;
        }
        StartingPriceRaw price = sellingMode.getStartingPrice();
        return price == null ? null : Money.of(price.getAmount(), price.getCurrency());
    }

    private static @Nullable String priceAutomationRuleIdOf(
            @Nullable OfferListingDtoV1SellingModeRaw sellingMode) {
        PriceAutomationRaw priceAutomation = sellingMode == null ? null : sellingMode.getPriceAutomation();
        PriceAutomationRuleRaw rule = priceAutomation == null ? null : priceAutomation.getRule();
        return rule == null ? null : rule.getId();
    }

    private static @Nullable String externalIdOf(OfferListingDtoRaw raw) {
        ExternalIdRaw external = raw.getExternal();
        return external == null ? null : external.getId();
    }

    private static @Nullable Boolean businessOnlyOf(OfferListingDtoRaw raw) {
        OfferListingDtoV1B2bRaw b2bBlock = raw.getB2b();
        return b2bBlock == null ? null : b2bBlock.getBuyableOnlyByBusiness();
    }

    private static @Nullable String shippingRatesIdOf(OfferListingDtoRaw raw) {
        OfferListingDtoV1DeliveryRaw delivery = raw.getDelivery();
        ShippingRatesRaw shippingRates = delivery == null ? null : delivery.getShippingRates();
        return shippingRates == null ? null : shippingRates.getId();
    }

    private static @Nullable String additionalServicesGroupIdOf(OfferListingDtoRaw raw) {
        OfferAdditionalServicesRaw additionalServices = raw.getAdditionalServices();
        UUID id = additionalServices == null ? null : additionalServices.getId();
        return id == null ? null : id.toString();
    }

    private static @Nullable String fundraisingCampaignIdOf(OfferListingDtoRaw raw) {
        JustIdRaw fundraisingCampaign = raw.getFundraisingCampaign();
        return fundraisingCampaign == null ? null : fundraisingCampaign.getId();
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
