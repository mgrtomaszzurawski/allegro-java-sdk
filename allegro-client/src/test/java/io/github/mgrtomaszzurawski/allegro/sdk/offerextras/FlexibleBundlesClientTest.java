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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.BundleCreatedBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundle;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleDiscountType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.FlexibleBundleSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the flexible-bundles facade, reached through
 * {@code offers().flexibleBundles()}. Pins the cursor-paged lazy summary stream
 * (whole-bundle discount mapped), the full-bundle read (slots + per-slot
 * discount), delete, fail-fast on nulls, and the error-path table on get.
 */
@WireMockTest
class FlexibleBundlesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String FLEX_PATH = "/sale/flexible-bundles";
    private static final String TEST_FLEX_ID = "11111111-1111-1111-1111-111111111111";
    private static final String FLEX_BUNDLE_PATH = FLEX_PATH + "/" + TEST_FLEX_ID;
    private static final String PAGE_ID_PARAM = "page.id";
    private static final String LIMIT_PARAM = "limit";
    private static final String PAGE_SIZE = "100";
    private static final String NEXT_CURSOR = "cursor-2";
    private static final String SUMMARY_ID = "flex-1";
    private static final String SECOND_SUMMARY_ID = "flex-2";
    private static final String OFFER_A = "offer-a";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final int WHOLE_PERCENTAGE = 10;
    private static final int SLOT_PERCENTAGE = 15;
    private static final int MIN_BOUGHT = 2;
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String EXPECTED_ERROR_CODE = "ConstraintViolationException";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_5XX_RECOVERY = "5xx-recovery";
    private static final String STATE_RECOVERED = "recovered";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long RETRY_AFTER_SECONDS = 1L;
    private static final int FAST_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified. A listing summary with a whole-bundle discount.
    private static final String SUMMARY = """
            {"id":"%s","createdBy":"USER","createdAt":"2026-07-01T10:15:30Z",
             "slotsRepresentatives":["%s","offer-b"],
             "discount":{"type":"WHOLE_BUNDLE_DISCOUNT",
               "bundle":{"minimumBoughtOffers":%d,
                 "discounts":[{"marketplaceId":"%s","percentage":%d}]},
               "slot":null}}
            """.formatted(SUMMARY_ID, OFFER_A, MIN_BOUGHT, MARKETPLACE_PL, WHOLE_PERCENTAGE);
    private static final String LISTING_PAGE_1 = """
            {"bundles":[%s],"nextPage":{"id":"%s"}}
            """.formatted(SUMMARY, NEXT_CURSOR);
    private static final String LISTING_PAGE_2 = """
            {"bundles":[%s],"nextPage":null}
            """.formatted(SUMMARY.replace(SUMMARY_ID, SECOND_SUMMARY_ID));
    // spec-derived: not yet wire-verified. A full bundle with one slot and a
    // per-slot discount (the get/slot-discount mapping is verified live before
    // the flexible-bundle write follow-up, once a seller token is restored).
    private static final String FULL_BUNDLE = """
            {"id":"%s","createdBy":"USER","createdAt":"2026-07-01T10:15:30Z",
             "slots":[{"id":"22222222-2222-2222-2222-222222222222","order":1,"entryPoint":true,
               "requiredQuantity":1,
               "offers":[{"id":"%s","excludedFromDiscount":false,"entryPoint":true}]}],
             "discount":{"type":"SLOT_DISCOUNT","bundle":null,
               "slot":{"slots":[{"order":1,
                 "discounts":[{"marketplaceId":"%s","percentage":%d}]}]}}}
            """.formatted(TEST_FLEX_ID, OFFER_A, MARKETPLACE_PL, SLOT_PERCENTAGE);
    // spec-derived: not yet wire-verified (errors[] contract shape).
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ConstraintViolationException","message":"invalid",
              "userMessage":"Nieprawidłowe","path":"slots"}]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape).
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
        stubFor(get(urlPathEqualTo(FLEX_PATH))
                .withQueryParam(PAGE_ID_PARAM, absent())
                .withQueryParam(LIMIT_PARAM, equalTo(PAGE_SIZE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(LISTING_PAGE_1)));
        stubFor(get(urlPathEqualTo(FLEX_PATH))
                .withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR))
                .withQueryParam(LIMIT_PARAM, equalTo(PAGE_SIZE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(LISTING_PAGE_2)));
    }

    @Test
    void streamBundles_whenTwoCursorPages_streamsAllAndMapsWholeBundleDiscount(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubTwoCursorPages();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<FlexibleBundleSummary> bundles = allegro.offers().flexibleBundles().streamBundles().toList();

            // then — both cursor pages streamed and the summary discount mapped
            assertEquals(2, bundles.size());
            FlexibleBundleSummary first = bundles.get(0);
            assertEquals(SUMMARY_ID, first.id());
            assertEquals(BundleCreatedBy.USER, first.createdBy());
            assertEquals(List.of(OFFER_A, "offer-b"), first.slotRepresentatives());
            assertEquals(FlexibleBundleDiscountType.WHOLE_BUNDLE_DISCOUNT, first.discount().type());
            assertEquals(MIN_BOUGHT, first.discount().wholeBundle().minimumBoughtOffers());
            assertEquals(WHOLE_PERCENTAGE,
                    first.discount().wholeBundle().marketplaceDiscounts().get(0).percentage());
            verify(1, getRequestedFor(urlPathEqualTo(FLEX_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR)));
        }
    }

    @Test
    void streamBundles_whenConsumerStopsAtFirstPage_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubTwoCursorPages();

        try (AllegroClient allegro = client(wmInfo)) {

            // when — take exactly the first page's single summary and stop
            long taken = allegro.offers().flexibleBundles().streamBundles().limit(1).count();

            // then — laziness: the second cursor page is never requested
            assertEquals(1, taken);
            verify(0, getRequestedFor(urlPathEqualTo(FLEX_PATH)).withQueryParam(PAGE_ID_PARAM, equalTo(NEXT_CURSOR)));
        }
    }

    @Test
    void get_whenBundleId_mapsSlotsAndSlotDiscount(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(FULL_BUNDLE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            FlexibleBundle bundle = allegro.offers().flexibleBundles().get(TEST_FLEX_ID);

            // then — the slot, its offer, and the per-slot discount are mapped
            assertEquals(TEST_FLEX_ID, bundle.id());
            assertEquals(1, bundle.slots().size());
            assertEquals(OFFER_A, bundle.slots().get(0).offers().get(0).offerId());
            assertFalse(bundle.slots().get(0).offers().get(0).excludedFromDiscount());
            assertEquals(FlexibleBundleDiscountType.SLOT_DISCOUNT, bundle.discount().type());
            assertEquals(SLOT_PERCENTAGE,
                    bundle.discount().slotDiscounts().get(0).marketplaceDiscounts().get(0).percentage());
            verify(1, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
        }
    }

    @Test
    void delete_whenBundleId_deletesBundlePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(delete(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().flexibleBundles().delete(TEST_FLEX_ID);

            // then
            verify(1, deleteRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
        }
    }

    @Test
    void flexibleBundleOps_whenBundleIdNull_throwBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().flexibleBundles();

            // then — the required bundle id is fail-fast before the wire
            assertThrows(NullPointerException.class, () -> bundles.get(null));
            assertThrows(NullPointerException.class, () -> bundles.delete(null));
            verify(0, anyRequestedFor(anyUrl()));
        }
    }

    @Test
    void get_when400WithErrors_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().flexibleBundles();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> bundles.get(TEST_FLEX_ID));
            assertEquals(EXPECTED_ERROR_CODE, failure.errors().get(0).code());
            verify(1, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
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
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(FULL_BUNDLE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            FlexibleBundle bundle = allegro.offers().flexibleBundles().get(TEST_FLEX_ID);

            // then — replayed once, the replay carried the fresh token
            assertEquals(TEST_FLEX_ID, bundle.id());
            verify(2, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var bundles = allegro.offers().flexibleBundles();

            // then
            assertThrows(AllegroNotFoundException.class, () -> bundles.get(TEST_FLEX_ID));
            verify(1, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
        }
    }

    @Test
    void get_when429WithRetryAfter_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {
            var bundles = allegro.offers().flexibleBundles();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> bundles.get(TEST_FLEX_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
        }
    }

    @Test
    void get_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first 500, retry returns 200
        stubToken();
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlPathEqualTo(FLEX_BUNDLE_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(FULL_BUNDLE)));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {

            // when
            FlexibleBundle bundle = allegro.offers().flexibleBundles().get(TEST_FLEX_ID);

            // then
            assertEquals(TEST_FLEX_ID, bundle.id());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(FLEX_BUNDLE_PATH)));
        }
    }
}
