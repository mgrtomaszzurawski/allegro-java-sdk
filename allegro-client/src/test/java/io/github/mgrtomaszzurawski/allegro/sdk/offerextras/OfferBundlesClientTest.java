/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleCreatedBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundlePublicationStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the offer-bundles facade, reached through
 * {@code offers().bundles()}. Pins the cursor-paged lazy stream, the single-bundle
 * mapping (offers/discounts/publication/createdBy), the discount PUT (body
 * verified, updated bundle returned), delete, fail-fast on nulls, and a
 * representative not-found error.
 */
@WireMockTest
class OfferBundlesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String BUNDLES_PATH = "/sale/bundles";
    private static final String TEST_BUNDLE_ID = "bundle-01";
    private static final String SECOND_BUNDLE_ID = "bundle-02";
    private static final String BUNDLE_PATH = BUNDLES_PATH + "/" + TEST_BUNDLE_ID;
    private static final String BUNDLE_DISCOUNT_PATH = BUNDLE_PATH + "/discount";
    private static final String PAGE_ID_PARAM = "page.id";
    private static final String NEXT_CURSOR = "cursor-2";
    private static final String TEST_OFFER_ID = "offer-1";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String DISCOUNT_AMOUNT = "10.00";
    private static final String NEW_DISCOUNT_AMOUNT = "15.00";
    private static final String CURRENCY_PLN = "PLN";
    private static final int REQUIRED_QUANTITY = 1;
    private static final long EXPIRY_SECONDS = 3600L;
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String EXPECTED_ERROR_CODE = "ConstraintViolationException";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_5XX_RECOVERY = "5xx-recovery";
    private static final String STATE_RECOVERED = "recovered";
    private static final long RETRY_AFTER_SECONDS = 1L;
    private static final int FAST_MAX_ATTEMPTS = 2;
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ConstraintViolationException","message":"invalid",
              "userMessage":"Nieprawidłowe","path":"discounts"}]}
            """;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified. One bundle, one bundled offer, one
    // per-marketplace discount and publication.
    private static final String BUNDLE = """
            {"id":"%s",
             "offers":[{"id":"%s","requiredQuantity":%d,"entryPoint":true}],
             "publication":[{"marketplace":{"id":"%s"},"status":"ACTIVE"}],
             "discounts":[{"marketplace":{"id":"%s"},"amount":"%s","currency":"%s"}],
             "createdAt":"2026-07-01T10:15:30Z","createdBy":"USER"}
            """.formatted(TEST_BUNDLE_ID, TEST_OFFER_ID, REQUIRED_QUANTITY, MARKETPLACE_PL,
            MARKETPLACE_PL, DISCOUNT_AMOUNT, CURRENCY_PLN);
    private static final String BUNDLE_PAGE_1 = """
            {"bundles":[%s],"nextPage":{"id":"%s"}}
            """.formatted(BUNDLE, NEXT_CURSOR);
    private static final String BUNDLE_PAGE_2 = """
            {"bundles":[%s],"nextPage":null}
            """.formatted(BUNDLE.replace(TEST_BUNDLE_ID, SECOND_BUNDLE_ID));
    private static final String UPDATED_BUNDLE = BUNDLE.replace(DISCOUNT_AMOUNT, NEW_DISCOUNT_AMOUNT);
    private static final String DISCOUNT_REQUEST_BODY = """
            {"discounts":[{"marketplace":{"id":"%s"},"amount":"%s","currency":"%s"}]}
            """.formatted(MARKETPLACE_PL, NEW_DISCOUNT_AMOUNT, CURRENCY_PLN);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"bundle not found","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.defaults());
    }

    private static AllegroClient client(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(retryPolicy)
                        .build());
    }

    private static RetryPolicy oneRetry() {
        return RetryPolicy.builder().maxAttempts(FAST_MAX_ATTEMPTS).build();
    }

    private static void stubToken() {
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    private static void stubTwoCursorPages() {
        stubFor(get(urlPathEqualTo(BUNDLES_PATH)).withQueryParam(PAGE_ID_PARAM, absent())
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BUNDLE_PAGE_1)));
        stubFor(get(urlPathEqualTo(BUNDLES_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BUNDLE_PAGE_2)));
    }

    @Test
    void streamBundles_whenTwoCursorPages_streamsAllAndMapsBundle(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubTwoCursorPages();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OfferBundle> bundles = allegro.offers().bundles().streamBundles().toList();

            // then — both cursor pages streamed and the first bundle fully mapped
            assertEquals(2, bundles.size());
            OfferBundle first = bundles.get(0);
            assertEquals(TEST_BUNDLE_ID, first.id());
            assertEquals(TEST_OFFER_ID, first.offers().get(0).offerId());
            assertTrue(first.offers().get(0).entryPoint());
            assertEquals(Money.of(DISCOUNT_AMOUNT, CURRENCY_PLN), first.discounts().get(0).amount());
            assertEquals(MARKETPLACE_PL, first.discounts().get(0).marketplaceId());
            assertEquals(BundlePublicationStatus.ACTIVE, first.publications().get(0).status());
            assertEquals(BundleCreatedBy.USER, first.createdBy());
            verify(1, getRequestedFor(urlPathEqualTo(BUNDLES_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR)));
        }
    }

    @Test
    void streamBundles_whenConsumerStopsAtFirstPage_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubTwoCursorPages();

        try (AllegroClient allegro = client(wmInfo)) {

            // when — take exactly the first page's single bundle and stop
            long taken = allegro.offers().bundles().streamBundles().limit(1).count();

            // then — laziness: the second cursor page is never requested
            assertEquals(1, taken);
            verify(0, getRequestedFor(urlPathEqualTo(BUNDLES_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR)));
        }
    }

    @Test
    void get_whenBundleId_mapsBundle(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BUNDLE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferBundle bundle = allegro.offers().bundles().get(TEST_BUNDLE_ID);

            // then
            assertEquals(TEST_BUNDLE_ID, bundle.id());
            assertEquals(REQUIRED_QUANTITY, bundle.offers().get(0).requiredQuantity());
            verify(1, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }

    @Test
    void get_whenCreatedByUnknownWireValue_degradesToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — a createdBy value this SDK version does not model (C3 forward-compat):
        // the Layer-1 enum degrades it to its sentinel and the domain maps it to UNKNOWN
        // rather than failing the read (before C3 this fixture failed deserialization)
        stubToken();
        String unknownCreatorBundle = BUNDLE.replace("\"createdBy\":\"USER\"", "\"createdBy\":\"SYSTEM\"");
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(unknownCreatorBundle)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferBundle bundle = allegro.offers().bundles().get(TEST_BUNDLE_ID);

            // then
            assertEquals(BundleCreatedBy.UNKNOWN, bundle.createdBy());
        }
    }

    @Test
    void updateDiscount_whenDiscountsGiven_putsBodyAndReturnsUpdatedBundle(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(put(urlPathEqualTo(BUNDLE_DISCOUNT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withRequestBody(equalToJson(DISCOUNT_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(UPDATED_BUNDLE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferBundle updated = allegro.offers().bundles().updateDiscount(TEST_BUNDLE_ID,
                    List.of(new BundleDiscount(MARKETPLACE_PL, Money.of(NEW_DISCOUNT_AMOUNT, CURRENCY_PLN))));

            // then — the new discount went out and the updated bundle came back
            assertEquals(Money.of(NEW_DISCOUNT_AMOUNT, CURRENCY_PLN), updated.discounts().get(0).amount());
            verify(1, putRequestedFor(urlPathEqualTo(BUNDLE_DISCOUNT_PATH)).withRequestBody(equalToJson(DISCOUNT_REQUEST_BODY)));
        }
    }

    @Test
    void delete_whenBundleId_deletesBundlePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(delete(urlPathEqualTo(BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().bundles().delete(TEST_BUNDLE_ID);

            // then
            verify(1, deleteRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }

    @Test
    void bundleOps_whenArgumentsNull_throwBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().bundles();
            List<BundleDiscount> discounts = List.of(
                    new BundleDiscount(MARKETPLACE_PL, Money.of(DISCOUNT_AMOUNT, CURRENCY_PLN)));

            // then — every required argument is fail-fast before the wire
            assertThrows(NullPointerException.class, () -> bundles.get(null));
            assertThrows(NullPointerException.class, () -> bundles.updateDiscount(null, discounts));
            assertThrows(NullPointerException.class, () -> bundles.updateDiscount(TEST_BUNDLE_ID, null));
            assertThrows(NullPointerException.class, () -> bundles.delete(null));
            verify(0, anyRequestedFor(anyUrl()));
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().bundles();

            // then
            assertThrows(AllegroNotFoundException.class, () -> bundles.get(TEST_BUNDLE_ID));
            verify(1, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }

    @Test
    void get_when400WithErrors_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().bundles();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> bundles.get(TEST_BUNDLE_ID));
            assertEquals(EXPECTED_ERROR_CODE, failure.errors().get(0).code());
            verify(1, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BUNDLE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferBundle bundle = allegro.offers().bundles().get(TEST_BUNDLE_ID);

            // then — replayed once, the replay carried the fresh token
            assertEquals(TEST_BUNDLE_ID, bundle.id());
            verify(2, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(BUNDLE_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when429WithRetryAfter_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {
            var bundles = allegro.offers().bundles();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> bundles.get(TEST_BUNDLE_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }

    @Test
    void get_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first 500, retry returns 200
        stubToken();
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlPathEqualTo(BUNDLE_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(BUNDLE)));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {

            // when
            OfferBundle bundle = allegro.offers().bundles().get(TEST_BUNDLE_ID);

            // then
            assertEquals(TEST_BUNDLE_ID, bundle.id());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(BUNDLE_PATH)));
        }
    }
}
