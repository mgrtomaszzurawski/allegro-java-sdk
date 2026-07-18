/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.billing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.builder.BillingFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.billing.model.BillingType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the billing facade: the billing-type dictionary
 * (with the mandatory error-path table) and lazy billing-entry streaming
 * (full-page-termination pagination, filter params on the wire).
 */
@WireMockTest
class BillingClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RETRY = "retry-5xx";
    private static final String STATE_RECOVERED = "recovered";

    private static final String TYPES_PATH = "/billing/billing-types";
    private static final String ENTRIES_PATH = "/billing/billing-entries";
    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_TYPE_ID = "type.id";
    private static final String TYPE_ID = "SALE_COMMISSION";
    private static final String TYPE_DESCRIPTION = "Sale commission";

    private static final int PAGE_SIZE = 100;
    private static final int SHORT_PAGE = 2;
    private static final String OFFSET_PAGE_TWO = String.valueOf(PAGE_SIZE);

    private static final long RETRY_AFTER_SECONDS = 30L;
    private static final int FAST_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Wire-verified: the billing-types shape was confirmed live on the sandbox via
    // the `billing-types` demo (234 types read, mapped cleanly, 2026-07-18).
    private static final String TYPES_BODY =
            "[{\"id\":\"" + TYPE_ID + "\",\"description\":\"" + TYPE_DESCRIPTION + "\"}]";
    // spec-derived: not yet wire-verified.
    private static final String ERRORS_BODY = """
            {"errors":[{"code":"InvalidQuery","message":"bad","path":"type.id"}]}
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

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    // spec-derived: not yet wire-verified. The billing-entries shape awaits the §2
    // sandbox pass (blocked on the seller token / a seeded ledger).
    private static String entryJson(int index) {
        String entryUuid = String.format("00000000-0000-0000-0000-%012d", index);
        return "{\"id\":\"" + entryUuid + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"type\":{\"id\":\"" + TYPE_ID + "\",\"name\":\"" + TYPE_DESCRIPTION + "\"},"
                + "\"value\":{\"amount\":\"-1.50\",\"currency\":\"PLN\"},"
                + "\"balance\":{\"amount\":\"98.50\",\"currency\":\"PLN\"}}";
    }

    private static String entriesPage(int count, int startIndex) {
        StringBuilder entries = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                entries.append(',');
            }
            entries.append(entryJson(startIndex + index));
        }
        return "{\"billingEntries\":[" + entries + "]}";
    }

    @Test
    void types_whenCalled_mapsDictionary(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TYPES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BillingType> types = allegro.billing().types();

            // then
            assertEquals(1, types.size());
            assertEquals(TYPE_ID, types.get(0).id());
            assertEquals(TYPE_DESCRIPTION, types.get(0).description());
        }
    }

    @Test
    void types_when400WithErrors_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TYPES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var billing = allegro.billing();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, billing::types);
            assertEquals(TEST_TRACE_ID, failure.traceId());
            assertEquals("type.id", failure.errors().get(0).path());
        }
    }

    @Test
    void types_when401Once_reauthenticatesAndReplays(WireMockRuntimeInfo wmInfo) {
        // given — first token rejected, replay with a fresh token succeeds
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(TYPES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TYPES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BillingType> types = allegro.billing().types();

            // then — replayed once, the replay carried the fresh token
            assertEquals(1, types.size());
            verify(1, getRequestedFor(urlEqualTo(TYPES_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void types_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TYPES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(ERRORS_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            var billing = allegro.billing();

            // then
            assertThrows(AllegroNotFoundException.class, billing::types);
        }
    }

    @Test
    void types_when429Exhausted_throwsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TYPES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_SECONDS))
                        .withBody(ERRORS_BODY)));
        RetryPolicy fast = RetryPolicy.builder()
                .maxAttempts(FAST_MAX_ATTEMPTS).maxRetryAfterSeconds(0L).build();

        try (AllegroClient allegro = client(wmInfo, fast)) {
            var billing = allegro.billing();

            // then
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, billing::types);
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(TYPES_PATH)));
        }
    }

    @Test
    void types_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(TYPES_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(TYPES_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TYPES_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BillingType> types = allegro.billing().types();

            // then
            assertEquals(1, types.size());
            verify(2, getRequestedFor(urlEqualTo(TYPES_PATH)));
        }
    }

    @Test
    void streamEntries_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(PAGE_SIZE, 0))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<BillingEntry> firstOnly = allegro.billing().streamEntries(BillingFilter.all())
                    .limit(1).toList();

            // then — page two never requested
            assertEquals(1, firstOnly.size());
            verify(0, getRequestedFor(urlPathEqualTo(ENTRIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamEntries_whenShortPage_terminates(WireMockRuntimeInfo wmInfo) {
        // given — full page then a short page ends the walk
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(PAGE_SIZE, 0))));
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(SHORT_PAGE, PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.billing().streamEntries(BillingFilter.all()).count();

            // then
            assertEquals(PAGE_SIZE + (long) SHORT_PAGE, total);
            verify(1, getRequestedFor(urlPathEqualTo(ENTRIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO)));
        }
    }

    @Test
    void streamEntries_whenFilterGiven_sendsQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_TYPE_ID, equalTo(TYPE_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(SHORT_PAGE, 0))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.billing().streamEntries(BillingFilter.builder().typeId(TYPE_ID).build())
                    .toList();

            // then
            verify(getRequestedFor(urlPathEqualTo(ENTRIES_PATH))
                    .withQueryParam(PARAM_TYPE_ID, equalTo(TYPE_ID)));
        }
    }

    @Test
    void streamEntries_whenFilterGiven_carriesFilterAcrossPageBoundary(WireMockRuntimeInfo wmInfo) {
        // given — both pages require the filter param; a page-2 request that dropped
        // it would miss the stub and 404, failing the walk
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_OFFSET, equalTo("0"))
                .withQueryParam(PARAM_TYPE_ID, equalTo(TYPE_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(PAGE_SIZE, 0))));
        stubFor(get(urlPathEqualTo(ENTRIES_PATH)).withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                .withQueryParam(PARAM_TYPE_ID, equalTo(TYPE_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(entriesPage(SHORT_PAGE, PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            long total = allegro.billing().streamEntries(
                    BillingFilter.builder().typeId(TYPE_ID).build()).count();

            // then — page two carried the filter param (else it would not match)
            assertEquals(PAGE_SIZE + (long) SHORT_PAGE, total);
            verify(1, getRequestedFor(urlPathEqualTo(ENTRIES_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_TWO))
                    .withQueryParam(PARAM_TYPE_ID, equalTo(TYPE_ID)));
        }
    }

    @Test
    void billing_whenClientClosed_throwsIllegalState(WireMockRuntimeInfo wmInfo) {
        // given
        AllegroClient allegro = client(wmInfo);
        allegro.close();

        // then
        assertThrows(IllegalStateException.class, allegro::billing);
    }
}
