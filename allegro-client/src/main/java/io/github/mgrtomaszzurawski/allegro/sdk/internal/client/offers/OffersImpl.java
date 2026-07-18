/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.BuyNowPriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceInputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ChangePriceWithoutOutputRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferCategoryRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OffersSearchResultDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferRequestV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRatingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOffersRequestStockRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeFormatRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellingModeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartOfferClassificationReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseOffersInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferBatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.Offers;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.PromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.UnfilledParameters;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferBundlesImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferTagsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferTranslationsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrapper behind the {@link Offers} facade.
 *
 * @since 0.2.0
 */
public final class OffersImpl implements Offers {

    private static final String OP_GET = "get offer";
    private static final String OP_CREATE = "create offer";
    private static final String OP_EDIT = "edit offer";
    private static final String OP_DELETE_DRAFT = "delete draft offer";
    private static final String OP_CHANGE_PRICE = "change offer Buy Now price";
    private static final String OP_STREAM = "stream offers";
    private static final String OP_SMART = "get offer Smart classification";
    private static final String OP_UNFILLED = "stream offers with unfilled parameters";
    private static final String OP_RATING = "get offer rating";

    /** Offers page ≤ 1000 (spec); 100 balances round-trips against payload size. */
    private static final int PAGE_SIZE = 100;

    private static final String QUERY_NAME = "name";
    private static final String QUERY_STATUS = "publication.status";
    private static final String QUERY_FORMAT = "sellingMode.format";
    private static final String QUERY_PRICE_FROM = "sellingMode.price.amount.gte";
    private static final String QUERY_PRICE_TO = "sellingMode.price.amount.lte";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private final HttpSupport http;
    private final OfferBatch batch;
    private final PromoOptions promoOptions;

    // ---- bucket F sub-facades ----
    private final OfferTags tags;
    private final OfferTranslations translations;
    private final OfferBundles bundles;

