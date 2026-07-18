/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.OfferTranslationType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the offer-translations facade, reached through
 * {@code offers().translations()}. Pins the title-translation read/write/delete,
 * the {@code PATCH} body, fail-fast on null inputs, and the mandatory error-path
 * table on {@code ofOffer} as the facade's representative endpoint.
 */
@WireMockTest
class OfferTranslationsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TEST_OFFER_ID = "8235476198";
    private static final String TEST_LANGUAGE = "en-US";
    private static final String TEST_TITLE = "Wireless keyboard";
    private static final String TRANSLATIONS_PATH = "/sale/offers/" + TEST_OFFER_ID + "/translations";
    private static final String TRANSLATION_PATH = TRANSLATIONS_PATH + "/" + TEST_LANGUAGE;
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
    // spec-derived: not yet wire-verified. A manual and an auto title translation.
    private static final String TRANSLATIONS_RESPONSE = """
            {"translations":[
              {"language":"%s","title":{"translation":"%s","type":"MANUAL"}},
              {"language":"de-DE","title":{"translation":"Kabellose Tastatur","type":"AUTO"}}
            ]}
            """.formatted(TEST_LANGUAGE, TEST_TITLE);
    // The shared SDK ObjectMapper serializes with the default ALWAYS inclusion,
    // so the unset description/safetyInformation go out as explicit nulls. This
    // pins that actual wire body; whether the server treats those nulls as
    // "no change" or "clear" is a KNOWN-SERVER-BEHAVIORS to-verify item.
    private static final String UPDATE_REQUEST_BODY = """
            {"description":null,"title":{"translation":"%s"},"safetyInformation":null}
            """.formatted(TEST_TITLE);
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ConstraintViolationException","message":"invalid",
              "userMessage":"Nieprawidłowe","path":"title"}]}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"offer not found","path":null}]}
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

    private static void stubToken(String accessToken) {
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    @Test
    void ofOffer_whenTranslationsReturned_mapsLanguageTitleAndType(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TRANSLATIONS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OfferTranslation> translations = allegro.offers().translations().ofOffer(TEST_OFFER_ID);

            // then — the title translation and its type map per language
            assertEquals(2, translations.size());
            assertEquals(TEST_LANGUAGE, translations.get(0).language());
            assertEquals(TEST_TITLE, translations.get(0).title());
            assertEquals(OfferTranslationType.MANUAL, translations.get(0).titleType());
            assertEquals(OfferTranslationType.AUTO, translations.get(1).titleType());
            verify(1, getRequestedFor(urlPathEqualTo(TRANSLATIONS_PATH)));
        }
    }

    @Test
    void update_whenRequestGiven_patchesTitleBodyToLanguagePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(patch(urlPathEqualTo(TRANSLATION_PATH))
                .withRequestBody(equalToJson(UPDATE_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().translations().update(TEST_OFFER_ID, TEST_LANGUAGE,
                    TranslationRequest.builder().title(TEST_TITLE).build());

            // then
            verify(1, patchRequestedFor(urlPathEqualTo(TRANSLATION_PATH))
                    .withRequestBody(equalToJson(UPDATE_REQUEST_BODY)));
        }
    }

    @Test
    void delete_whenLanguageGiven_deletesLanguagePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlPathEqualTo(TRANSLATION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().translations().delete(TEST_OFFER_ID, TEST_LANGUAGE);

            // then
            verify(1, deleteRequestedFor(urlPathEqualTo(TRANSLATION_PATH)));
        }
    }

    @Test
    void translationWrites_whenArgumentsNull_throwBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var translations = allegro.offers().translations();
            TranslationRequest request = TranslationRequest.builder().title(TEST_TITLE).build();

            // then — every required argument is fail-fast before the wire
            assertThrows(NullPointerException.class, () -> translations.ofOffer(null));
            assertThrows(NullPointerException.class, () -> translations.update(null, TEST_LANGUAGE, request));
            assertThrows(NullPointerException.class, () -> translations.update(TEST_OFFER_ID, null, request));
            assertThrows(NullPointerException.class, () -> translations.update(TEST_OFFER_ID, TEST_LANGUAGE, null));
            assertThrows(NullPointerException.class, () -> translations.delete(null, TEST_LANGUAGE));
            assertThrows(NullPointerException.class, () -> translations.delete(TEST_OFFER_ID, null));
            verify(0, anyRequestedFor(anyUrl()));
        }
    }

    @Test
    void ofOffer_when400WithErrors_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var translations = allegro.offers().translations();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> translations.ofOffer(TEST_OFFER_ID));
            assertEquals(1, failure.errors().size());
            assertEquals(EXPECTED_ERROR_CODE, failure.errors().get(0).code());
        }
    }

    @Test
    void ofOffer_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
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
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TRANSLATIONS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<OfferTranslation> translations = allegro.offers().translations().ofOffer(TEST_OFFER_ID);

            // then — replayed once, the replay carried the fresh token
            assertEquals(2, translations.size());
            verify(2, getRequestedFor(urlPathEqualTo(TRANSLATIONS_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(TRANSLATIONS_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void ofOffer_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var translations = allegro.offers().translations();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> translations.ofOffer(TEST_OFFER_ID));
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void ofOffer_when429WithRetryAfter_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {
            var translations = allegro.offers().translations();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> translations.ofOffer(TEST_OFFER_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(TRANSLATIONS_PATH)));
        }
    }

    @Test
    void ofOffer_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first 500, retry returns 200
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlPathEqualTo(TRANSLATIONS_PATH))
                .inScenario(SCENARIO_5XX_RECOVERY).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TRANSLATIONS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {

            // when
            List<OfferTranslation> translations = allegro.offers().translations().ofOffer(TEST_OFFER_ID);

            // then
            assertEquals(2, translations.size());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(TRANSLATIONS_PATH)));
        }
    }
}
