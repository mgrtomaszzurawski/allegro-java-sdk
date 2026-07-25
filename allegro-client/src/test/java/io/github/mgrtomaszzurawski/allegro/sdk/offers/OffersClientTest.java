/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferPart;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.Offer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PartialOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ProductSetElement;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SafetyInformation;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OffersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ArrayForObjectHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.StrictOneOfModule;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.UnknownSubtypeToBaseHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@WireMockTest
class OffersClientTest {

    private static final String TEST_TOKEN = "offers-test-token";
    private static final String OFFER_ID = "13579";
    private static final String GET_PATH = "/sale/product-offers/" + OFFER_ID;
    private static final String CHANGE_PRICE_PATH_PATTERN =
            "/offers/" + OFFER_ID + "/change-price-commands/[0-9a-fA-F-]{36}";
    private static final String OFFER_FIXTURE = "offers/product-offer.json";
    private static final String LIVE_OFFER_ID = "7781898446";
    private static final String LIVE_GET_PATH = "/sale/product-offers/" + LIVE_OFFER_ID;
    private static final String LIVE_OFFER_FIXTURE = "offers/product-offer-live.json";
    private static final String LIVE_BASE_MARKETPLACE = "allegro-pl";
    private static final MessageToSellerMode LIVE_MESSAGE_MODE = MessageToSellerMode.OPTIONAL;
    private static final String LIVE_INVOICE_TYPE = "VAT";
    private static final String NEW_AMOUNT = "149.50";
    private static final String CURRENCY_PLN = "PLN";
    private static final String PARTS_BOTH_URL =
            "/sale/product-offers/" + OFFER_ID + "/parts?include=stock&include=price";
    private static final String PARTS_STOCK_URL =
            "/sale/product-offers/" + OFFER_ID + "/parts?include=stock";
    private static final String PARTS_PATH_PATTERN = "/sale/product-offers/" + OFFER_ID + "/parts";
    private static final String MARKETPLACE_CZ = "allegro-cz";
    private static final int PARTIAL_STOCK = 42;
    private static final String PARTIAL_PRICE = "129.00";
    private static final String PARTIAL_BOTH_BODY =
            "{\"id\":\"" + OFFER_ID + "\",\"stock\":{\"available\":" + PARTIAL_STOCK + "},"
                    + "\"sellingMode\":{\"price\":{\"amount\":\"" + PARTIAL_PRICE + "\",\"currency\":\"PLN\"}},"
                    + "\"additionalMarketplaces\":{\"" + MARKETPLACE_CZ
                    + "\":{\"sellingMode\":{\"price\":{\"amount\":\"3200.00\",\"currency\":\"CZK\"}}}}}";
    private static final String PARTIAL_STOCK_BODY =
            "{\"id\":\"" + OFFER_ID + "\",\"stock\":{\"available\":" + PARTIAL_STOCK + "}}";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"offer not found\"}]}";
    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"PRICE_TOO_LOW\",\"message\":\"price below minimum\","
                    + "\"path\":\"input.buyNowPrice\"}]}";

