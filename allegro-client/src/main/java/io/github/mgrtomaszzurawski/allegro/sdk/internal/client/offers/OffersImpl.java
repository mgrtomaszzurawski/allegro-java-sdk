/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferListingDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OffersSearchResultDtoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferRatingRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SalePartialProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferResponseV1Raw;
import io.github.mgrtomaszzurawski.allegro.client.model.SaleProductOfferStatusResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerOfferBaseEventRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SellerOfferEventsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.SmartOfferClassificationReportRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseOffersInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.UnfilledParametersResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.FlexibleBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferBundles;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTags;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.OfferTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferRating;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferBatch;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.OfferMedia;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.Offers;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.PromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.CreateOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.EditOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferEventFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferEvent;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferProcessingStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PartialOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PriceChangeResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SmartClassification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.UnfilledParameters;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.FlexibleBundlesImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferBundlesImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferTagsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offerextras.OfferTranslationsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.mapping.OfferRequestMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.Objects;
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
    private static final String OP_STREAM = "stream offers";
    private static final String OP_COUNT = "count offers";
    private static final String OP_SMART = "get offer Smart classification";
    private static final String OP_UNFILLED = "stream offers with unfilled parameters";
    private static final String OP_RATING = "get offer rating";
    private static final String OP_EVENTS = "stream offer events";
    private static final String OP_OPERATION_STATUS = "get offer operation status";
    private static final String OP_GET_FIELDS = "get offer parts";

    /** Offers page ≤ 1000 (spec); 100 balances round-trips against payload size. */
    private static final int PAGE_SIZE = 100;
    // A count probe fetches the first page at the smallest legal size (Allegro's `limit`
    // range is 1..1000) purely to read the response's `totalCount`.
    private static final int COUNT_PROBE_OFFSET = 0;
    private static final int COUNT_PROBE_LIMIT = 1;
    private static final long EMPTY_COUNT = 0L;

    private static final String QUERY_NAME = "name";
    private static final String QUERY_STATUS = "publication.status";
    private static final String QUERY_FORMAT = "sellingMode.format";
    private static final String QUERY_PRICE_FROM = "sellingMode.price.amount.gte";
    private static final String QUERY_PRICE_TO = "sellingMode.price.amount.lte";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_FROM = "from";
    private static final String QUERY_TYPE = "type";
    private static final String QUERY_INCLUDE = "include";
    private static final String PART_STOCK = "stock";
    private static final String PART_PRICE = "price";
    private static final String ERR_NO_PARTS = "at least one offer part is required";
    private static final String ERR_OFFER_ID = "offerId must not be null";
    /** Marker in the create/edit response Location URL that precedes the async operation id. */
    private static final String OPERATIONS_SEGMENT = "/operations/";

    private final HttpSupport http;
    private final OfferBatch batch;
    private final PromoOptions promoOptions;
    private final OfferMedia media;

    // ---- bucket F sub-facades ----
    private final OfferTags tags;
    private final OfferTranslations translations;
    private final OfferBundles bundles;
    private final FlexibleBundles flexibleBundles;

    public OffersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.batch = new OfferBatchImpl(runtime);
        this.promoOptions = new PromoOptionsImpl(runtime);
        this.media = new OfferMediaImpl(runtime);
        // [append point: offers sub-facade wiring] Bucket A constructs its own
        // sub-facades here (batch/promoOptions/media); bucket F constructs its
        // sub-facades (tags/translations/bundles/flexibleBundles/rating) from
        // this same runtime. One block per bucket, append-only, BACKLOG order.
        this.tags = new OfferTagsImpl(runtime);
        this.translations = new OfferTranslationsImpl(runtime);
        this.bundles = new OfferBundlesImpl(runtime);
        this.flexibleBundles = new FlexibleBundlesImpl(runtime);
    }

    @Override
    public Offer get(String offerId) {
        return Offer.from(http.getAuthenticated(
                ApiPaths.productOffer(offerId), SaleProductOfferResponseV1Raw.class, OP_GET));
    }

    @Override
    public PartialOffer getFields(String offerId, OfferPart... parts) {
        Objects.requireNonNull(offerId, ERR_OFFER_ID);
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException(ERR_NO_PARTS);
        }
        Query query = Query.create();
        for (OfferPart part : parts) {
            query.add(QUERY_INCLUDE, wirePart(part));
        }
        return PartialOffer.from(http.request(OP_GET_FIELDS)
                .get(ApiPaths.productOfferParts(offerId))
                .query(query)
                .fetch(SalePartialProductOfferResponseRaw.class));
    }

    private static String wirePart(OfferPart part) {
        return part == OfferPart.STOCK ? PART_STOCK : PART_PRICE;
    }

    @Override
    public Offer create(CreateOfferRequest request) {
        // jsonBodyPartial (not jsonBody): the generated request type pre-initializes
        // empty collections and leaves nullable scalars (e.g. `language`) null, and
        // Allegro rejects `language:null` with a JsonMappingException — send only the
        // fields actually set (the mapper builds them).
        var located = http.request(OP_CREATE)
                .post(ApiPaths.SALE_PRODUCT_OFFERS)
                .jsonBodyPartial(OfferRequestMapper.createBody(request))
                .fetchLocation(SaleProductOfferResponseV1Raw.class);
        return Offer.from(located.value(), operationIdFrom(located.location()));
    }

    @Override
    public Offer edit(String offerId, EditOfferRequest request) {
        // A partial PATCH: the mapper builds only the changed fields and jsonBodyPartial
        // omits null AND empty fields, so untouched fields — including the request type's
        // pre-initialized empty collections — are absent from the wire rather than reset.
        var located = http.request(OP_EDIT)
                .patch(ApiPaths.productOffer(offerId))
                .jsonBodyPartial(OfferRequestMapper.editBody(request))
                .fetchLocation(SaleProductOfferResponseV1Raw.class);
        return Offer.from(located.value(), operationIdFrom(located.location()));
    }

    /**
     * The async create/edit operation id is the segment after {@code /operations/} in the response
     * {@code Location} URL ({@code .../sale/product-offers/{offerId}/operations/{operationId}}), or
     * {@code null} when the server sends no such header (or one that is not an operations URL).
     */
    private static @Nullable String operationIdFrom(@Nullable String location) {
        if (location == null) {
            return null;
        }
        int marker = location.indexOf(OPERATIONS_SEGMENT);
        if (marker < 0) {
            return null;
        }
        String operationId = location.substring(marker + OPERATIONS_SEGMENT.length());
        return operationId.isBlank() ? null : operationId;
    }

    @Override
    public void deleteDraft(String offerId) {
        http.request(OP_DELETE_DRAFT).delete(ApiPaths.offerDraft(offerId)).send();
    }

    @Override
    public PriceChangeResult changeBuyNowPrice(String offerId, Money buyNowPrice) {
        // A single-offer price change resolves synchronously; the command's *Raw
        // request/response assembly lives in ChangePriceCommand to keep this wrapper lean.
        return ChangePriceCommand.apply(http, offerId, buyNowPrice);
    }

    @Override
    public Stream<OfferSummary> streamOffers(OfferFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchPage(filter, pageIndex));
    }

    @Override
    public long countOffers(OfferFilter filter) {
        // Fetch a single minimal page purely to read the server's totalCount — a constant
        // number of matching offers is returned regardless of how large the result set is,
        // so this stays O(1) rather than paging the whole listing.
        Query query = filterQuery(filter)
                .add(QUERY_OFFSET, COUNT_PROBE_OFFSET)
                .add(QUERY_LIMIT, COUNT_PROBE_LIMIT);
        OffersSearchResultDtoRaw response = http.request(OP_COUNT)
                .get(ApiPaths.SALE_OFFERS)
                .query(query)
                .fetch(OffersSearchResultDtoRaw.class);
        Integer totalCount = response.getTotalCount();
        // The offers listing always carries totalCount (ADR-010 only wires this accessor where the
        // server reports one); the null branch is a defensive guard, not a supported "no total" signal.
        return totalCount == null ? EMPTY_COUNT : totalCount.longValue();
    }

    private Query filterQuery(OfferFilter filter) {
        return Query.create()
                .add(QUERY_NAME, filter.name())
                .add(QUERY_STATUS, wireValueOf(filter.status()))
                .add(QUERY_FORMAT, wireValueOf(filter.format()))
                .add(QUERY_PRICE_FROM, filter.priceFrom())
                .add(QUERY_PRICE_TO, filter.priceTo());
    }

    private PagedSpliterator.Page<OfferSummary> fetchPage(OfferFilter filter, int pageIndex) {
        Query query = filterQuery(filter)
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
    public Stream<OfferEvent> streamEvents(OfferEventFilter filter) {
        return PagedSpliterator.cursorStream(cursor -> fetchEventPage(filter, cursor));
    }

    private PagedSpliterator.CursorPage<OfferEvent> fetchEventPage(OfferEventFilter filter,
            @Nullable String from) {
        Query query = Query.create()
                .add(QUERY_FROM, from)
                .add(QUERY_LIMIT, PAGE_SIZE)
                .add(QUERY_TYPE, filter.type());
        SellerOfferEventsResponseRaw response = http.request(OP_EVENTS)
                .get(ApiPaths.SALE_OFFER_EVENTS)
                .query(query)
                .fetch(SellerOfferEventsResponseRaw.class);
        List<SellerOfferBaseEventRaw> events = response.getOfferEvents();
        List<OfferEvent> items = events == null
                ? List.of()
                : events.stream().map(OfferEvent::from).toList();
        // A full page means there may be more; advance the cursor to the last event id.
        String nextCursor = items.size() == PAGE_SIZE ? items.get(items.size() - 1).id() : null;
        return new PagedSpliterator.CursorPage<>(items, nextCursor);
    }

    @Override
    public OfferProcessingStatus operationStatus(String offerId, String operationId) {
        return OfferProcessingStatus.from(http.getAuthenticated(
                ApiPaths.offerOperation(offerId, operationId),
                SaleProductOfferStatusResponseRaw.class, OP_OPERATION_STATUS));
    }

    @Override
    public OfferBatch batch() {
        return batch;
    }

    @Override
    public PromoOptions promoOptions() {
        return promoOptions;
    }

    @Override
    public OfferMedia media() {
        return media;
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

    @Override
    public FlexibleBundles flexibleBundles() {
        return flexibleBundles;
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
