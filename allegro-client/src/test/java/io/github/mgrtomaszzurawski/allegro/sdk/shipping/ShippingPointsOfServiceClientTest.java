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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the points-of-service facade (bucket C starter
 * slice): the create/get happy paths (request-body pinning, full field mapping,
 * read-only enum fallback) and the mandatory error-path table (400 typed field
 * errors, 401 replay, 404, 429 exhaustion, 5xx retry vs POST-not-retried).
 *
 * <p>Response fixtures are {@code spec-derived}: not yet wire-verified. The
 * sandbox {@code pos-roundtrip} demo scenario confirms them before the bucket's
 * final PR (TESTING.md §2).
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

    private static final String CREATED_FIXTURE = "shipping/pos-created.json";
    private static final String GET_FIXTURE = "shipping/pos-get.json";

    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
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

    private static PointOfServiceRequest sampleRequest() {
        return PointOfServiceRequest.builder()
                .name(POS_NAME)
                .type(PosType.PICKUP_POINT)
                .status(PosStatus.ACTIVE)
                .confirmationType(ConfirmationType.CONTACT_NOT_REQUIRED)
                .address(Address.builder()
                        .street("Grunwaldzka 100")
                        .city("Gdansk")
                        .zipCode("80-244")
                        .state("pomorskie")
                        .countryCode("PL")
                        .coordinates(new Coordinates(54.372158, 18.638306))
                        .build())
                .openHours(List.of(OpenHour.builder()
                        .dayOfWeek("MONDAY").fromTime("08:00").toTime("16:00").build()))
                .externalId(POS_EXTERNAL_ID)
                .phoneNumber("+48111222333")
                .email("pickup@example.com")
                .serviceTime("PT24H")
                .build();
    }

    @Test
    void create_whenValidRequest_postsPosAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(POS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(POS_NAME)))
                .withRequestBody(matchingJsonPath("$.type", equalTo("PICKUP_POINT")))
                .withRequestBody(matchingJsonPath("$.status", equalTo("ACTIVE")))
                .withRequestBody(matchingJsonPath("$.confirmationType",
                        equalTo("CONTACT_NOT_REQUIRED")))
                .withRequestBody(matchingJsonPath("$.address.city", equalTo("Gdansk")))
                .withRequestBody(matchingJsonPath("$.address.coordinates.lat"))
                .withRequestBody(matchingJsonPath("$.phoneNumber", equalTo("+48111222333")))
                .withRequestBody(matchingJsonPath("$.openHours[0].dayOfWeek", equalTo("MONDAY")))
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
            assertEquals("Gdansk", created.address().city());
            verify(1, postRequestedFor(urlEqualTo(POS_PATH)));
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
            assertEquals("+48111222333", point.phoneNumber());
            assertEquals("pickup@example.com", point.email());
            assertEquals("PT24H", point.serviceTime());
            assertEquals("Grunwaldzka 100", point.address().street());
            assertEquals("PL", point.address().countryCode());
            assertEquals(54.372158, point.address().coordinates().latitude());
            assertEquals(18.638306, point.address().coordinates().longitude());
            assertEquals(2, point.openHours().size());
            assertEquals("TUESDAY", point.openHours().get(1).dayOfWeek());
            assertEquals(List.of("d5178eed-ccb6-473d-844b-d27764297d56"), point.locationIds());
            assertEquals(List.of("CASH"), point.payments());
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
        stubFor(get(urlEqualTo(POS_BY_ID_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(get(urlEqualTo(POS_BY_ID_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(STATE_REAUTHED)
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
        stubFor(post(urlEqualTo(POS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var points = allegro.shipping().points();
            PointOfServiceRequest request = sampleRequest();

            // then — a single POST, surfaced as a server exception (asserted via type)
            assertThrows(io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException.class,
                    () -> points.create(request));
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
