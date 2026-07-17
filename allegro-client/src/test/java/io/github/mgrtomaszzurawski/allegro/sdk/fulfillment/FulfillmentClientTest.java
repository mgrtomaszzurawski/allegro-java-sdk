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
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.WithdrawalAddressBuilder;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.PhoneNumber;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalOperation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.RemovalPreference;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.WithdrawalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the fulfillment facade's starter slice (removal
 * preferences): the write→read round-trip and the mandatory error-path table
 * (400 typed errors / 401 replay / 404 / 429 / 5xx), per TESTING.md.
 */
@WireMockTest
class FulfillmentClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String REMOVAL_PATH = "/fulfillment/removal/preferences";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RETRY = "retry-then-recover";
    private static final String STATE_RECOVERED = "recovered";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long RETRY_AFTER_SECONDS = 7L;
    private static final long RETRY_AFTER_IMMEDIATE = 0L;

    private static final String WIRE_WITHDRAWAL = RemovalOperation.WITHDRAWAL.wireValue();
    private static final String WIRE_DISPOSAL = RemovalOperation.DISPOSAL.wireValue();
    private static final String JSON_PATH_OPERATION = "$.operation";
    private static final String JSON_PATH_ADDRESS_COMPANY = "$.address.company";
    private static final String JSON_PATH_ADDRESS_PHONE_NUMBER = "$.address.phone.number";
    private static final String TEST_ERROR_CODE = "ValidationException";
    private static final String TEST_ERROR_PATH = "operation";

    private static final String TEST_COMPANY = "Warehouse Sp. z o.o.";
    private static final String TEST_STREET = "Uliczna 7";
    private static final String TEST_POSTAL_CODE = "60-166";
    private static final String TEST_CITY = "Poznan";
    private static final String TEST_COUNTRY_CODE = "PL";
    private static final String TEST_PHONE_COUNTRY = "48";
    private static final String TEST_PHONE_NUMBER = "123123123";
    private static final String TEST_ADDITIONAL_INFO = "Gate 3";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (One Fulfillment requires an enrolled
    // seller account; a sandbox capture during the bucket's exploration pass
    // confirms or corrects these shapes before the bucket's final PR).
    private static final String DISPOSAL_RESPONSE = """
            {"operation":"DISPOSAL"}
            """;
    private static final String WITHDRAWAL_RESPONSE = """
            {"operation":"WITHDRAWAL","address":{"company":"%s","street":"%s",
             "postalCode":"%s","city":"%s","countryCode":"%s",
             "phone":{"countryCode":"%s","number":"%s"},"additionalInfo":"%s"}}
            """.formatted(TEST_COMPANY, TEST_STREET, TEST_POSTAL_CODE, TEST_CITY,
            TEST_COUNTRY_CODE, TEST_PHONE_COUNTRY, TEST_PHONE_NUMBER, TEST_ADDITIONAL_INFO);
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"operation is required",
              "userMessage":"Niepoprawne dane","path":"%s"}]}
            """.formatted(TEST_ERROR_CODE, TEST_ERROR_PATH);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"Not found","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(retryPolicy)
                        .build());
    }

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.builder().enabled(false).build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static WithdrawalAddress sampleAddress() {
        return new WithdrawalAddressBuilder()
                .company(TEST_COMPANY)
                .street(TEST_STREET)
                .postalCode(TEST_POSTAL_CODE)
                .city(TEST_CITY)
                .countryCode(TEST_COUNTRY_CODE)
                .phone(PhoneNumber.of(TEST_PHONE_COUNTRY, TEST_PHONE_NUMBER))
                .additionalInfo(TEST_ADDITIONAL_INFO)
                .build();
    }

    @Test
    void removalPreference_whenDisposal_sendsVendorHeadersAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DISPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RemovalPreference preference = allegro.fulfillment().removalPreference();

            // then
            assertEquals(RemovalOperation.DISPOSAL, preference.operation());
            assertNull(preference.withdrawalAddress());
            verify(1, getRequestedFor(urlEqualTo(REMOVAL_PATH)));
        }
    }

    @Test
    void removalPreference_whenWithdrawal_mapsAddressAndPhone(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(WITHDRAWAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RemovalPreference preference = allegro.fulfillment().removalPreference();

            // then — nested address and phone survive the Raw→record mapping
            assertEquals(RemovalOperation.WITHDRAWAL, preference.operation());
            WithdrawalAddress address = preference.withdrawalAddress();
            assertEquals(TEST_COMPANY, address.company());
            assertEquals(TEST_CITY, address.city());
            assertEquals(TEST_PHONE_COUNTRY, address.phone().countryCode());
            assertEquals(TEST_PHONE_NUMBER, address.phone().number());
            assertEquals(TEST_ADDITIONAL_INFO, address.additionalInfo());
        }
    }

    @Test
    void setRemovalPreference_whenWithdrawal_putsVendorBodyAndReturnsStored(WireMockRuntimeInfo wmInfo) {
        // given — the spec documents 201 Created for this write
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(WITHDRAWAL_RESPONSE)));
        RemovalPreference request = RemovalPreference.builder()
                .operation(RemovalOperation.WITHDRAWAL)
                .withdrawalAddress(sampleAddress())
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RemovalPreference stored = allegro.fulfillment().setRemovalPreference(request);

            // then — the request carried the vendor content type and full body,
            // and the server's echo mapped back to the record
            assertEquals(RemovalOperation.WITHDRAWAL, stored.operation());
            assertEquals(TEST_COMPANY, stored.withdrawalAddress().company());
            verify(1, putRequestedFor(urlEqualTo(REMOVAL_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                    .withRequestBody(matchingJsonPath(JSON_PATH_OPERATION, equalTo(WIRE_WITHDRAWAL)))
                    .withRequestBody(matchingJsonPath(JSON_PATH_ADDRESS_COMPANY, equalTo(TEST_COMPANY)))
                    .withRequestBody(matchingJsonPath(JSON_PATH_ADDRESS_PHONE_NUMBER,
                            equalTo(TEST_PHONE_NUMBER))));
        }
    }

    @Test
    void setRemovalPreference_whenDisposal_putsOperationWithoutAddress(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(DISPOSAL_RESPONSE)));
        RemovalPreference request = RemovalPreference.builder()
                .operation(RemovalOperation.DISPOSAL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RemovalPreference stored = allegro.fulfillment().setRemovalPreference(request);

            // then
            assertEquals(RemovalOperation.DISPOSAL, stored.operation());
            assertNull(stored.withdrawalAddress());
            verify(1, putRequestedFor(urlEqualTo(REMOVAL_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PATH_OPERATION, equalTo(WIRE_DISPOSAL))));
        }
    }

    @Test
    void removalPreference_when401Once_reauthenticatesAndReplaysWithFreshToken(
            WireMockRuntimeInfo wmInfo) {
        // given — first GET 401; after re-auth 200. Token endpoint hands out
        // token-one first, token-two on the second acquisition.
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DISPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            RemovalPreference preference = allegro.fulfillment().removalPreference();

            // then — replayed once, second request carried the FRESH token
            assertEquals(RemovalOperation.DISPOSAL, preference.operation());
            verify(2, getRequestedFor(urlEqualTo(REMOVAL_PATH)));
            verify(1, getRequestedFor(urlEqualTo(REMOVAL_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void removalPreference_whenBadRequest_throwsBadRequestWithParsedErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var fulfillment = allegro.fulfillment();

            // then — the typed field errors are parsed from the errors[] payload
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, fulfillment::removalPreference);
            assertEquals(1, failure.errors().size());
            assertEquals(TEST_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(TEST_ERROR_PATH, failure.errors().get(0).path());
        }
    }

    @Test
    void removalPreference_whenNotFound_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var fulfillment = allegro.fulfillment();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, fulfillment::removalPreference);
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void removalPreference_whenRateLimited_throwsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — retry disabled, so the single 429 surfaces immediately with
        // the server's Retry-After preserved on the typed exception.
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo)) {
            var fulfillment = allegro.fulfillment();

            // then
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, fulfillment::removalPreference);
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
        }
    }

    @Test
    void removalPreference_whenRateLimitedThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — retry enabled: a 429 (Retry-After: 0) then 200; the SDK honours
        // Retry-After and replays the idempotent GET.
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .inScenario(SCENARIO_RETRY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER,
                                Long.toString(RETRY_AFTER_IMMEDIATE)))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .inScenario(SCENARIO_RETRY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DISPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo, RetryPolicy.builder().maxAttempts(2).build())) {
            // when
            RemovalPreference preference = allegro.fulfillment().removalPreference();

            // then — the retry fired and the second attempt returned the body
            assertEquals(RemovalOperation.DISPOSAL, preference.operation());
            verify(2, getRequestedFor(urlEqualTo(REMOVAL_PATH)));
        }
    }

    @Test
    void removalPreference_whenServerErrorThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — retry enabled: first 500, then 200 on the idempotent GET.
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .inScenario(SCENARIO_RETRY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .inScenario(SCENARIO_RETRY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DISPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo, RetryPolicy.builder().maxAttempts(2).build())) {
            // when
            RemovalPreference preference = allegro.fulfillment().removalPreference();

            // then — the retry happened and the second attempt returned the body
            assertEquals(RemovalOperation.DISPOSAL, preference.operation());
            verify(2, getRequestedFor(urlEqualTo(REMOVAL_PATH)));
        }
    }

    @Test
    void removalPreference_whenServerErrorPersists_throwsServerExceptionWithTraceId(
            WireMockRuntimeInfo wmInfo) {
        // given — retry disabled, so a persistent 5xx maps straight through.
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(REMOVAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var fulfillment = allegro.fulfillment();

            // then — status and the server trace id survive onto the exception
            AllegroServerException failure =
                    assertThrows(AllegroServerException.class, fulfillment::removalPreference);
            assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }
}
