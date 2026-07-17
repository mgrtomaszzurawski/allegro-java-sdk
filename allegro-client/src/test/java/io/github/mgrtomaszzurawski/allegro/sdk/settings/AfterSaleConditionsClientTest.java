/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * End-to-end WireMock coverage of the after-sale warranties facade: request and
 * response mapping, lazy pagination, the vendor media type on the wire, and the
 * mandatory error-path table (400/401-replay/404/429/5xx).
 */
@WireMockTest
class AfterSaleConditionsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String WARRANTIES_PATH = "/after-sales-service-conditions/warranties";
    private static final String WARRANTY_ID = "09f0b4cc-7880-11e9-8f9e-2a86e4085a59";
    private static final String WARRANTY_PATH = WARRANTIES_PATH + "/" + WARRANTY_ID;
    private static final String ATTACHMENT_ID = "54702c96-4ccd-4c0e-b4c7-382a71e810b5";
    private static final String SELLER_ID = "111332841";

    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    private static final String OFFSET_PAGE_0 = "0";
    private static final String OFFSET_PAGE_1 = "100";
    private static final String OFFSET_PAGE_2 = "200";
    private static final String LIMIT_VALUE = "100";
    private static final int FULL_PAGE = 100;
    private static final int SECOND_PAGE_SIZE = 3;

    private static final String WARRANTY_ID_PREFIX = "w-";
    private static final String WARRANTY_NAME_PREFIX = "Warranty ";
    private static final String NAME = "2 year seller warranty";
    private static final String DESCRIPTION = "Covers manufacturing defects";
    private static final String INDIVIDUAL_PERIOD = "P24M";
    private static final String TRACE_ID = "4631702648f0524e";
    private static final long RETRY_AFTER_SECONDS = 1L;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified — confirmed by the settings-warranty
    // demo write->read before the bucket's final PR.
    private static final String WARRANTY_RESPONSE = """
            {"id":"%s","seller":{"id":"%s"},"name":"%s","type":"SELLER",
             "individual":{"period":"%s","lifetime":false},
             "corporate":{"lifetime":true},
             "attachment":{"id":"%s","name":"warranty.pdf",
               "url":"https://api.allegro.pl/after-sales-service-conditions/attachments/%s"},
             "description":"%s"}
            """.formatted(WARRANTY_ID, SELLER_ID, NAME, INDIVIDUAL_PERIOD, ATTACHMENT_ID,
            ATTACHMENT_ID, DESCRIPTION);
    // spec-derived: not yet wire-verified (errors[] shape on a 400).
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationException","message":"name is required",
              "userMessage":"Nazwa jest wymagana","path":"name"}]}
            """;
    private static final String BAD_REQUEST_CODE = "ValidationException";
    private static final String BAD_REQUEST_PATH = "name";

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.defaults());
    }

    private static AllegroClient clientFastRetry(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.builder().maxAttempts(2).build());
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

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static String warrantiesPage(int count) {
        StringBuilder warranties = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                warranties.append(',');
            }
            warranties.append("{\"id\":\"").append(WARRANTY_ID_PREFIX).append(index)
                    .append("\",\"name\":\"").append(WARRANTY_NAME_PREFIX).append(index)
                    .append("\"}");
        }
        return "{\"count\":" + count + ",\"warranties\":[" + warranties + "]}";
    }

    private static WarrantyRequest sellerWarranty() {
        return WarrantyRequest.builder()
                .name(NAME)
                .type(WarrantyType.SELLER)
                .individual(WarrantyPeriod.of(INDIVIDUAL_PERIOD))
                .description(DESCRIPTION)
                .build();
    }

    // ---- happy path: mapping ----

    @Test
    void warranty_whenFound_mapsFullDefinition(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(WARRANTY_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Warranty warranty = allegro.settings().afterSale().warranty(WARRANTY_ID);

            // then — every mapped field survives the Raw -> record round-trip
            assertEquals(WARRANTY_ID, warranty.id());
            assertEquals(SELLER_ID, warranty.sellerId());
            assertEquals(NAME, warranty.name());
            assertEquals(WarrantyType.SELLER, warranty.type());
            assertNotNull(warranty.individual());
            assertEquals(INDIVIDUAL_PERIOD, warranty.individual().period());
            assertFalse(warranty.individual().lifetime());
            assertNotNull(warranty.corporate());
            assertTrue(warranty.corporate().lifetime());
            assertNotNull(warranty.attachment());
            assertEquals(ATTACHMENT_ID, warranty.attachment().id());
            assertEquals(DESCRIPTION, warranty.description());
            verify(1, getRequestedFor(urlEqualTo(WARRANTY_PATH)));
        }
    }

    @Test
    void createWarranty_whenValidRequest_postsVendorBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(WARRANTIES_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Warranty created = allegro.settings().afterSale().createWarranty(sellerWarranty());

            // then — request body carried the mapped fields; response mapped back
            assertEquals(WARRANTY_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(WARRANTIES_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("SELLER")))
                    .withRequestBody(matchingJsonPath("$.individual.period", equalTo(INDIVIDUAL_PERIOD))));
        }
    }

    @Test
    void updateWarranty_whenValidRequest_putsVendorBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(WARRANTY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Warranty updated = allegro.settings().afterSale().updateWarranty(WARRANTY_ID, sellerWarranty());

            // then
            assertEquals(WARRANTY_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(WARRANTY_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME))));
        }
    }

    // ---- pagination ----

    @Test
    void streamWarranties_whenSinglePartialPage_mapsSummariesAndStops(WireMockRuntimeInfo wmInfo) {
        // given — a short page ends the walk (size < requested limit)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(2))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<WarrantySummary> warranties = allegro.settings().afterSale()
                    .streamWarranties().toList();

            // then
            assertEquals(2, warranties.size());
            assertEquals(WARRANTY_ID_PREFIX + "0", warranties.get(0).id());
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1)));
        }
    }

    @Test
    void streamWarranties_whenFirstPageFull_doesNotFetchSecondPageUntilNeeded(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies "there may be more"
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(SECOND_PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — consumer takes a single element only
            List<WarrantySummary> firstOnly = allegro.settings().afterSale()
                    .streamWarranties().limit(1).toList();

            // then — page two was never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1)));
        }
    }

    @Test
    void streamWarranties_whenTwoPages_fetchesBothWithAdvancingOffset(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(SECOND_PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.settings().afterSale().streamWarranties().count();

            // then — both pages walked, offset advanced, no phantom third page
            assertEquals(FULL_PAGE + SECOND_PAGE_SIZE, total);
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1)));
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_2)));
        }
    }

    // ---- error-path table ----

    @Test
    void createWarranty_whenBadRequest_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(WARRANTIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();
            WarrantyRequest request = sellerWarranty();

            // then — typed field errors survive; POST is not retried
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> afterSale.createWarranty(request));
            assertEquals(1, failure.errors().size());
            assertEquals(BAD_REQUEST_CODE, failure.errors().get(0).code());
            assertEquals(BAD_REQUEST_PATH, failure.errors().get(0).path());
            assertEquals(TRACE_ID, failure.traceId());
            verify(1, postRequestedFor(urlEqualTo(WARRANTIES_PATH)));
        }
    }

    @Test
    void warranty_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — first token, then a rotated token on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(WARRANTY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(WARRANTY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Warranty warranty = allegro.settings().afterSale().warranty(WARRANTY_ID);

            // then — replayed once, second attempt carried the fresh token
            assertEquals(WARRANTY_ID, warranty.id());
            verify(2, getRequestedFor(urlEqualTo(WARRANTY_PATH)));
            verify(1, getRequestedFor(urlEqualTo(WARRANTY_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void warranty_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(WARRANTY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> afterSale.warranty(WARRANTY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TRACE_ID, failure.traceId());
        }
    }

    @Test
    void warranty_whenRateLimited_retriesThenThrowsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — persistent 429 with a Retry-After the SDK must surface
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(WARRANTY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = clientFastRetry(wmInfo)) {
            var afterSale = allegro.settings().afterSale();

            // then — retried to the (fast) attempt cap, then typed with retryAfter
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> afterSale.warranty(WARRANTY_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(2, getRequestedFor(urlEqualTo(WARRANTY_PATH)));
        }
    }

    @Test
    void warranty_whenServerErrorThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — a GET is retried; the second attempt succeeds
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(WARRANTY_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(get(urlEqualTo(WARRANTY_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Warranty warranty = allegro.settings().afterSale().warranty(WARRANTY_ID);

            // then
            assertEquals(WARRANTY_ID, warranty.id());
            verify(2, getRequestedFor(urlEqualTo(WARRANTY_PATH)));
        }
    }

    @Test
    void createWarranty_whenServerError_isNotRetried(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(WARRANTIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();
            WarrantyRequest request = sellerWarranty();

            // then — POST writes are not retried by default
            assertThrows(AllegroServerException.class, () -> afterSale.createWarranty(request));
            verify(1, postRequestedFor(urlEqualTo(WARRANTIES_PATH)));
        }
    }
}
