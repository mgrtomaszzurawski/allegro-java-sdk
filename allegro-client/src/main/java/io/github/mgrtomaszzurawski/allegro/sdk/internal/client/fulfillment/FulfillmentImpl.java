/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.fulfillment;

import io.github.mgrtomaszzurawski.allegro.client.model.AvailableProductResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AvailableProductsListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRefundDispositionsResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentRemovalPreferenceRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.FulfillmentWithdrawalAddressRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PhoneNumberWithCountryCodeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockProductItemRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.StockProductListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxIdRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TaxIdResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.AdvanceShipNotices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.Fulfillment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.StockFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AvailableProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.FulfillmentOrder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundDisposition;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StockItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.TaxId;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Endpoint wrappers behind the {@link Fulfillment} facade. Maps the public
 * domain records to and from the generated {@code *Raw} DTOs and drives the
 * shared transport.
 *
 * @since 0.2.0
 */
public final class FulfillmentImpl implements Fulfillment {

    /** Allegro caps {@code limit} at 1000; 100 keeps each page small but few. */
    private static final int PAGE_SIZE = 100;

    private static final String OP_GET_REMOVAL_PREFERENCE = "get fulfillment removal preference";
    private static final String OP_SET_REMOVAL_PREFERENCE = "set fulfillment removal preference";
    private static final String OP_STREAM_STOCK = "stream fulfillment stock";
    private static final String OP_STREAM_AVAILABLE_PRODUCTS = "stream fulfillment available products";
    private static final String OP_GET_PARCELS = "get fulfillment order parcels";
    private static final String OP_STREAM_REFUND_DISPOSITIONS = "stream fulfillment refund dispositions";
    private static final String OP_GET_TAX_ID = "get fulfillment tax id";
    private static final String OP_ADD_TAX_ID = "add fulfillment tax id";
    private static final String OP_UPDATE_TAX_ID = "update fulfillment tax id";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_PHRASE = "phrase";
    private static final String QUERY_SORT = "sort";
    private static final String QUERY_PRODUCT_ID = "productId";
    private static final String QUERY_PRODUCT_AVAILABILITY = "productAvailability";
    private static final String QUERY_PRODUCT_STATUS = "productStatus";
    private static final String QUERY_ASN_STATUS = "asnStatus";
    private static final String QUERY_OUT_OF_STOCK_IN_FROM = "outOfStockInFrom";
    private static final String QUERY_OUT_OF_STOCK_IN_TO = "outOfStockInTo";
    private static final String QUERY_CREATED_GTE = "createdAt.gte";
    private static final String QUERY_CREATED_LTE = "createdAt.lte";

    private static final String ERR_PREFERENCE_NULL = "preference must not be null";
    private static final String ERR_FILTER_NULL = "filter must not be null";
    private static final String ERR_ORDER_ID_NULL = "orderId must not be null";
    private static final String ERR_TAX_ID_NULL = "taxId must not be null";

    private final HttpSupport http;
    private final AdvanceShipNotices advanceShipNotices;

