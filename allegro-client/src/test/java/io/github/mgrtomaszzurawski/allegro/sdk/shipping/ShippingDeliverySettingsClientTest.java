/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliverySettingsView;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.JoinStrategy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for {@code shipping().settings()} — the seller's
 * delivery-settings read and PUT-semantics update. Pins the auth header, path,
 * request body (join policy, free-delivery thresholds) and the response mapping.
 * The full transport error-path table (401 replay, 404, 429, 5xx) is exercised
 * once for the shipping domain by {@link ShippingPointsOfServiceClientTest}; this
 * class adds the write bad-request field-error path.
 *
 * <p>Fixture provenance: wire-verified 2026-07-18 (sandbox) — the
 * {@code delivery-settings} demo probe read the live settings and round-tripped
 * an idempotent update; see {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
@WireMockTest
class ShippingDeliverySettingsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String SETTINGS_PATH = "/sale/delivery-settings";
    private static final String SETTINGS_FIXTURE = "shipping/delivery-settings.json";

    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String FREE_AMOUNT = "200.00";
    private static final String ABROAD_AMOUNT = "500.00";
    private static final String CURRENCY = "PLN";
    private static final String STRATEGY_MAX = "MAX";
    private static final String STRATEGY_SUM = "SUM";
    private static final String UPDATED_AT = "2026-07-18T12:00:00Z";

    private static final String NEW_FREE_AMOUNT = "150.00";

    private static final String BAD_REQUEST_BODY = """
            {"errors":[{"code":"AmountInvalid","message":"amount is invalid",\
            "userMessage":"Podaj poprawną kwotę","path":"freeDelivery.amount"}]}
            """;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    @Test
    void get_whenSettingsExist_mapsEveryField(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(SETTINGS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SETTINGS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            DeliverySettingsView settings = allegro.shipping().settings().get();

            // then
            assertEquals(MARKETPLACE_ID, settings.marketplaceId());
            assertEquals(Money.of(FREE_AMOUNT, CURRENCY), settings.freeDelivery());
            assertEquals(Money.of(ABROAD_AMOUNT, CURRENCY), settings.abroadFreeDelivery());
            assertEquals(JoinStrategy.MAX, settings.joinPolicy());
            assertEquals(UPDATED_AT, settings.updatedAt());
        }
    }

    @Test
    void update_whenValidRequest_putsSettingsBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(put(urlEqualTo(SETTINGS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.marketplace.id", equalTo(MARKETPLACE_ID)))
                .withRequestBody(matchingJsonPath("$.joinPolicy.strategy", equalTo(STRATEGY_SUM)))
                .withRequestBody(matchingJsonPath("$.freeDelivery.amount", equalTo(NEW_FREE_AMOUNT)))
                .withRequestBody(matchingJsonPath("$.freeDelivery.currency", equalTo(CURRENCY)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SETTINGS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            DeliverySettingsView updated = allegro.shipping().settings().update(
                    DeliverySettingsRequest.builder()
                            .marketplaceId(MARKETPLACE_ID)
                            .freeDelivery(Money.of(NEW_FREE_AMOUNT, CURRENCY))
                            .joinPolicy(JoinStrategy.SUM)
                            .build());

            // then — response mapped, and the PUT body carried the settings
            assertEquals(JoinStrategy.MAX, updated.joinPolicy());
            verify(1, putRequestedFor(urlEqualTo(SETTINGS_PATH)));
        }
    }

    @Test
    void update_whenServerRejectsBody_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(put(urlEqualTo(SETTINGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(BAD_REQUEST_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            DeliverySettingsRequest request = DeliverySettingsRequest.builder()
                    .freeDelivery(Money.of("-1", CURRENCY))
                    .joinPolicy(JoinStrategy.MAX)
                    .build();
            var settings = allegro.shipping().settings();

            // then — the parsed field error names the offending path
            AllegroBadRequestException rejected = assertThrows(AllegroBadRequestException.class,
                    () -> settings.update(request));
            assertEquals("freeDelivery.amount", rejected.errors().get(0).path());
            verify(1, putRequestedFor(urlEqualTo(SETTINGS_PATH)));
        }
    }

    @Test
    void update_whenStrategyMax_sendsMaxStrategy(WireMockRuntimeInfo wmInfo) {
        // given — a second strategy value proves the enum maps to the wire verbatim
        stubToken();
        stubFor(put(urlEqualTo(SETTINGS_PATH))
                .withRequestBody(matchingJsonPath("$.joinPolicy.strategy", equalTo(STRATEGY_MAX)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SETTINGS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.shipping().settings().update(DeliverySettingsRequest.builder()
                    .joinPolicy(JoinStrategy.MAX).build());

            // then
            verify(1, putRequestedFor(urlEqualTo(SETTINGS_PATH)));
        }
    }
}
