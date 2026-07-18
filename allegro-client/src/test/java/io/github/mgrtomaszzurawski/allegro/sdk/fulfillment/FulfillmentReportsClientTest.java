/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.RefundDispositionFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.StockFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AccountableParty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AvailableProduct;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.FulfillmentOrder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundActionState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundDisposition;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundDispositionType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RefundStockStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReserveStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StockItem;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.StorageFeeStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.TaxId;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the fulfillment reports surface (stock, available products,
 * parcels, refund dispositions) and the tax-id resource on {@code
 * client.fulfillment()} — lazy pagination (count-based for stock/products,
 * short-page for refund dispositions), filter propagation across page
 * boundaries, nested record mapping, and the void tax-id writes. The mandatory
 * error-path table for this facade lives in {@link FulfillmentClientTest}
 * (removal endpoint), so it is not repeated here.
 */
@WireMockTest
class FulfillmentReportsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;

    private static final String STOCK_PATH = "/fulfillment/stock";
    private static final String AVAILABLE_PRODUCTS_PATH = "/fulfillment/available-products";
    private static final String REFUND_DISPOSITIONS_PATH = "/fulfillment/returns/refund-dispositions";
    private static final String TAX_ID_PATH = "/fulfillment/tax-id";
    private static final String ORDER_ID = "abc-order-123";
    private static final String PARCELS_PATH = "/fulfillment/orders/" + ORDER_ID + "/parcels";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_PHRASE = "phrase";
    private static final String QUERY_CREATED_GTE = "createdAt.gte";
    private static final String OFFSET_FIRST = "0";
    private static final String OFFSET_SECOND = "100";
    private static final String OFFSET_THIRD = "200";
    private static final String TEST_PHRASE = "headphones";

    private static final String TEST_PRODUCT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String TEST_PRODUCT_NAME = "Wireless Headphones";
    private static final String TEST_OFFER_ID = "offer-9";
    private static final String TEST_TAX_ID = "PL1234567890";
    private static final String TEST_VERIFICATION_STATUS = "VERIFIED";
    private static final String JSON_PATH_TAX_ID = "$.taxId";

    private static final String TEST_WAYBILL = "WB-0001";
    private static final String TEST_ITEM_PRODUCT_ID = "prod-1";
    private static final String TEST_SERIAL_ONE = "SN-1";
    private static final String TEST_SERIAL_TWO = "SN-2";
    private static final LocalDate TEST_EXPIRATION = LocalDate.of(2027, 1, 31);
    private static final int TEST_ITEM_QUANTITY = 3;

    private static final String TEST_BUYER_LOGIN = "buyer-login";
    private static final int TEST_REFUND_QUANTITY = 2;
    private static final OffsetDateTime CREATED_FROM =
            OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    // spec-derived: not yet wire-verified (One Fulfillment requires an enrolled
    // seller account; a sandbox capture during the bucket's exploration pass
    // confirms or corrects these shapes before the bucket's final PR).
    private static final String STOCK_RICH_RESPONSE = """
            {"stock":[{
              "product":{"id":"%s","name":"%s","gtins":["0123456789012"],"image":"http://img/1"},
              "quantity":{"available":10,"onOrder":2,"onHold":1},
              "sellingStats":{"lastFourteenDaysAverage":1.5,"lastThirtyDaysSum":40},
              "reserve":{"outOfStockIn":30,"status":"NORMAL"},
              "storageFee":{"status":"CHARGED","feeStatusAt":"2026-07-01T00:00:00Z",
               "details":{"chargedItemsQuantity":3,"amountNet":2.5,"amountGross":3.08,"currency":"PLN"}},
              "offerId":"%s"
            }],"count":1,"totalCount":1}
            """.formatted(TEST_PRODUCT_ID, TEST_PRODUCT_NAME, TEST_OFFER_ID);

    private static final String AVAILABLE_PRODUCT_RESPONSE = """
            {"products":[{"id":"%s","name":"%s","gtins":["0123456789012"],"image":"http://img/1"}],
             "count":1,"totalCount":1}
            """.formatted(TEST_PRODUCT_ID, TEST_PRODUCT_NAME);

    private static final String REFUND_RICH_RESPONSE = """
            {"report":[{
              "type":"RETURN","refund":{"status":"COMPLETED","details":"ACTION_NEEDED"},
              "stockStatus":"NON_SELLABLE","verificationStatus":"CONFIRMED",
              "accountableForNonSellability":"WAREHOUSE","orderId":"%s","offerId":"%s",
              "product":{"gtins":["0123456789012"],"name":"%s","quantity":%d},
              "buyer":{"login":"%s"},"createdAt":"2026-07-10T12:00:00Z"
            }]}
            """.formatted(ORDER_ID, TEST_OFFER_ID, TEST_PRODUCT_NAME, TEST_REFUND_QUANTITY, TEST_BUYER_LOGIN);

    private static final String PARCELS_RESPONSE = """
            {"orderId":"%s","parcels":[{"waybill":"%s","items":[
              {"productId":"%s","offerId":"%s","quantity":%d,
               "serialNumbers":["%s","%s"],"expirationDate":"2027-01-31"}]}]}
            """.formatted(ORDER_ID, TEST_WAYBILL, TEST_ITEM_PRODUCT_ID, TEST_OFFER_ID,
            TEST_ITEM_QUANTITY, TEST_SERIAL_ONE, TEST_SERIAL_TWO);

    private static final String TAX_ID_RESPONSE = """
            {"taxId":"%s","verificationStatus":"%s"}
            """.formatted(TEST_TAX_ID, TEST_VERIFICATION_STATUS);

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(RetryPolicy.builder().enabled(false).build())
                        .build());
    }

    /** A stock page of {@code count} minimal items, advertising {@code totalCount}. */
    private static String stockPage(int count, int totalCount) {
        StringBuilder json = new StringBuilder("{\"stock\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{}");
        }
        return json.append("],\"count\":").append(count)
                .append(",\"totalCount\":").append(totalCount).append('}').toString();
    }

    /** An available-products page of {@code count} minimal items. */
    private static String availableProductsPage(int count, int totalCount) {
        StringBuilder json = new StringBuilder("{\"products\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{}");
        }
        return json.append("],\"count\":").append(count)
                .append(",\"totalCount\":").append(totalCount).append('}').toString();
    }

    /** A refund-dispositions page of {@code count} minimal rows (no count/totalCount). */
    private static String refundPage(int count) {
        StringBuilder json = new StringBuilder("{\"report\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{}");
        }
        return json.append("]}").toString();
    }

    private static void stubStockPage(String offset, String body) {
        stubFor(get(urlPathEqualTo(STOCK_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withQueryParam(QUERY_OFFSET, equalTo(offset))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(body)));
    }

    // ---- stock ----

    @Test
    void stock_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies more, but only the first is consumed
        stubStockPage(OFFSET_FIRST, stockPage(PAGE_SIZE, 500));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<StockItem> firstOnly = allegro.fulfillment().stock().limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST)));
            verify(0, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
        }
    }

    @Test
    void stock_whenOffsetReachesTotalCount_stopsWithoutExtraFetch(WireMockRuntimeInfo wmInfo) {
        // given — two full pages, total 200; count-based termination must stop at
        // 200 even though the last page is FULL (a short-page walk would over-fetch).
        stubStockPage(OFFSET_FIRST, stockPage(PAGE_SIZE, 200));
        stubStockPage(OFFSET_SECOND, stockPage(PAGE_SIZE, 200));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.fulfillment().stock().count();

            // then
            assertEquals(200L, total);
            verify(1, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
            verify(0, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_THIRD)));
        }
    }

    @Test
    void stock_whenFiltered_propagatesFilterAcrossPageBoundary(WireMockRuntimeInfo wmInfo) {
        // given — the phrase filter must ride along on the second page too
        stubFor(get(urlPathEqualTo(STOCK_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .withQueryParam(QUERY_PHRASE, equalTo(TEST_PHRASE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(stockPage(PAGE_SIZE, 200))));
        stubFor(get(urlPathEqualTo(STOCK_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .withQueryParam(QUERY_PHRASE, equalTo(TEST_PHRASE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(stockPage(PAGE_SIZE, 200))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.fulfillment()
                    .stock(StockFilter.builder().phrase(TEST_PHRASE).build())
                    .count();

            // then — both pages carried the filter
            assertEquals(200L, total);
            verify(1, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                    .withQueryParam(QUERY_PHRASE, equalTo(TEST_PHRASE)));
        }
    }

    @Test
    void stock_whenItemHasAllFields_mapsNestedTree(WireMockRuntimeInfo wmInfo) {
        // given
        stubStockPage(OFFSET_FIRST, STOCK_RICH_RESPONSE);

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            StockItem item = allegro.fulfillment().stock().toList().get(0);

            // then — the nested product/reserve/fee tree and enums survive mapping
            assertEquals(TEST_PRODUCT_ID, item.product().id());
            assertEquals(TEST_PRODUCT_NAME, item.product().name());
            assertEquals(TEST_OFFER_ID, item.offerId());
            assertEquals(ReserveStatus.NORMAL, item.reserve().status());
            assertEquals(StorageFeeStatus.CHARGED, item.storageFee().status());
            assertEquals("2.5", item.storageFee().details().netAmount().amount());
            assertEquals("PLN", item.storageFee().details().netAmount().currency());
        }
    }

    // ---- available products ----

    @Test
    void availableProducts_whenOffsetReachesTotalCount_stopsWithoutExtraFetch(WireMockRuntimeInfo wmInfo) {
        // given — page one full, page two short, total 150
        stubFor(get(urlPathEqualTo(AVAILABLE_PRODUCTS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(availableProductsPage(PAGE_SIZE, 150))));
        stubFor(get(urlPathEqualTo(AVAILABLE_PRODUCTS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(availableProductsPage(50, 150))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.fulfillment().availableProducts().count();

            // then
            assertEquals(150L, total);
            verify(0, getRequestedFor(urlPathEqualTo(AVAILABLE_PRODUCTS_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_THIRD)));
        }
    }

    @Test
    void availableProducts_whenProductReturned_mapsFields(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(AVAILABLE_PRODUCTS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(AVAILABLE_PRODUCT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AvailableProduct product = allegro.fulfillment().availableProducts().toList().get(0);

            // then
            assertEquals(TEST_PRODUCT_ID, product.id());
            assertEquals(TEST_PRODUCT_NAME, product.name());
            assertEquals(1, product.gtins().size());
        }
    }

    // ---- refund dispositions ----

    @Test
    void refundDispositions_whenFullLastPage_makesOneExtraEmptyFetch(WireMockRuntimeInfo wmInfo) {
        // given — a full page (no count/totalCount) forces one extra fetch that
        // comes back empty; short-page termination then stops.
        stubFor(get(urlPathEqualTo(REFUND_DISPOSITIONS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(refundPage(PAGE_SIZE))));
        stubFor(get(urlPathEqualTo(REFUND_DISPOSITIONS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(refundPage(0))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — the no-arg overload walks the whole report
            long total = allegro.fulfillment().refundDispositions().count();

            // then — the extra (empty) page was fetched, and a third was not
            assertEquals(PAGE_SIZE, total);
            verify(1, getRequestedFor(urlPathEqualTo(REFUND_DISPOSITIONS_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
            verify(0, getRequestedFor(urlPathEqualTo(REFUND_DISPOSITIONS_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_THIRD)));
        }
    }

    @Test
    void refundDispositions_whenFiltered_propagatesCreatedAtBound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(REFUND_DISPOSITIONS_PATH))
                .withQueryParam(QUERY_CREATED_GTE, equalTo(CREATED_FROM.toString()))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(refundPage(0))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.fulfillment().refundDispositions(
                    RefundDispositionFilter.builder().createdFrom(CREATED_FROM).build()).count();

            // then — the creation-time lower bound reached the wire
            assertEquals(0L, total);
            verify(1, getRequestedFor(urlPathEqualTo(REFUND_DISPOSITIONS_PATH))
                    .withQueryParam(QUERY_CREATED_GTE, equalTo(CREATED_FROM.toString())));
        }
    }

    @Test
    void refundDispositions_whenRowHasAllFields_mapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(REFUND_DISPOSITIONS_PATH)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(REFUND_RICH_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RefundDisposition row = allegro.fulfillment()
                    .refundDispositions(RefundDispositionFilter.all()).toList().get(0);

            // then — enums, verification status (free-form string) and sub-records map
            assertEquals(RefundDispositionType.RETURN, row.type());
            assertEquals(RefundStockStatus.NON_SELLABLE, row.stockStatus());
            assertEquals(AccountableParty.WAREHOUSE, row.accountableForNonSellability());
            assertEquals("CONFIRMED", row.verificationStatus());
            assertEquals(RefundActionState.ACTION_NEEDED, row.refund().details());
            assertEquals(TEST_REFUND_QUANTITY, row.product().quantity());
            assertEquals(TEST_BUYER_LOGIN, row.buyer().login());
        }
    }

    // ---- parcels ----

    @Test
    void parcelsOf_whenOrderHasParcels_mapsNested(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PARCELS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PARCELS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            FulfillmentOrder order = allegro.fulfillment().parcelsOf(ORDER_ID);

            // then — the parcel/item tree survives, including serials and expiry
            assertEquals(ORDER_ID, order.orderId());
            assertEquals(1, order.parcels().size());
            assertEquals(TEST_WAYBILL, order.parcels().get(0).waybill());
            assertEquals(TEST_ITEM_QUANTITY, order.parcels().get(0).items().get(0).quantity());
            assertEquals(2, order.parcels().get(0).items().get(0).serialNumbers().size());
            assertEquals(TEST_EXPIRATION, order.parcels().get(0).items().get(0).expirationDate());
            verify(1, getRequestedFor(urlEqualTo(PARCELS_PATH)));
        }
    }

    // ---- tax id ----

    @Test
    void taxId_whenRegistered_mapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TAX_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAX_ID_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            TaxId taxId = allegro.fulfillment().taxId();

            // then
            assertEquals(TEST_TAX_ID, taxId.taxId());
            assertEquals(TEST_VERIFICATION_STATUS, taxId.verificationStatus());
        }
    }

    @Test
    void addTaxId_whenCalled_postsVendorBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TAX_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.fulfillment().addTaxId(TEST_TAX_ID);

            // then — the vendor content type and the single-field body reached the wire
            verify(1, postRequestedFor(urlEqualTo(TAX_ID_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                    .withRequestBody(matchingJsonPath(JSON_PATH_TAX_ID, equalTo(TEST_TAX_ID))));
        }
    }

    @Test
    void updateTaxId_whenCalled_putsVendorBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(TAX_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.fulfillment().updateTaxId(TEST_TAX_ID);

            // then
            verify(1, putRequestedFor(urlEqualTo(TAX_ID_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PATH_TAX_ID, equalTo(TEST_TAX_ID))));
        }
    }

    @Test
    void addTaxId_whenNull_throwsBeforeSending(WireMockRuntimeInfo wmInfo) {
        // given
        try (AllegroClient allegro = client(wmInfo)) {
            var fulfillment = allegro.fulfillment();

            // then — fail-fast, no request leaves the SDK
            assertThrows(NullPointerException.class, () -> fulfillment.addTaxId(null));
            verify(0, postRequestedFor(urlEqualTo(TAX_ID_PATH)));
        }
    }

    @Test
    void stock_whenEmptyReport_returnsEmptyStreamWithoutExtraFetch(WireMockRuntimeInfo wmInfo) {
        // given — an empty first page terminates the walk immediately
        stubStockPage(OFFSET_FIRST, stockPage(0, 0));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<StockItem> items = allegro.fulfillment().stock().toList();

            // then — no elements, and the walk did not request a second page
            assertEquals(List.of(), items);
            verify(0, getRequestedFor(urlPathEqualTo(STOCK_PATH))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
        }
    }
}
