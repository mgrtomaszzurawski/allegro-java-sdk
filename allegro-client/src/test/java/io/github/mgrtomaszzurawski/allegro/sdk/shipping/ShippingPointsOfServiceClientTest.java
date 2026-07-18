/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.builder.PointOfServiceRequestBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Address;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ConfirmationType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Coordinates;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.OpenHour;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PointOfServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PosType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the points-of-service facade (bucket C starter
 * slice): the create/get/delete happy paths (request-body pinning, full field
 * mapping incl. nested Address/Coordinates/OpenHour + enum fallback) and the
 * mandatory error-path table (400 typed field errors, 401 replay, 404, 429
 * exhaustion, 5xx retry vs POST-not-retried).
 *
 * <p>Response fixtures are {@code spec-derived}: not yet wire-verified. The
 * sandbox {@code pos-roundtrip} demo scenario confirms them before the bucket's
 * final PR (TESTING.md §2). The shared address / open-hours / coordinate values
 * used both to build the request and to assert the fixtures are hoisted into
 * {@code TEST_*}-style constants so the two never drift.
 */
@WireMockTest
class ShippingPointsOfServiceClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String POS_PATH = "/points-of-service";
    private static final String POS_ID = "8f3b1c2e-0000-4a00-9b00-111122223333";
    private static final String POS_BY_ID_PATH = POS_PATH + "/" + POS_ID;
    private static final String POS_NAME = "Pickup Point Center";
    private static final String POS_EXTERNAL_ID = "agent-c-demo-001";
    private static final String SELLER_ID = "111332841";

    // Address / open-hours / contact payload — shared by sampleRequest() and the
    // pos-get.json fixture assertions so the request and the fixture never drift.
    private static final String STREET = "Grunwaldzka 100";
    private static final String CITY = "Gdansk";
    private static final String ZIP_CODE = "80-244";
    private static final String STATE = "pomorskie";
    private static final String COUNTRY_CODE = "PL";
    private static final String PHONE_NUMBER = "+48111222333";
    private static final String EMAIL = "pickup@example.com";
    private static final String SERVICE_TIME = "PT24H";
    private static final String OPEN_DAY = "MONDAY";
    private static final String OPEN_DAY_SECOND = "TUESDAY";
    private static final String OPEN_FROM = "08:00";
    private static final String OPEN_TO = "16:00";
    private static final double LATITUDE = 54.372158;
    private static final double LONGITUDE = 18.638306;
    private static final String LOCATION_ID = "d5178eed-ccb6-473d-844b-d27764297d56";
    private static final String PAYMENT_CASH = "CASH";

    // Enum wire values pinned in the create request body.
    private static final String TYPE_PICKUP_POINT = "PICKUP_POINT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String CONFIRMATION_CONTACT_NOT_REQUIRED = "CONTACT_NOT_REQUIRED";

    private static final String CREATED_FIXTURE = "shipping/pos-created.json";
    private static final String GET_FIXTURE = "shipping/pos-get.json";
    private static final String LIST_FIXTURE = "shipping/pos-list.json";
    private static final String UPDATED_FIXTURE = "shipping/pos-updated.json";

    // Query parameters on GET /points-of-service (seller.id required, countryCode optional).
    private static final String PARAM_SELLER_ID = "seller.id";
    private static final String PARAM_COUNTRY_CODE = "countryCode";

    // Second point in pos-list.json — proves the list maps every item, not just the first.
    private static final int LIST_SIZE = 2;
    private static final String SECOND_POS_NAME = "Warehouse Desk";
    // New name PUT in the update round-trip, echoed back by pos-updated.json.
    private static final String UPDATED_NAME = "Pickup Point Center East";
    private static final String EMPTY_SEARCH_RESULT = "{\"posList\":[]}";

    // create/update/list resolve the seller id from GET /me (Allegro requires it
    // in the body/query even though the token identifies the seller).
    private static final String ME_PATH = "/me";
    private static final String ME_RESPONSE = "{\"id\":\"" + SELLER_ID + "\"}";

    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RETRY_5XX = "retry-5xx";
    private static final String STATE_RECOVERED = "recovered";
    private static final String RETRY_AFTER_SECONDS = "7";
    private static final int MAX_ATTEMPTS_FAST = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    // spec-derived: not yet wire-verified (errors[] contract shape).
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationException.Field.NotNull",
              "message":"name must not be null","userMessage":"Nazwa jest wymagana",
              "path":"name","details":null}]}
            """;
    // Unmodelled enum values must map to the UNKNOWN sentinel, never break parsing.
    private static final String UNKNOWN_ENUM_RESPONSE = """
            {"id":"%s","name":"future point","type":"PARCEL_LOCKER",
             "address":{"city":"Gdansk","zipCode":"80-244","state":"pomorskie","countryCode":"PL"},
             "openHours":[],"confirmationType":"CONTACT_NOT_REQUIRED","status":"SUSPENDED"}
            """.formatted(POS_ID);

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

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    /** Stub {@code GET /me} so the seller-id resolver returns {@link #SELLER_ID}. */
    private static void stubMe() {
        stubFor(get(urlEqualTo(ME_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ME_RESPONSE)));
    }

    private static PointOfServiceRequestBuilder sampleRequestBuilder() {
        return PointOfServiceRequest.builder()
                .name(POS_NAME)
                .type(PosType.PICKUP_POINT)
                .status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
                .address(Address.builder()
                        .street(STREET)
                        .city(CITY)
                        .zipCode(ZIP_CODE)
                        .state(STATE)
                        .countryCode(COUNTRY_CODE)
                        .coordinates(new Coordinates(LATITUDE, LONGITUDE))
                        .build())
                .openHours(List.of(OpenHour.builder()
                        .dayOfWeek(OPEN_DAY).fromTime(OPEN_FROM).toTime(OPEN_TO).build()))
                .externalId(POS_EXTERNAL_ID)
                .phoneNumber(PHONE_NUMBER)
                .email(EMAIL)
                .serviceTime(SERVICE_TIME);
    }

    private static PointOfServiceRequest sampleRequest() {
        return sampleRequestBuilder().build();
    }

    @Test
    void create_whenValidRequest_postsPosAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(post(urlEqualTo(POS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(POS_NAME)))
                .withRequestBody(matchingJsonPath("$.type", equalTo(TYPE_PICKUP_POINT)))
                .withRequestBody(matchingJsonPath("$.status", equalTo(STATUS_ACTIVE)))
                .withRequestBody(matchingJsonPath("$.confirmationType",
                        equalTo(CONFIRMATION_CONTACT_NOT_REQUIRED)))
                // the SDK injects the seller id (resolved from /me) into the body
                .withRequestBody(matchingJsonPath("$.seller.id", equalTo(SELLER_ID)))
                .withRequestBody(matchingJsonPath("$.address.city", equalTo(CITY)))
                .withRequestBody(matchingJsonPath("$.address.coordinates.lat"))
                .withRequestBody(matchingJsonPath("$.phoneNumber", equalTo(PHONE_NUMBER)))
                .withRequestBody(matchingJsonPath("$.openHours[0].dayOfWeek", equalTo(OPEN_DAY)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(CREATED_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService created = allegro.shipping().points().create(sampleRequest());

            // then
            assertEquals(POS_ID, created.id());
            assertEquals(POS_NAME, created.name());
            assertEquals(PosType.PICKUP_POINT, created.type());
            assertEquals(PosStatus.ACTIVE, created.status());
            assertEquals(POS_EXTERNAL_ID, created.externalId());
            assertEquals(CITY, created.address().city());
            verify(1, postRequestedFor(urlEqualTo(POS_PATH)));
        }
    }

    @Test
    void list_whenCalled_returnsAllPointsAndSendsResolvedSellerFilter(WireMockRuntimeInfo wmInfo) {
        // given — seller.id is a required query parameter, resolved from /me
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(get(urlPathEqualTo(POS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withQueryParam(PARAM_SELLER_ID, equalTo(SELLER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(LIST_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PointOfService> points = allegro.shipping().points().list();

            // then — every item in the wrapper maps, not just the first
            assertEquals(LIST_SIZE, points.size());
            assertEquals(POS_ID, points.get(0).id());
            assertEquals(SECOND_POS_NAME, points.get(1).name());
            assertEquals(PosStatus.TEMPORARILY_CLOSED, points.get(1).status());
            verify(1, getRequestedFor(urlPathEqualTo(POS_PATH))
                    .withQueryParam(PARAM_SELLER_ID, equalTo(SELLER_ID)));
        }
    }

    @Test
    void list_whenCountryCode_addsCountryFilterToRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(get(urlPathEqualTo(POS_PATH))
                .withQueryParam(PARAM_SELLER_ID, equalTo(SELLER_ID))
                .withQueryParam(PARAM_COUNTRY_CODE, equalTo(COUNTRY_CODE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(LIST_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PointOfService> points = allegro.shipping().points().list(COUNTRY_CODE);

            // then — both filters reached the wire
            assertFalse(points.isEmpty());
            verify(1, getRequestedFor(urlPathEqualTo(POS_PATH))
                    .withQueryParam(PARAM_SELLER_ID, equalTo(SELLER_ID))
                    .withQueryParam(PARAM_COUNTRY_CODE, equalTo(COUNTRY_CODE)));
        }
    }

    @Test
    void list_whenServerOmitsPosList_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given — the wrapper arrives with an empty posList
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(get(urlPathEqualTo(POS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(EMPTY_SEARCH_RESULT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PointOfService> points = allegro.shipping().points().list();

            // then — no NPE, a real empty list
            assertTrue(points.isEmpty());
        }
    }

    @Test
    void update_whenValidRequest_putsPosAndMapsUpdatedRecord(WireMockRuntimeInfo wmInfo) {
        // given — a full-representation PUT carrying the new name
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(put(urlEqualTo(POS_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(UPDATED_NAME)))
                // the PUT body carries the resolved seller id and the path id
                .withRequestBody(matchingJsonPath("$.seller.id", equalTo(SELLER_ID)))
                .withRequestBody(matchingJsonPath("$.id", equalTo(POS_ID)))
                .withRequestBody(matchingJsonPath("$.address.city", equalTo(CITY)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(UPDATED_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService updated = allegro.shipping().points()
                    .update(POS_ID, sampleRequestBuilder().name(UPDATED_NAME).build());

            // then — the PUT reached the wire and the response mapped
            assertEquals(POS_ID, updated.id());
            assertEquals(UPDATED_NAME, updated.name());
            verify(1, putRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void sellerIdResolver_resolvesMeOnceAndCachesAcrossOperations(WireMockRuntimeInfo wmInfo) {
        // given — create and list both need the seller id, resolved from /me
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(post(urlEqualTo(POS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(CREATED_FIXTURE)));
        stubFor(get(urlPathEqualTo(POS_PATH))
                .withQueryParam(PARAM_SELLER_ID, equalTo(SELLER_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(LIST_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — two operations via two fresh points() accessors on one client
            allegro.shipping().points().create(sampleRequest());
            allegro.shipping().points().list();

            // then — /me was called exactly once; the id is cached and reused
            verify(1, getRequestedFor(urlEqualTo(ME_PATH)));
        }
    }

    @Test
    void get_whenExists_mapsAllFieldsIncludingNestedAndEnums(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(GET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService point = allegro.shipping().points().get(POS_ID);

            // then — scalar + enum + nested + collection fields all survive
            assertEquals(POS_ID, point.id());
            assertEquals(SELLER_ID, point.sellerId());
            assertEquals(PosType.PICKUP_POINT, point.type());
            assertEquals(ConfirmationType.CONTACT_NOT_REQUIRED, point.confirmationType());
            assertEquals(PosStatus.ACTIVE, point.status());
            assertEquals(PHONE_NUMBER, point.phoneNumber());
            assertEquals(EMAIL, point.email());
            assertEquals(SERVICE_TIME, point.serviceTime());
            assertEquals(STREET, point.address().street());
            assertEquals(COUNTRY_CODE, point.address().countryCode());
            assertEquals(LATITUDE, point.address().coordinates().latitude());
            assertEquals(LONGITUDE, point.address().coordinates().longitude());
            assertEquals(2, point.openHours().size());
            assertEquals(OPEN_DAY_SECOND, point.openHours().get(1).dayOfWeek());
            assertEquals(List.of(LOCATION_ID), point.locationIds());
            assertEquals(List.of(PAYMENT_CASH), point.payments());
            verify(1, getRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void get_whenUnknownEnumValues_mapsToUnknownSentinel(WireMockRuntimeInfo wmInfo) {
        // given — the server introduces enum values this release does not model
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_ENUM_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService point = allegro.shipping().points().get(POS_ID);

            // then — deserialization does not break; unmapped values become UNKNOWN
            assertEquals(PosType.UNKNOWN, point.type());
            assertEquals(PosStatus.UNKNOWN, point.status());
            assertEquals(ConfirmationType.CONTACT_NOT_REQUIRED, point.confirmationType());
        }
    }

    @Test
    void create_whenBadRequest_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(post(urlEqualTo(POS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var points = allegro.shipping().points();
            PointOfServiceRequest request = sampleRequest();

            // then — typed field errors parsed from errors[]
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> points.create(request));
            assertEquals(TestHttpConstants.HTTP_BAD_REQUEST, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertFalse(failure.errors().isEmpty());
            assertEquals("ValidationException.Field.NotNull", failure.errors().get(0).code());
            assertEquals("name", failure.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(POS_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(GET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService point = allegro.shipping().points().get(POS_ID);

            // then — replayed exactly once, second request carried the fresh token
            assertEquals(POS_ID, point.id());
            verify(2, getRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
            verify(1, getRequestedFor(urlEqualTo(POS_BY_ID_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var points = allegro.shipping().points();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> points.get(POS_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
            verify(1, getRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void get_when429Exhausted_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — a GET is retryable; capped Retry-After keeps the test fast
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(POS_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS)));
        RetryPolicy fastRetry = RetryPolicy.builder()
                .maxAttempts(MAX_ATTEMPTS_FAST).maxRetryAfterSeconds(0).build();

        try (AllegroClient allegro = client(wmInfo, fastRetry)) {
            var points = allegro.shipping().points();

            // then — retried to exhaustion, then the typed rate-limit failure
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> points.get(POS_ID));
            assertEquals(Long.parseLong(RETRY_AFTER_SECONDS), failure.retryAfterSeconds());
            verify(MAX_ATTEMPTS_FAST, getRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void get_when500ThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(POS_BY_ID_PATH)).inScenario(SCENARIO_RETRY_5XX)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(POS_BY_ID_PATH)).inScenario(SCENARIO_RETRY_5XX)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(GET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PointOfService point = allegro.shipping().points().get(POS_ID);

            // then — the transient 5xx was retried and the second attempt won
            assertEquals(POS_ID, point.id());
            verify(2, getRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void create_when500_isNotRetried(WireMockRuntimeInfo wmInfo) {
        // given — POST is not idempotent; retryPost defaults to false
        stubToken(TEST_TOKEN);
        stubMe();
        stubFor(post(urlEqualTo(POS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var points = allegro.shipping().points();
            PointOfServiceRequest request = sampleRequest();

            // then — a single POST, surfaced as a server exception
            assertThrows(AllegroServerException.class, () -> points.create(request));
            verify(1, postRequestedFor(urlEqualTo(POS_PATH)));
        }
    }

    @Test
    void delete_whenExists_sendsAuthenticatedDelete(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(POS_BY_ID_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.shipping().points().delete(POS_ID);

            // then — the DELETE reached the wire with the bearer token
            verify(1, deleteRequestedFor(urlEqualTo(POS_BY_ID_PATH)));
        }
    }

    @Test
    void create_whenClientClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given
        AllegroClient allegro = client(wmInfo);
        allegro.close();

        // then — the accessor guards against use after close
        assertThrows(IllegalStateException.class, allegro::shipping);
    }
}
