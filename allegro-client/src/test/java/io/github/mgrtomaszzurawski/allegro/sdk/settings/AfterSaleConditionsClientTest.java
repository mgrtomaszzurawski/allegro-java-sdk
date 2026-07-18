/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ImpliedWarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.WarrantyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ImpliedWarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnCostCoveredBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyAvailability;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRestrictionCause;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.Warranty;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyPeriod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantySummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.WarrantyType;
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
    // The endpoint caps offset at 59, so a page-aligned next offset (60) is out
    // of range and must never be requested.
    private static final String OFFSET_OUT_OF_RANGE = "60";
    private static final String LIMIT_VALUE = "60";
    private static final int FULL_PAGE = 60;
    private static final int PARTIAL_PAGE = 2;

    private static final String WARRANTY_ID_PREFIX = "w-";
    private static final String WARRANTY_NAME_PREFIX = "Warranty ";
    private static final String NAME = "2 year seller warranty";
    private static final String DESCRIPTION = "Covers manufacturing defects";
    private static final String INDIVIDUAL_PERIOD = "P24M";
    private static final String CORPORATE_PERIOD = "P12M";
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

    private static final String IMPLIED_WARRANTIES_PATH =
            "/after-sales-service-conditions/implied-warranties";
    private static final String IMPLIED_WARRANTY_ID = "b953a7de-3817-4c89-896d-9ae71e56c0ff";
    private static final String IMPLIED_WARRANTY_PATH = IMPLIED_WARRANTIES_PATH + "/" + IMPLIED_WARRANTY_ID;
    private static final String IMPLIED_PERIOD = "P2Y";
    // Distinct from the individual period so the mapping test proves no field-swap.
    private static final String IMPLIED_CORPORATE_PERIOD = "P3Y";
    private static final String ADDRESS_CITY = "Poznań";
    // Live-verified 2026-07-18 (sandbox, seller TestBoxSDK) via the
    // settings-implied-warranty demo write->read (create->get round-trip green).
    private static final String IMPLIED_WARRANTY_RESPONSE = """
            {"id":"%s","seller":{"id":"%s"},"name":"%s",
             "individual":{"period":"%s"},"corporate":{"period":"%s"},
             "address":{"name":"Allegro sp. z o.o.","street":"Grunwaldzka 182",
               "postCode":"60-166","city":"%s","countryCode":"PL"},
             "description":"%s"}
            """.formatted(IMPLIED_WARRANTY_ID, SELLER_ID, NAME, IMPLIED_PERIOD,
                    IMPLIED_CORPORATE_PERIOD, ADDRESS_CITY, DESCRIPTION);
    // Only the always-present fields — pins the nullable-mapping branches
    // (absent seller / corporate / address) in ImpliedWarranty.from.
    private static final String IMPLIED_WARRANTY_MINIMAL_RESPONSE = """
            {"id":"%s","name":"%s","individual":{"period":"%s"}}
            """.formatted(IMPLIED_WARRANTY_ID, NAME, IMPLIED_PERIOD);

    private static final String RETURN_POLICIES_PATH = "/after-sales-service-conditions/return-policies";
    private static final String RETURN_POLICY_ID = "6a8e0f2c-1111-4c0e-b4c7-382a71e810b5";
    private static final String RETURN_POLICY_PATH = RETURN_POLICIES_PATH + "/" + RETURN_POLICY_ID;
    private static final String WITHDRAWAL_PERIOD = "P14D";
    private static final String RESTRICTION_NAME = "SEALED_MEDIA";
    private static final String SELLER_EMAIL = "seller@example.com";
    // Live-verified 2026-07-18 (sandbox, seller TestBoxSDK) via the
    // settings-return-policy demo create->read->update->delete round-trip (green).
    private static final String RETURN_POLICY_RESPONSE = """
            {"id":"%s","isFulfillment":false,"seller":{"id":"%s"},"name":"%s",
             "availability":{"range":"RESTRICTED","restrictionCause":{"name":"%s","description":"d"}},
             "withdrawalPeriod":"%s","returnCost":{"coveredBy":"SELLER"},
             "address":{"name":"Allegro sp. z o.o.","street":"Grunwaldzka 182",
               "postCode":"60-166","city":"%s","countryCode":"PL"},
             "contact":{"phoneNumber":"123 123 123","email":"%s"},
             "options":{"cashOnDeliveryNotAllowed":true,"freeAccessoriesReturnRequired":true,
               "refundLoweredByReceivedDiscount":false,"businessReturnAllowed":false,
               "collectBySellerOnly":true}}
            """.formatted(RETURN_POLICY_ID, SELLER_ID, NAME, RESTRICTION_NAME, WITHDRAWAL_PERIOD,
            ADDRESS_CITY, SELLER_EMAIL);
    // A DISABLED policy carries no withdrawalPeriod/returnCost/address/contact/
    // options — pins those nullable-mapping branches in ReturnPolicy.from.
    private static final String RETURN_POLICY_DISABLED_RESPONSE = """
            {"id":"%s","isFulfillment":true,"seller":{"id":"%s"},"name":"%s",
             "availability":{"range":"DISABLED","restrictionCause":{"name":"%s"}}}
            """.formatted(RETURN_POLICY_ID, SELLER_ID, NAME, RESTRICTION_NAME);

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
                .corporate(WarrantyPeriod.of(CORPORATE_PERIOD))
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
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
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
                    .withRequestBody(matchingJsonPath("$.individual.period", equalTo(INDIVIDUAL_PERIOD)))
                    .withRequestBody(matchingJsonPath("$.corporate.period", equalTo(CORPORATE_PERIOD))));
        }
    }

    @Test
    void updateWarranty_whenValidRequest_putsVendorBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(WARRANTY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
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
    void streamWarranties_whenPartialPage_mapsSummariesAndStops(WireMockRuntimeInfo wmInfo) {
        // given — a short page ends the walk (size < requested limit)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<WarrantySummary> warranties = allegro.settings().afterSale()
                    .streamWarranties().toList();

            // then — summaries map and the walk stops
            assertEquals(PARTIAL_PAGE, warranties.size());
            assertEquals(WARRANTY_ID_PREFIX + "0", warranties.get(0).id());
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_OUT_OF_RANGE)));
        }
    }

    @Test
    void streamWarranties_whenFullPage_stopsAtOffsetCeiling(WireMockRuntimeInfo wmInfo) {
        // given — a full page of 60; the endpoint's offset max is 59, so there is
        // no legal next page to request
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(FULL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.settings().afterSale().streamWarranties().count();

            // then — the full page is returned but no out-of-range offset is sent
            assertEquals(FULL_PAGE, total);
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_OUT_OF_RANGE)));
        }
    }

    @Test
    void streamWarranties_whenNotConsumed_defersTheFetch(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(warrantiesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — building the stream must not touch the wire
            var stream = allegro.settings().afterSale().streamWarranties();
            verify(0, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH)));

            // then — the first page is fetched only on terminal consumption
            stream.findFirst();
            verify(1, getRequestedFor(urlPathEqualTo(WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
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

    // ---- implied warranties (rękojmia) ----

    private static String impliedWarrantiesPage(int count) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                items.append(',');
            }
            items.append("{\"id\":\"").append(WARRANTY_ID_PREFIX).append(index)
                    .append("\",\"name\":\"").append(WARRANTY_NAME_PREFIX).append(index)
                    .append("\"}");
        }
        return "{\"count\":" + count + ",\"impliedWarranties\":[" + items + "]}";
    }

    private static ImpliedWarrantyRequest sampleImpliedWarranty() {
        return ImpliedWarrantyRequest.builder()
                .name(NAME)
                .individual(ImpliedWarrantyPeriod.of(IMPLIED_PERIOD))
                .description(DESCRIPTION)
                .build();
    }

    @Test
    void impliedWarranty_whenFound_mapsFullDefinition(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(IMPLIED_WARRANTY_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(IMPLIED_WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ImpliedWarranty implied = allegro.settings().afterSale().impliedWarranty(IMPLIED_WARRANTY_ID);

            // then — every mapped field survives the Raw -> record round-trip
            assertEquals(IMPLIED_WARRANTY_ID, implied.id());
            assertEquals(SELLER_ID, implied.sellerId());
            assertEquals(NAME, implied.name());
            assertNotNull(implied.individual());
            assertEquals(IMPLIED_PERIOD, implied.individual().period());
            assertNotNull(implied.corporate());
            assertEquals(IMPLIED_CORPORATE_PERIOD, implied.corporate().period());
            assertNotNull(implied.address());
            assertEquals(ADDRESS_CITY, implied.address().city());
            assertEquals(DESCRIPTION, implied.description());
            verify(1, getRequestedFor(urlEqualTo(IMPLIED_WARRANTY_PATH)));
        }
    }

    @Test
    void impliedWarranty_whenMinimalResponse_mapsAbsentFieldsAsNull(WireMockRuntimeInfo wmInfo) {
        // given — a response with only the always-present fields
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(IMPLIED_WARRANTY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(IMPLIED_WARRANTY_MINIMAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ImpliedWarranty implied = allegro.settings().afterSale().impliedWarranty(IMPLIED_WARRANTY_ID);

            // then — the optional fields map to null, individual still maps
            assertEquals(IMPLIED_WARRANTY_ID, implied.id());
            assertEquals(IMPLIED_PERIOD, implied.individual().period());
            assertNull(implied.sellerId());
            assertNull(implied.corporate());
            assertNull(implied.address());
            assertNull(implied.description());
        }
    }

    @Test
    void createImpliedWarranty_whenValidRequest_postsBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(IMPLIED_WARRANTIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(IMPLIED_WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ImpliedWarranty created = allegro.settings().afterSale()
                    .createImpliedWarranty(sampleImpliedWarranty());

            // then — request body carried the mapped fields; response mapped back
            assertEquals(IMPLIED_WARRANTY_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(IMPLIED_WARRANTIES_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.individual.period", equalTo(IMPLIED_PERIOD))));
        }
    }

    @Test
    void updateImpliedWarranty_whenValidRequest_putsBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(IMPLIED_WARRANTY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(IMPLIED_WARRANTY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ImpliedWarranty updated = allegro.settings().afterSale()
                    .updateImpliedWarranty(IMPLIED_WARRANTY_ID, sampleImpliedWarranty());

            // then
            assertEquals(IMPLIED_WARRANTY_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(IMPLIED_WARRANTY_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME))));
        }
    }

    @Test
    void streamImpliedWarranties_whenPartialPage_mapsSummariesAndStops(WireMockRuntimeInfo wmInfo) {
        // given — a short page ends the walk
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(IMPLIED_WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(impliedWarrantiesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<ImpliedWarrantySummary> implied = allegro.settings().afterSale()
                    .streamImpliedWarranties().toList();

            // then
            assertEquals(PARTIAL_PAGE, implied.size());
            assertEquals(WARRANTY_ID_PREFIX + "0", implied.get(0).id());
            assertEquals(WARRANTY_NAME_PREFIX + "0", implied.get(0).name());
            verify(1, getRequestedFor(urlPathEqualTo(IMPLIED_WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(IMPLIED_WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_OUT_OF_RANGE)));
        }
    }

    @Test
    void streamImpliedWarranties_whenNotConsumed_defersTheFetch(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(IMPLIED_WARRANTIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(impliedWarrantiesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — building the stream must not touch the wire
            var stream = allegro.settings().afterSale().streamImpliedWarranties();
            verify(0, getRequestedFor(urlPathEqualTo(IMPLIED_WARRANTIES_PATH)));

            // then — the first page is fetched only on terminal consumption
            stream.findFirst();
            verify(1, getRequestedFor(urlPathEqualTo(IMPLIED_WARRANTIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
        }
    }

    @Test
    void impliedWarranty_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(IMPLIED_WARRANTY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> afterSale.impliedWarranty(IMPLIED_WARRANTY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TRACE_ID, failure.traceId());
        }
    }

    @Test
    void createImpliedWarranty_whenBadRequest_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(IMPLIED_WARRANTIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();
            ImpliedWarrantyRequest request = sampleImpliedWarranty();

            // then — typed field errors survive; POST is not retried
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> afterSale.createImpliedWarranty(request));
            assertEquals(1, failure.errors().size());
            assertEquals(BAD_REQUEST_CODE, failure.errors().get(0).code());
            assertEquals(BAD_REQUEST_PATH, failure.errors().get(0).path());
            assertEquals(TRACE_ID, failure.traceId());
            verify(1, postRequestedFor(urlEqualTo(IMPLIED_WARRANTIES_PATH)));
        }
    }

    // ---- return policies ----

    private static String returnPoliciesPage(int count) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                items.append(',');
            }
            String suffix = "%02d".formatted(index);
            items.append("{\"id\":\"00000000-0000-0000-0000-0000000000").append(suffix)
                    .append("\",\"seller\":{\"id\":\"").append(SELLER_ID)
                    .append("\"},\"name\":\"").append(WARRANTY_NAME_PREFIX).append(index)
                    .append("\",\"availability\":{\"range\":\"FULL\"}}");
        }
        return "{\"count\":" + count + ",\"returnPolicies\":[" + items + "]}";
    }

    // The server requires options once availability is enabled (not DISABLED).
    private static final ReturnPolicyOptions SAMPLE_OPTIONS =
            new ReturnPolicyOptions(true, false, false, false, false);

    private static ReturnPolicyRequest sampleReturnPolicy() {
        return ReturnPolicyRequest.builder()
                .name(NAME)
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.restricted(ReturnRestrictionCause.SEALED_MEDIA))
                .withdrawalPeriod(WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.SELLER)
                .options(SAMPLE_OPTIONS)
                .build();
    }

    private static ReturnPolicyUpdateRequest sampleReturnPolicyUpdate() {
        return ReturnPolicyUpdateRequest.builder()
                .name(NAME)
                .availability(ReturnPolicyAvailability.full())
                .options(SAMPLE_OPTIONS)
                .build();
    }

    @Test
    void returnPolicy_whenFound_mapsFullDefinition(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RETURN_POLICY_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RETURN_POLICY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ReturnPolicy policy = allegro.settings().afterSale().returnPolicy(RETURN_POLICY_ID);

            // then — every nested field survives the Raw -> record round-trip
            assertEquals(RETURN_POLICY_ID, policy.id());
            assertFalse(policy.fulfillment());
            assertEquals(SELLER_ID, policy.sellerId());
            assertEquals(NAME, policy.name());
            assertEquals(ReturnRange.RESTRICTED, policy.availability().range());
            assertEquals(ReturnRestrictionCause.SEALED_MEDIA, policy.availability().restrictionCause());
            assertEquals(WITHDRAWAL_PERIOD, policy.withdrawalPeriod());
            assertEquals(ReturnCostCoveredBy.SELLER, policy.returnCost());
            assertNotNull(policy.address());
            assertEquals(ADDRESS_CITY, policy.address().city());
            assertNotNull(policy.contact());
            assertEquals(SELLER_EMAIL, policy.contact().email());
            assertNotNull(policy.options());
            assertTrue(policy.options().cashOnDeliveryNotAllowed());
            verify(1, getRequestedFor(urlEqualTo(RETURN_POLICY_PATH)));
        }
    }

    @Test
    void returnPolicy_whenDisabledRange_mapsAbsentFieldsAsNull(WireMockRuntimeInfo wmInfo) {
        // given — a DISABLED policy omits withdrawalPeriod/returnCost/address/contact/options
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RETURN_POLICY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RETURN_POLICY_DISABLED_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ReturnPolicy policy = allegro.settings().afterSale().returnPolicy(RETURN_POLICY_ID);

            // then
            assertTrue(policy.fulfillment());
            assertEquals(ReturnRange.DISABLED, policy.availability().range());
            assertEquals(SELLER_ID, policy.sellerId());
            assertNull(policy.withdrawalPeriod());
            assertNull(policy.returnCost());
            assertNull(policy.address());
            assertNull(policy.contact());
            assertNull(policy.options());
        }
    }

    @Test
    void createReturnPolicy_whenValidRequest_postsBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RETURN_POLICIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(RETURN_POLICY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ReturnPolicy created = allegro.settings().afterSale().createReturnPolicy(sampleReturnPolicy());

            // then — request body carried the mapped fields; response mapped back
            assertEquals(RETURN_POLICY_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(RETURN_POLICIES_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.isFulfillment", equalTo("false")))
                    .withRequestBody(matchingJsonPath("$.availability.range", equalTo("RESTRICTED")))
                    .withRequestBody(matchingJsonPath("$.options.cashOnDeliveryNotAllowed", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.availability.restrictionCause.name",
                            equalTo(RESTRICTION_NAME)))
                    .withRequestBody(matchingJsonPath("$.returnCost.coveredBy", equalTo("SELLER"))));
        }
    }

    @Test
    void updateReturnPolicy_whenValidRequest_putsBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(RETURN_POLICY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RETURN_POLICY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ReturnPolicy updated = allegro.settings().afterSale()
                    .updateReturnPolicy(RETURN_POLICY_ID, sampleReturnPolicyUpdate());

            // then — the update body carries no isFulfillment (fixed at creation)
            assertEquals(RETURN_POLICY_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(RETURN_POLICY_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.availability.range", equalTo("FULL")))
                    .withRequestBody(notMatching("(?s).*isFulfillment.*")));
        }
    }

    @Test
    void deleteReturnPolicy_whenCalled_sendsDelete(WireMockRuntimeInfo wmInfo) {
        // given — the server returns the deleted policy; the SDK discards the body
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(RETURN_POLICY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RETURN_POLICY_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.settings().afterSale().deleteReturnPolicy(RETURN_POLICY_ID);

            // then
            verify(1, deleteRequestedFor(urlEqualTo(RETURN_POLICY_PATH)));
        }
    }

    @Test
    void streamReturnPolicies_whenPartialPage_mapsPoliciesAndStops(WireMockRuntimeInfo wmInfo) {
        // given — a short page ends the walk
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(RETURN_POLICIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnPoliciesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<ReturnPolicy> policies = allegro.settings().afterSale()
                    .streamReturnPolicies().toList();

            // then — full policies map and the walk stops
            assertEquals(PARTIAL_PAGE, policies.size());
            assertEquals(ReturnRange.FULL, policies.get(0).availability().range());
            verify(1, getRequestedFor(urlPathEqualTo(RETURN_POLICIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
            verify(0, getRequestedFor(urlPathEqualTo(RETURN_POLICIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_OUT_OF_RANGE)));
        }
    }

    @Test
    void streamReturnPolicies_whenNotConsumed_defersTheFetch(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(RETURN_POLICIES_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(returnPoliciesPage(PARTIAL_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — building the stream must not touch the wire
            var stream = allegro.settings().afterSale().streamReturnPolicies();
            verify(0, getRequestedFor(urlPathEqualTo(RETURN_POLICIES_PATH)));

            // then — the first page is fetched only on terminal consumption
            stream.findFirst();
            verify(1, getRequestedFor(urlPathEqualTo(RETURN_POLICIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
        }
    }

    @Test
    void returnPolicy_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RETURN_POLICY_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> afterSale.returnPolicy(RETURN_POLICY_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
        }
    }

    @Test
    void createReturnPolicy_whenBadRequest_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RETURN_POLICIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var afterSale = allegro.settings().afterSale();
            ReturnPolicyRequest request = sampleReturnPolicy();

            // then — typed field errors survive; POST is not retried
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> afterSale.createReturnPolicy(request));
            assertEquals(BAD_REQUEST_PATH, failure.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(RETURN_POLICIES_PATH)));
        }
    }
}