    public OffersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.batch = new OfferBatchImpl(runtime);
        this.promoOptions = new PromoOptionsImpl(runtime);
        // [append point: offers sub-facade wiring] Bucket A constructs its own
        // sub-facades here (batch/promoOptions/media); bucket F constructs its
        // sub-facades (tags/translations/bundles/flexibleBundles/rating) from
        // this same runtime. One block per bucket, append-only, BACKLOG order.
        this.tags = new OfferTagsImpl(runtime);
        this.translations = new OfferTranslationsImpl(runtime);
        this.bundles = new OfferBundlesImpl(runtime);
    }

    @Override
    public Offer get(String offerId) {
        return Offer.from(http.getAuthenticated(
                ApiPaths.productOffer(offerId), SaleProductOfferResponseV1Raw.class, OP_GET));
    }

    /** The generated Buy Now price DTO for a {@link Money} amount. */
    private static BuyNowPriceRaw priceOf(Money money) {
        return new BuyNowPriceRaw().amount(money.amount()).currency(money.currency());
    }

    @Override
    public Offer create(CreateOfferRequest request) {
        SellingModeRaw sellingMode = new SellingModeRaw()
                .format(SellingModeFormatRaw.BUY_NOW)
                .price(priceOf(request.buyNowPrice()));
        SaleProductOfferRequestV1Raw body = new SaleProductOfferRequestV1Raw()
                .name(request.name())
                .category(new OfferCategoryRequestRaw().id(request.categoryId()))
                .sellingMode(sellingMode)
                .stock(new SaleProductOffersRequestStockRaw().available(request.availableStock()));
        if (!request.imageUrls().isEmpty()) {
            body.images(request.imageUrls());
        }
        return Offer.from(http.request(OP_CREATE)
                .post(ApiPaths.SALE_PRODUCT_OFFERS)
                .jsonBody(body)
                .fetch(SaleProductOfferResponseV1Raw.class));
    }

    @Override
    public Offer edit(String offerId, EditOfferRequest request) {
        // A partial PATCH: build only the changed fields and serialize omitting
        // null AND empty fields (jsonBodyPartial), so untouched fields — including
        // the request type's pre-initialized empty collections — are absent from
        // the wire rather than sent and reset.
        SaleProductOfferRequestV1Raw body = new SaleProductOfferRequestV1Raw();
        if (request.name() != null) {
            body.name(request.name());
        }
        if (request.buyNowPrice() != null) {
            body.sellingMode(new SellingModeRaw().price(priceOf(request.buyNowPrice())));
        }
        if (request.availableStock() != null) {
            body.stock(new SaleProductOffersRequestStockRaw().available(request.availableStock()));
        }
        if (request.imageUrls() != null) {
            body.images(request.imageUrls());
        }
        return Offer.from(http.request(OP_EDIT)
                .patch(ApiPaths.productOffer(offerId))
                .jsonBodyPartial(body)
                .fetch(SaleProductOfferResponseV1Raw.class));
    }

    @Override
    public void deleteDraft(String offerId) {
        http.request(OP_DELETE_DRAFT).delete(ApiPaths.offerDraft(offerId)).send();
    }

    @Override
    public void changeBuyNowPrice(String offerId, Money buyNowPrice) {
        // Allegro's price change is a command keyed by a client-generated id;
        // for a single offer it resolves synchronously, so one PUT suffices.
        String commandId = UUID.randomUUID().toString();
        ChangePriceWithoutOutputRaw body = new ChangePriceWithoutOutputRaw()
                .id(commandId)
                .input(new ChangePriceInputRaw().buyNowPrice(
                        new PriceRaw().amount(buyNowPrice.amount()).currency(buyNowPrice.currency())));
        http.request(OP_CHANGE_PRICE)
                .put(ApiPaths.changePriceCommand(offerId, commandId))
                .jsonBody(body)
                .send();
    }

    @Override
    public Stream<OfferSummary> streamOffers(OfferFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<OfferSummary> fetchPage(OfferFilter filter, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_NAME, filter.name())
                .add(QUERY_STATUS, wireValueOf(filter.status()))
                .add(QUERY_FORMAT, wireValueOf(filter.format()))
                .add(QUERY_PRICE_FROM, filter.priceFrom())
                .add(QUERY_PRICE_TO, filter.priceTo())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        OffersSearchResultDtoRaw response = http.request(OP_STREAM)
                .get(ApiPaths.SALE_OFFERS)
                .query(query)
                .fetch(OffersSearchResultDtoRaw.class);
        List<OfferListingDtoRaw> offers = response.getOffers();
        List<OfferSummary> items = offers == null
                ? List.of()
                : offers.stream().map(OfferSummary::from).toList();
        // The listing carries totalCount, but a full page is the robust
        // "there may be more" signal shared with the other SDK streams.
        boolean hasMore = items.size() == PAGE_SIZE;
        return new PagedSpliterator.Page<>(items, hasMore);
    }

    @Override
    public SmartClassification smartClassification(String offerId) {
        return SmartClassification.from(http.getAuthenticated(
                ApiPaths.offerSmart(offerId), SmartOfferClassificationReportRaw.class, OP_SMART));
    }

    @Override
    public Stream<UnfilledParameters> streamUnfilledParameters() {
        return PagedSpliterator.stream(this::fetchUnfilledPage);
    }

    private PagedSpliterator.Page<UnfilledParameters> fetchUnfilledPage(int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        UnfilledParametersResponseRaw response = http.request(OP_UNFILLED)
                .get(ApiPaths.SALE_OFFERS_UNFILLED_PARAMETERS)
                .query(query)
                .fetch(UnfilledParametersResponseRaw.class);
        List<UnfilledParametersResponseOffersInnerRaw> offers = response.getOffers();
        List<UnfilledParameters> items = offers == null
                ? List.of()
                : offers.stream().map(UnfilledParameters::from).toList();
        boolean hasMore = items.size() == PAGE_SIZE;
        return new PagedSpliterator.Page<>(items, hasMore);
    }

    @Override
    public OfferBatch batch() {
        return batch;
    }

    @Override
    public PromoOptions promoOptions() {
        return promoOptions;
    }

    // ---- bucket F sub-accessors ----
    @Override
    public OfferTags tags() {
        return tags;
    }

    @Override
    public OfferTranslations translations() {
        return translations;
    }

    @Override
    public OfferRating rating(String offerId) {
        return OfferRating.from(http.getAuthenticated(
                ApiPaths.offerRating(offerId), OfferRatingRaw.class, OP_RATING));
    }

    @Override
    public OfferBundles bundles() {
        return bundles;
    }

    /** The wire token for a filter enum, or {@code null} to omit it (never {@code UNKNOWN}). */
    private static @Nullable String wireValueOf(@Nullable OfferStatus status) {
        return status == null || status == OfferStatus.UNKNOWN ? null : status.name();
    }

    /** The wire token for a filter enum, or {@code null} to omit it (never {@code UNKNOWN}). */
    private static @Nullable String wireValueOf(@Nullable OfferFormat format) {
        return format == null || format == OfferFormat.UNKNOWN ? null : format.name();
    }
}