    public FulfillmentImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
        this.advanceShipNotices = new AdvanceShipNoticesImpl(runtime);
    }

    @Override
    public AdvanceShipNotices advanceShipNotices() {
        return advanceShipNotices;
    }

    @Override
    public RemovalPreference removalPreference() {
        return RemovalPreference.from(
                http.getAuthenticated(ApiPaths.FULFILLMENT_REMOVAL_PREFERENCES,
                        FulfillmentRemovalPreferenceRaw.class, OP_GET_REMOVAL_PREFERENCE));
    }

    @Override
    public RemovalPreference setRemovalPreference(RemovalPreference preference) {
        Objects.requireNonNull(preference, ERR_PREFERENCE_NULL);
        return RemovalPreference.from(
                http.putJsonAuthenticated(ApiPaths.FULFILLMENT_REMOVAL_PREFERENCES,
                        toRaw(preference), FulfillmentRemovalPreferenceRaw.class,
                        OP_SET_REMOVAL_PREFERENCE));
    }

    @Override
    public Stream<StockItem> stock() {
        return stock(StockFilter.all());
    }

    @Override
    public Stream<StockItem> stock(StockFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.stream(pageIndex -> fetchStockPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<StockItem> fetchStockPage(StockFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_PHRASE, filter.phrase())
                .add(QUERY_SORT, filter.sort())
                .add(QUERY_PRODUCT_ID, filter.productId())
                .add(QUERY_PRODUCT_AVAILABILITY, filter.productAvailability())
                .add(QUERY_PRODUCT_STATUS, filter.productStatus())
                .add(QUERY_ASN_STATUS, filter.asnStatus())
                .add(QUERY_OUT_OF_STOCK_IN_FROM, filter.outOfStockInFrom())
                .add(QUERY_OUT_OF_STOCK_IN_TO, filter.outOfStockInTo())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        StockProductListRaw response = http.request(OP_STREAM_STOCK)
                .get(ApiPaths.FULFILLMENT_STOCK)
                .query(query)
                .fetch(StockProductListRaw.class);
        List<StockProductItemRaw> stock = response.getStock();
        List<StockItem> items = stock == null
                ? List.of()
                : stock.stream().map(StockItem::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    @Override
    public Stream<AvailableProduct> availableProducts() {
        return PagedSpliterator.stream(this::fetchAvailableProductsPage);
    }

    private PagedSpliterator.Page<AvailableProduct> fetchAvailableProductsPage(int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        AvailableProductsListRaw response = http.request(OP_STREAM_AVAILABLE_PRODUCTS)
                .get(ApiPaths.FULFILLMENT_AVAILABLE_PRODUCTS)
                .query(query)
                .fetch(AvailableProductsListRaw.class);
        List<AvailableProductResponseRaw> products = response.getProducts();
        List<AvailableProduct> items = products == null
                ? List.of()
                : products.stream().map(AvailableProduct::from).toList();
        return new PagedSpliterator.Page<>(items, hasMore(offset, items.size(), response.getTotalCount()));
    }

    @Override
    public FulfillmentOrder parcelsOf(String orderId) {
        Objects.requireNonNull(orderId, ERR_ORDER_ID_NULL);
        return FulfillmentOrder.from(http.getAuthenticated(
                ApiPaths.fulfillmentOrderParcels(orderId), FulfillmentOrderRaw.class, OP_GET_PARCELS));
    }

    @Override
    public Stream<RefundDisposition> refundDispositions() {
        return refundDispositions(RefundDispositionFilter.all());
    }

    @Override
    public Stream<RefundDisposition> refundDispositions(RefundDispositionFilter filter) {
        Objects.requireNonNull(filter, ERR_FILTER_NULL);
        return PagedSpliterator.stream(pageIndex -> fetchRefundDispositionsPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<RefundDisposition> fetchRefundDispositionsPage(
            RefundDispositionFilter filter, int pageIndex) {
        int offset = pageIndex * PAGE_SIZE;
        Query query = Query.create()
                .add(QUERY_CREATED_GTE, filter.createdFrom())
                .add(QUERY_CREATED_LTE, filter.createdTo())
                .add(QUERY_OFFSET, offset)
                .add(QUERY_LIMIT, PAGE_SIZE);
        FulfillmentRefundDispositionsResponseRaw response = http.request(OP_STREAM_REFUND_DISPOSITIONS)
                .get(ApiPaths.FULFILLMENT_REFUND_DISPOSITIONS)
                .query(query)
                .fetch(FulfillmentRefundDispositionsResponseRaw.class);
        List<FulfillmentRefundDispositionRaw> report = response.getReport();
        List<RefundDisposition> items = report == null
                ? List.of()
                : report.stream().map(RefundDisposition::from).toList();
        // The refund-dispositions response carries no count/totalCount, so a full
        // page is the only signal that another page may follow.
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public TaxId taxId() {
        return TaxId.from(http.getAuthenticated(
                ApiPaths.FULFILLMENT_TAX_ID, TaxIdResponseRaw.class, OP_GET_TAX_ID));
    }

    @Override
    public void addTaxId(String taxId) {
        http.request(OP_ADD_TAX_ID)
                .post(ApiPaths.FULFILLMENT_TAX_ID)
                .jsonBody(taxIdRequest(taxId))
                .send();
    }

    @Override
    public void updateTaxId(String taxId) {
        http.request(OP_UPDATE_TAX_ID)
                .put(ApiPaths.FULFILLMENT_TAX_ID)
                .jsonBody(taxIdRequest(taxId))
                .send();
    }

    /**
     * Count-based has-more: another page exists while the walked offset has not
     * yet reached the reported total. A missing total falls back to the
     * full-page heuristic so the walk still terminates.
     */
    private static boolean hasMore(int offset, int returnedCount, @Nullable BigDecimal totalCount) {
        if (totalCount == null) {
            return returnedCount == PAGE_SIZE;
        }
        return (long) offset + returnedCount < totalCount.longValue();
    }

    private static TaxIdRequestRaw taxIdRequest(String taxId) {
        Objects.requireNonNull(taxId, ERR_TAX_ID_NULL);
        return new TaxIdRequestRaw().taxId(taxId);
    }

    private static FulfillmentRemovalPreferenceRaw toRaw(RemovalPreference preference) {
        FulfillmentRemovalPreferenceRaw raw = new FulfillmentRemovalPreferenceRaw();
        raw.setOperation(FulfillmentRemovalPreferenceRaw.OperationEnum
                .fromValue(preference.operation().wireValue()));
        raw.setAddress(toRaw(preference.withdrawalAddress()));
        return raw;
    }

    private static @Nullable FulfillmentWithdrawalAddressRaw toRaw(@Nullable WithdrawalAddress address) {
        if (address == null) {
            return null;
        }
        FulfillmentWithdrawalAddressRaw raw = new FulfillmentWithdrawalAddressRaw();
        raw.setCompany(address.company());
        raw.setStreet(address.street());
        raw.setPostalCode(address.postalCode());
        raw.setCity(address.city());
        raw.setCountryCode(address.countryCode());
        raw.setPhone(toRaw(address.phone()));
        raw.setAdditionalInfo(address.additionalInfo());
        return raw;
    }

    private static PhoneNumberWithCountryCodeRaw toRaw(PhoneNumber phone) {
        PhoneNumberWithCountryCodeRaw raw = new PhoneNumberWithCountryCodeRaw();
        raw.setCountryCode(phone.countryCode());
        raw.setNumber(phone.number());
        return raw;
    }
}
