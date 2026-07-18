/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for {@link Promotions}: lazy offset/limit pagination (proving
 * a page is fetched only when consumed and that the required {@code promotionType}
 * filter survives page boundaries), the polymorphic benefit mapping across all
 * three families plus the forward-compat degrade of an unknown benefit type
 * (C4), the create/modify request bodies, deactivate, and the mandatory
 * 400/401-replay/404/429/5xx error-path table.
 */
@WireMockTest
class PromotionsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String PROMOTIONS_PATH = "/sale/loyalty/promotions";
    private static final String TEST_PROMOTION_ID = "promo-1";
    private static final String PROMOTION_PATH = PROMOTIONS_PATH + "/" + TEST_PROMOTION_ID;
    private static final String TEST_OFFER_ID = "12345";

    private static final String PROMOTION_TYPE_PARAM = "promotionType";
    private static final String OFFER_ID_PARAM = "offer.id";
    private static final String OFFSET_PARAM = "offset";
    private static final String LARGE_ORDER_TYPE = "LARGE_ORDER_DISCOUNT";
    private static final String MULTIPACK_TYPE = "UNIT_PERCENTAGE_DISCOUNT";
    private static final String WHOLESALE_TYPE = "WHOLESALE_PRICE_LIST";
    private static final String CONTAINS_OFFERS_TYPE = "CONTAINS_OFFERS";
    private static final String ALL_OFFERS_TYPE = "ALL_OFFERS";
    private static final String ASSIGNED_EXTERNALLY_TYPE = "OFFERS_ASSIGNED_EXTERNALLY";
    private static final String FIRST_PAGE_OFFSET = "0";
    private static final String SECOND_PAGE_OFFSET = "100";

    private static final String TEST_AMOUNT = "100.00";
    private static final String TEST_CURRENCY = "PLN";
    private static final String LARGE_ORDER_PERCENTAGE = "10";
    private static final String MULTIPACK_PERCENTAGE = "50";
    private static final String MULTIPACK_BUY_QUANTITY = "3";
    private static final String MULTIPACK_DISCOUNTED_QUANTITY = "1";
    private static final String WHOLESALE_PERCENTAGE = "15";
    private static final String WHOLESALE_QUANTITY = "10";
    private static final String WHOLESALE_NAME = "Wholesale tiers";
    private static final String ERR_UNSERIALIZABLE_BENEFIT_PREFIX = "cannot serialize a benefit";
    private static final String ERR_UNSERIALIZABLE_CRITERION = "cannot serialize an unknown offer-criterion type";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long TEST_RETRY_AFTER = 1L;
    private static final String ERROR_CODE_VALIDATION = "ValidationError";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Not found","path":null}]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String VALIDATION_ERROR_RESPONSE = """
            {"errors":[{"code":"ValidationError","message":"Invalid benefit",
              "userMessage":"Invalid benefit","path":"benefits","details":null,"metadata":null}]}
            """;
    // spec-derived: not yet wire-verified (all three benefit families + an offer criterion)
    private static final String RICH_PROMOTION_RESPONSE = """
            {"id":"promo-1","status":"ACTIVE","createdAt":"2026-07-17T10:15:30Z",
             "benefits":[
               {"specification":{"type":"LARGE_ORDER_DISCOUNT","thresholds":[
                 {"orderValue":{"lowerBound":{"amount":"100.00","currency":"PLN"}},
                  "discount":{"percentage":"10"}}]}},
               {"specification":{"type":"UNIT_PERCENTAGE_DISCOUNT",
                 "configuration":{"percentage":50},"trigger":{"forEachQuantity":3,"discountedNumber":1}}},
               {"specification":{"type":"WHOLESALE_PRICE_LIST","name":"Wholesale tiers","thresholds":[
                 {"quantity":{"lowerBound":10},"discount":{"percentage":"15"}}]}}],
             "offerCriteria":[{"type":"CONTAINS_OFFERS","offers":[{"id":"12345"}]}]}
            """;
    // spec-derived: not yet wire-verified (a benefit family this SDK version does not model)
    private static final String UNKNOWN_BENEFIT_RESPONSE = """
            {"id":"promo-2","status":"SUSPENDED",
             "benefits":[{"specification":{"type":"FUTURE_FAMILY","payload":{"x":1}}}],
             "offerCriteria":[{"type":"ALL_OFFERS"}]}
            """;
    // spec-derived: not yet wire-verified (a status and criterion type this SDK version does not model)
    private static final String UNKNOWN_ENUMS_RESPONSE = """
            {"id":"promo-3","status":"FUTURE_STATUS",
             "benefits":[{"specification":{"type":"LARGE_ORDER_DISCOUNT","thresholds":[
               {"orderValue":{"lowerBound":{"amount":"100.00","currency":"PLN"}},
                "discount":{"percentage":"10"}}]}}],
             "offerCriteria":[{"type":"FUTURE_TYPE"}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(2)
                                .backoffStrategy(RetryPolicy.BackoffStrategy.FIXED)
                                .build())
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static String minimalPromotion(String id) {
        return "{\"id\":\"" + id + "\",\"status\":\"ACTIVE\",\"benefits\":[],\"offerCriteria\":[]}";
    }

    private static String promotionsPage(String promotionsJson, long totalCount) {
        return "{\"promotions\":[" + promotionsJson + "],\"totalCount\":" + totalCount + "}";
    }

    private static PromotionRequest largeOrderRequest() {
        return PromotionRequest.builder()
                .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                        new Benefit.OrderValueThreshold(
                                Money.of(TEST_AMOUNT, TEST_CURRENCY), LARGE_ORDER_PERCENTAGE))))
                .addOfferCriterion(OfferCriterion.containing(List.of(TEST_OFFER_ID)))
                .build();
    }

    private static PromotionRequest allFamiliesRequest() {
        return PromotionRequest.builder()
                .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                        new Benefit.OrderValueThreshold(
                                Money.of(TEST_AMOUNT, TEST_CURRENCY), LARGE_ORDER_PERCENTAGE))))
                .addBenefit(new Benefit.MultiPackDiscount(MULTIPACK_PERCENTAGE,
                        new BigDecimal(MULTIPACK_BUY_QUANTITY), new BigDecimal(MULTIPACK_DISCOUNTED_QUANTITY)))
                .addBenefit(new Benefit.WholesalePriceList(WHOLESALE_NAME, List.of(
                        new Benefit.QuantityThreshold(
                                new BigDecimal(WHOLESALE_QUANTITY), WHOLESALE_PERCENTAGE))))
                .addOfferCriterion(OfferCriterion.containing(List.of(TEST_OFFER_ID)))
                .addOfferCriterion(OfferCriterion.allOffers())
                .addOfferCriterion(OfferCriterion.assignedExternally())
                .build();
    }

    @Test
    void streamPromotions_whenConsumerShortCircuits_fetchesOnlyFirstPage(WireMockRuntimeInfo wmInfo) {
        // given — the first page has more to follow (totalCount > returned)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(FIRST_PAGE_OFFSET))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(promotionsPage(minimalPromotion("promo-0"), 2))));
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(SECOND_PAGE_OFFSET))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(promotionsPage(minimalPromotion("promo-1"), 2))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — only the first element is consumed
            Promotion first = allegro.pricing().promotions()
                    .streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT)
                    .findFirst().orElseThrow();

            // then — the second page was never requested (laziness)
            assertEquals("promo-0", first.id());
            verify(1, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(FIRST_PAGE_OFFSET)));
            verify(0, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(SECOND_PAGE_OFFSET)));
        }
    }

    @Test
    void streamPromotions_whenMultiplePages_walksAllAndKeepsTypeFilter(WireMockRuntimeInfo wmInfo) {
        // given — two pages, each carrying the required promotionType filter
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(FIRST_PAGE_OFFSET))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(promotionsPage(minimalPromotion("promo-0"), 2))));
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(SECOND_PAGE_OFFSET))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(promotionsPage(minimalPromotion("promo-1"), 2))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — the whole result set is materialised
            List<String> promotionIds = allegro.pricing().promotions()
                    .streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT)
                    .map(Promotion::id).toList();

            // then — both pages walked, and both requests carried the type filter
            assertEquals(List.of("promo-0", "promo-1"), promotionIds);
            verify(1, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(FIRST_PAGE_OFFSET))
                    .withQueryParam(PROMOTION_TYPE_PARAM, equalTo(LARGE_ORDER_TYPE)));
            verify(1, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(SECOND_PAGE_OFFSET))
                    .withQueryParam(PROMOTION_TYPE_PARAM, equalTo(LARGE_ORDER_TYPE)));
        }
    }

    @Test
    void streamPromotions_withOfferId_sendsOfferIdFilter(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(promotionsPage(minimalPromotion("promo-0"), 1))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long count = allegro.pricing().promotions()
                    .streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT, TEST_OFFER_ID)
                    .count();

            // then — the offer.id filter is on the request
            assertEquals(1, count);
            verify(1, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH))
                    .withQueryParam(OFFER_ID_PARAM, equalTo(TEST_OFFER_ID))
                    .withQueryParam(PROMOTION_TYPE_PARAM, equalTo(LARGE_ORDER_TYPE)));
        }
    }

    @Test
    void get_whenAllBenefitFamilies_mapsEachToItsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RICH_PROMOTION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion promotion = allegro.pricing().promotions().get(TEST_PROMOTION_ID);

            // then
            assertEquals(TEST_PROMOTION_ID, promotion.id());
            assertEquals(Promotion.Status.ACTIVE, promotion.status());
            assertEquals(3, promotion.benefits().size());

            Benefit.LargeOrderDiscount large = assertInstanceOf(
                    Benefit.LargeOrderDiscount.class, promotion.benefits().get(0));
            assertEquals(Money.of(TEST_AMOUNT, TEST_CURRENCY),
                    large.thresholds().get(0).orderValueFrom());
            assertEquals(LARGE_ORDER_PERCENTAGE, large.thresholds().get(0).discountPercentage());

            Benefit.MultiPackDiscount multi = assertInstanceOf(
                    Benefit.MultiPackDiscount.class, promotion.benefits().get(1));
            assertEquals(MULTIPACK_PERCENTAGE, multi.discountPercentage());
            assertEquals(new BigDecimal(3), multi.buyQuantity());
            assertEquals(new BigDecimal(1), multi.discountedQuantity());

            Benefit.WholesalePriceList wholesale = assertInstanceOf(
                    Benefit.WholesalePriceList.class, promotion.benefits().get(2));
            assertEquals(WHOLESALE_NAME, wholesale.name());
            assertEquals(new BigDecimal(10), wholesale.thresholds().get(0).quantityFrom());
            assertEquals(WHOLESALE_PERCENTAGE, wholesale.thresholds().get(0).discountPercentage());

            OfferCriterion criterion = promotion.offerCriteria().get(0);
            assertEquals(OfferCriterion.Type.CONTAINS_OFFERS, criterion.type());
            assertEquals(List.of(TEST_OFFER_ID), criterion.offerIds());
        }
    }

    @Test
    void get_whenUnknownBenefitType_degradesToUnknownBenefit(WireMockRuntimeInfo wmInfo) {
        // given — a benefit family this SDK version does not model (forward-compat, C4)
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_BENEFIT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion promotion = allegro.pricing().promotions().get(TEST_PROMOTION_ID);

            // then — the unknown subtype degrades to the sentinel instead of failing
            assertEquals(Promotion.Status.SUSPENDED, promotion.status());
            Benefit.UnknownBenefit unknown = assertInstanceOf(
                    Benefit.UnknownBenefit.class, promotion.benefits().get(0));
            assertEquals("FUTURE_FAMILY", unknown.type());
            assertEquals(OfferCriterion.Type.ALL_OFFERS, promotion.offerCriteria().get(0).type());
            assertTrue(promotion.offerCriteria().get(0).offerIds().isEmpty());
        }
    }

    @Test
    void get_whenUnknownStatusAndCriterionType_degradeToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — a status and offer-criterion type this SDK version does not model
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_ENUMS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion promotion = allegro.pricing().promotions().get(TEST_PROMOTION_ID);

            // then — the unmodelled enum values degrade instead of failing the read
            assertEquals(Promotion.Status.UNKNOWN, promotion.status());
            assertEquals(OfferCriterion.Type.UNKNOWN, promotion.offerCriteria().get(0).type());
        }
    }

    @Test
    void create_whenAllBenefitFamilies_serializesEachDiscriminatedSpecification(WireMockRuntimeInfo wmInfo) {
        // given — one benefit of each family and all three writable criterion types
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(PROMOTIONS_PATH))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[0].specification.type", equalTo(LARGE_ORDER_TYPE)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[1].specification.type", equalTo(MULTIPACK_TYPE)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[1].specification.configuration.percentage", equalTo(MULTIPACK_PERCENTAGE)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[1].specification.trigger.forEachQuantity", equalTo(MULTIPACK_BUY_QUANTITY)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[1].specification.trigger.discountedNumber",
                        equalTo(MULTIPACK_DISCOUNTED_QUANTITY)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[2].specification.type", equalTo(WHOLESALE_TYPE)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[2].specification.name", equalTo(WHOLESALE_NAME)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[2].specification.thresholds[0].quantity.lowerBound",
                        equalTo(WHOLESALE_QUANTITY)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[2].specification.thresholds[0].discount.percentage",
                        equalTo(WHOLESALE_PERCENTAGE)))
                .withRequestBody(matchingJsonPath("$.offerCriteria[0].type", equalTo(CONTAINS_OFFERS_TYPE)))
                .withRequestBody(matchingJsonPath("$.offerCriteria[1].type", equalTo(ALL_OFFERS_TYPE)))
                .withRequestBody(matchingJsonPath("$.offerCriteria[2].type", equalTo(ASSIGNED_EXTERNALLY_TYPE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(RICH_PROMOTION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion created = allegro.pricing().promotions().create(allFamiliesRequest());

            // then
            assertEquals(TEST_PROMOTION_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void create_whenUnknownBenefit_throwsBeforeSending(WireMockRuntimeInfo wmInfo) {
        // given — a read-only sentinel benefit that cannot be serialized
        PromotionRequest request = PromotionRequest.builder()
                .addBenefit(new Benefit.UnknownBenefit("FUTURE_FAMILY"))
                .addOfferCriterion(OfferCriterion.allOffers())
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();

            // then — rejected client-side, no request is sent
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class, () -> promotions.create(request));
            assertTrue(failure.getMessage().startsWith(ERR_UNSERIALIZABLE_BENEFIT_PREFIX));
            verify(0, postRequestedFor(urlEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void create_whenUnknownCriterionType_throwsBeforeSending(WireMockRuntimeInfo wmInfo) {
        // given — a read-only sentinel criterion type that cannot be serialized
        PromotionRequest request = PromotionRequest.builder()
                .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                        new Benefit.OrderValueThreshold(
                                Money.of(TEST_AMOUNT, TEST_CURRENCY), LARGE_ORDER_PERCENTAGE))))
                .addOfferCriterion(new OfferCriterion(OfferCriterion.Type.UNKNOWN, List.of()))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();

            // then
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class, () -> promotions.create(request));
            assertEquals(ERR_UNSERIALIZABLE_CRITERION, failure.getMessage());
            verify(0, postRequestedFor(urlEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void create_whenValidRequest_postsDiscriminatedBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(PROMOTIONS_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[0].specification.type", equalTo(LARGE_ORDER_TYPE)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[0].specification.thresholds[0].orderValue.lowerBound.amount",
                        equalTo(TEST_AMOUNT)))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[0].specification.thresholds[0].discount.percentage",
                        equalTo(LARGE_ORDER_PERCENTAGE)))
                .withRequestBody(matchingJsonPath(
                        "$.offerCriteria[0].type", equalTo("CONTAINS_OFFERS")))
                .withRequestBody(matchingJsonPath(
                        "$.offerCriteria[0].offers[0].id", equalTo(TEST_OFFER_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(RICH_PROMOTION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion created = allegro.pricing().promotions().create(largeOrderRequest());

            // then
            assertEquals(TEST_PROMOTION_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void modify_whenValidRequest_putsToPromotionPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(PROMOTION_PATH))
                .withRequestBody(matchingJsonPath(
                        "$.benefits[0].specification.type", equalTo(LARGE_ORDER_TYPE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RICH_PROMOTION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion modified = allegro.pricing().promotions()
                    .modify(TEST_PROMOTION_ID, largeOrderRequest());

            // then
            assertEquals(TEST_PROMOTION_ID, modified.id());
            verify(1, putRequestedFor(urlEqualTo(PROMOTION_PATH)));
        }
    }

    @Test
    void deactivate_sendsDeleteToPromotionPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(PROMOTION_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.pricing().promotions().deactivate(TEST_PROMOTION_ID);

            // then
            verify(1, deleteRequestedFor(urlEqualTo(PROMOTION_PATH)));
        }
    }

    @Test
    void create_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(PROMOTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(VALIDATION_ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();
            PromotionRequest request = largeOrderRequest();

            // then
            AllegroBadRequestException failure = assertThrows(
                    AllegroBadRequestException.class, () -> promotions.create(request));
            assertEquals(1, failure.errors().size());
            assertEquals(ERROR_CODE_VALIDATION, failure.errors().get(0).code());
            assertEquals("benefits", failure.errors().get(0).path());
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();

            // then
            AllegroNotFoundException failure = assertThrows(
                    AllegroNotFoundException.class, () -> promotions.get(TEST_PROMOTION_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
        }
    }

    @Test
    void streamPromotions_when429_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — every attempt is throttled; the policy allows one retry
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(TEST_RETRY_AFTER))));

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();

            // then — evaluating the lazy stream retries once, then surfaces Retry-After
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> promotions.streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT).count());
            assertEquals(TEST_RETRY_AFTER, failure.retryAfterSeconds());
            verify(2, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void streamPromotions_when5xx_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given — a GET is retried once, then the server error surfaces
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(PROMOTIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            Promotions promotions = allegro.pricing().promotions();

            // then
            assertThrows(AllegroServerException.class,
                    () -> promotions.streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT).count());
            verify(2, getRequestedFor(urlPathEqualTo(PROMOTIONS_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(PROMOTION_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RICH_PROMOTION_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Promotion promotion = allegro.pricing().promotions().get(TEST_PROMOTION_ID);

            // then — re-authenticated and replayed with the fresh token
            assertEquals(TEST_PROMOTION_ID, promotion.id());
            verify(2, getRequestedFor(urlEqualTo(PROMOTION_PATH)));
        }
    }
}