    private static OffersImpl offers(WireMockRuntimeInfo wmInfo) {
        // Mirror the production AllegroClient mapper (modules + tolerance handlers) so a
        // wire-accurate fixture exercises the same deserialization path the SDK uses live.
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
                .registerModule(new StrictOneOfModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addHandler(new UnknownSubtypeToBaseHandler())
                .addHandler(new ArrayForObjectHandler());
        RetryHandler retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        HttpRuntime runtime = new HttpRuntime() {
            @Override public String baseUrl() {
                return wmInfo.getHttpBaseUrl();
            }

            @Override public RetryHandler retryHandler() {
                return retryHandler;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return AllegroExecutionInterceptor.noop();
            }

            @Override public ObjectMapper objectMapper() {
                return mapper;
            }

            @Override public Duration readTimeout() {
                return Duration.ofSeconds(5);
            }

            @Override public String requireToken() {
                return TEST_TOKEN;
            }

            @Override public void reauthenticate() {
                // no-op: 401 replay is covered in the transport suite
            }
        };
        return new OffersImpl(runtime);
    }

    @Test
    void get_whenOfferExists_returnsMappedOffer(WireMockRuntimeInfo wmInfo) {
        // given — the sandbox-shaped product-offer payload
        stubFor(get(urlEqualTo(GET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(OFFER_FIXTURE)));

        // when
        Offer offer = offers(wmInfo).get(OFFER_ID);

        // then — every mapped field, including the shared Money type and enums
        assertEquals(OFFER_ID, offer.id());
        assertEquals("Mechaniczna klawiatura RGB — test SDK", offer.name());
        assertEquals("257", offer.categoryId());
        assertEquals(OfferFormat.BUY_NOW, offer.format());
        assertEquals(OfferStatus.ACTIVE, offer.status());
        assertEquals(Money.of("199.99", CURRENCY_PLN), offer.buyNowPrice());
        assertEquals(7, offer.availableStock());
    }

    @Test
    void get_whenRealSandboxOfferWithArrayWarnings_deserializesThroughSdk(WireMockRuntimeInfo wmInfo) {
        // given — the wire-accurate sandbox capture whose `warnings` is [] (the spec types it
        // as an object); without ArrayForObjectHandler this read throws MismatchedInputException
        // and fails the whole offer. Regression guard for the live-found deserialization bug.
        stubFor(get(urlEqualTo(LIVE_GET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(LIVE_OFFER_FIXTURE)));

        // when — the real offer reads through the SDK without failing
        Offer offer = offers(wmInfo).get(LIVE_OFFER_ID);

        // then — core fields map from the real payload
        assertEquals(LIVE_OFFER_ID, offer.id());
        assertEquals("[A-seed] Nauka duza ksiazka dla malych dzieci", offer.name());
        assertEquals("66781", offer.categoryId());
        assertEquals(OfferFormat.BUY_NOW, offer.format());
        assertEquals(OfferStatus.ACTIVE, offer.status());
        assertEquals(Money.of("29.99", CURRENCY_PLN), offer.buyNowPrice());
        assertEquals(5, offer.availableStock());
        // publication details map from the real payload (republish flag + base marketplace)
        assertNotNull(offer.publication());
        assertEquals(Boolean.FALSE, offer.publication().republish());
        assertEquals(LIVE_BASE_MARKETPLACE, offer.publication().baseMarketplaceId());
        // response metadata the server fills with defaults maps too
        assertNotNull(offer.messageToSellerSettings());
        assertEquals(LIVE_MESSAGE_MODE, offer.messageToSellerSettings().mode());
        assertNotNull(offer.payments());
        assertEquals(LIVE_INVOICE_TYPE, offer.payments().invoice());
        // the productSet's GPSR safety information (a oneOf) resolves to the TEXT form end-to-end
        ProductSetElement boundProduct = offer.productSet().get(0);
        assertNotNull(boundProduct.safetyInformation());
        assertEquals(SafetyInformation.TEXT, boundProduct.safetyInformation().type());
        assertNotNull(boundProduct.safetyInformation().description());
        // Allegro's validation block maps (this active offer validated cleanly, timestamp present)
        assertNotNull(offer.validation());
        assertNotNull(offer.validation().validatedAt());
        assertEquals(0, offer.validation().errors().size());
        assertEquals(0, offer.validation().warnings().size());
    }

    @Test
    void get_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(GET_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class, () -> offers(wmInfo).get(OFFER_ID));
    }

    @Test
    void changeBuyNowPrice_whenAccepted_putsPriceCommandWithBody(WireMockRuntimeInfo wmInfo) {
        // given — the command id is a client-generated UUID in the path
        stubFor(put(urlPathMatching(CHANGE_PRICE_PATH_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        // when
        offers(wmInfo).changeBuyNowPrice(OFFER_ID, Money.of(NEW_AMOUNT, CURRENCY_PLN));

        // then — exactly one PUT carrying the new Buy Now price in the command body
        verify(1, putRequestedFor(urlPathMatching(CHANGE_PRICE_PATH_PATTERN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.input.buyNowPrice.amount", equalTo(NEW_AMOUNT)))
                .withRequestBody(matchingJsonPath("$.input.buyNowPrice.currency", equalTo(CURRENCY_PLN))));
    }

    @Test
    void changeBuyNowPrice_whenPriceRejected_throwsBadRequestWithTypedFieldError(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlPathMatching(CHANGE_PRICE_PATH_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(BAD_REQUEST_BODY)));

        // then — the typed field error survives to the caller
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> offers(wmInfo).changeBuyNowPrice(OFFER_ID, Money.of(NEW_AMOUNT, CURRENCY_PLN)));
        assertEquals("input.buyNowPrice", failure.errors().get(0).path());
    }

    @Test
    void get_whenAuctionOffer_hasNoBuyNowPrice(WireMockRuntimeInfo wmInfo) {
        // given — an auction carries a starting price, not a Buy Now price
        stubFor(get(urlEqualTo(GET_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(
                        "{\"id\":\"" + OFFER_ID + "\",\"name\":\"Aukcja\","
                                + "\"category\":{\"id\":\"257\"},"
                                + "\"sellingMode\":{\"format\":\"AUCTION\"},"
                                + "\"publication\":{\"status\":\"ACTIVE\"}}")));

        // when
        Offer offer = offers(wmInfo).get(OFFER_ID);

        // then
        assertEquals(OfferFormat.AUCTION, offer.format());
        assertNull(offer.buyNowPrice());
        assertNull(offer.availableStock());
    }

    @Test
    void getFields_whenBothParts_requestsBothIncludesAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given — a parts response carrying stock, base price and a marketplace price
        stubFor(get(urlEqualTo(PARTS_BOTH_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PARTIAL_BOTH_BODY)));

        // when — request both the stock and the price parts
        PartialOffer partial = offers(wmInfo).getFields(OFFER_ID, OfferPart.STOCK, OfferPart.PRICE);

        // then — the include array is sent as one repeated parameter per part...
        verify(1, getRequestedFor(urlEqualTo(PARTS_BOTH_URL)));
        // ...and every requested part is mapped
        assertEquals(OFFER_ID, partial.id());
        assertEquals(PARTIAL_STOCK, partial.availableStock());
        assertEquals(PARTIAL_PRICE, partial.price().amount());
        assertEquals("3200.00", partial.marketplacePrices().get(MARKETPLACE_CZ).amount());
    }

    @Test
    void getFields_whenStockOnly_requestsOnlyStockInclude(WireMockRuntimeInfo wmInfo) {
        // given — a stock-only parts response
        stubFor(get(urlEqualTo(PARTS_STOCK_URL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PARTIAL_STOCK_BODY)));

        // when — request only the stock part
        PartialOffer partial = offers(wmInfo).getFields(OFFER_ID, OfferPart.STOCK);

        // then — only include=stock is sent, and only the stock is populated
        verify(1, getRequestedFor(urlEqualTo(PARTS_STOCK_URL)));
        assertEquals(PARTIAL_STOCK, partial.availableStock());
        assertNull(partial.price());
        assertNull(partial.marketplacePrices().get(MARKETPLACE_CZ));
    }

    @Test
    void getFields_whenNoParts_throwsWithoutCallingServer(WireMockRuntimeInfo wmInfo) {
        // when/then — at least one part is required, and no request is made
        assertThrows(IllegalArgumentException.class, () -> offers(wmInfo).getFields(OFFER_ID));
        verify(0, getRequestedFor(urlPathMatching(PARTS_PATH_PATTERN)));
    }

    @Test
    void getFields_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given — the parts read is rejected as not found
        stubFor(get(urlPathMatching(PARTS_PATH_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class,
                () -> offers(wmInfo).getFields(OFFER_ID, OfferPart.STOCK));
    }
}
