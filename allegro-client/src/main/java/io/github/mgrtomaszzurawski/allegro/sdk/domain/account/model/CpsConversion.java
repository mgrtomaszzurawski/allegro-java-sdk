/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model;

import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionCommissionAllegroRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionCommissionPublisherRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionCommissionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionMarketplaceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionOfferCategoryRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionOfferRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionOfferSellerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionOfferUnitPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.CpsConversionRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A single affiliate CPS (Cost Per Sale) conversion, as returned by
 * {@code Affiliate.streamCpsConversions(...)}. Beta resource.
 *
 * @param id conversion identifier
 * @param status conversion lifecycle status, or {@code null} if not reported
 * @param lastModifiedAt when the conversion was last modified, or {@code null}
 * @param orderCreatedAt when the related order was created, or {@code null}
 * @param quantity net ordered quantity (ordered minus returned), or {@code null}
 * @param marketplaceId marketplace the conversion belongs to, or {@code null}
 * @param offer the converted offer, or {@code null}
 * @param commission the commission breakdown, or {@code null}
 * @param publisherUrlParameters the affiliate tracking parameters echoed back on
 *     the publisher link; empty if none were reported
 *
 * @since 0.2.0
 */
public record CpsConversion(
        String id,
        @Nullable ConversionStatus status,
        @Nullable OffsetDateTime lastModifiedAt,
        @Nullable OffsetDateTime orderCreatedAt,
        @Nullable Integer quantity,
        @Nullable String marketplaceId,
        @Nullable Offer offer,
        @Nullable Commission commission,
        Map<String, String> publisherUrlParameters) {

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static CpsConversion from(CpsConversionRaw raw) {
        CpsConversionMarketplaceRaw marketplace = raw.getMarketplace();
        return new CpsConversion(
                raw.getId(),
                ConversionStatus.from(raw.getStatus()),
                raw.getLastModifiedAt(),
                raw.getOrderCreatedAt(),
                raw.getQuantity(),
                marketplace == null ? null : marketplace.getId(),
                Offer.from(raw.getOffer()),
                Commission.from(raw.getCommission()),
                publisherUrlParameters(raw.getPublisherUrlParameters()));
    }

    /**
     * An unmodifiable copy of the tracking parameters, empty when absent. A
     * {@code null}-valued entry is dropped rather than allowed to abort the stream
     * (the same forward-compat stance as the price/enum fields), so this tolerates a
     * wire map that {@link Map#copyOf} would reject.
     */
    private static Map<String, String> publisherUrlParameters(@Nullable Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    /**
     * The converted offer.
     *
     * @param id offer id
     * @param name offer title, or {@code null}
     * @param categoryId id of the offer's category, or {@code null}
     * @param unitPrice unit price, or {@code null}
     * @param sellerLogin the seller's login, or {@code null}
     */
    public record Offer(
            String id,
            @Nullable String name,
            @Nullable String categoryId,
            @Nullable Money unitPrice,
            @Nullable String sellerLogin) {

        static @Nullable Offer from(@Nullable CpsConversionOfferRaw raw) {
            if (raw == null) {
                return null;
            }
            CpsConversionOfferCategoryRaw category = raw.getCategory();
            CpsConversionOfferUnitPriceRaw price = raw.getUnitPrice();
            CpsConversionOfferSellerRaw seller = raw.getSeller();
            return new Offer(
                    raw.getId(),
                    raw.getName(),
                    category == null ? null : category.getId(),
                    price == null ? null : money(price.getAmount(), price.getCurrency()),
                    seller == null ? null : seller.getLogin());
        }
    }

    /**
     * The commission split for a conversion.
     *
     * @param publisher the publisher's commission, or {@code null}
     * @param allegro Allegro's commission, or {@code null}
     */
    public record Commission(@Nullable Money publisher, @Nullable Money allegro) {

        static @Nullable Commission from(@Nullable CpsConversionCommissionRaw raw) {
            if (raw == null) {
                return null;
            }
            CpsConversionCommissionPublisherRaw publisher = raw.getPublisher();
            CpsConversionCommissionAllegroRaw allegro = raw.getAllegro();
            return new Commission(
                    publisher == null ? null : money(publisher.getAmount(), publisher.getCurrency()),
                    allegro == null ? null : money(allegro.getAmount(), allegro.getCurrency()));
        }
    }

    /**
     * Build {@link Money} from a beta price pair, tolerating an incomplete
     * object: the CPS price/commission leaf amounts are nullable on the wire, so
     * a present-but-empty object maps to {@code null} instead of aborting the
     * stream with an {@code IllegalArgumentException} from {@link Money}.
     */
    private static @Nullable Money money(@Nullable String amount, @Nullable String currency) {
        return amount == null || currency == null ? null : Money.of(amount, currency);
    }
}
