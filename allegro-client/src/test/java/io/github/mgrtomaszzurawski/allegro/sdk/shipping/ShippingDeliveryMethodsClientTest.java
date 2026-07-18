/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryMethod;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PaymentPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for {@code shipping().deliveryMethods()} — the
 * root-facade read that lists the delivery methods Allegro offers the seller.
 * The full transport error-path table (401 replay, 404, 429, 5xx) is exercised
 * once for the shipping domain by {@link ShippingPointsOfServiceClientTest};
 * this class pins the delivery-method mapping plus a representative retryable
 * server error on the root-facade path.
 *
 * <p>The response field shape is wire-verified: the {@code delivery-methods}
 * demo probe read 571 live methods from the sandbox (2026-07-18, app token) and
 * confirmed the mapped fields — see {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
@WireMockTest
class ShippingDeliveryMethodsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String DELIVERY_METHODS_PATH = "/sale/delivery-methods";
    private static final String METHODS_FIXTURE = "shipping/delivery-methods.json";
    private static final String EMPTY_METHODS = "{\"deliveryMethods\":[]}";

    private static final int METHODS_SIZE = 3;
    private static final String COURIER_ID = "8f9a6c2e-1111-4a00-9b00-aaaabbbbcccc";
    private static final String COURIER_NAME = "Kurier";
    private static final String COURIER_MARKETPLACE = "allegro-pl";
    private static final String DISPATCH_COUNTRY = "PL";

    private static final int MAX_ATTEMPTS_FAST = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
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

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    @Test
    void deliveryMethods_whenMethodsExist_mapsEveryFieldOfEveryMethod(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(METHODS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<DeliveryMethod> methods = allegro.shipping().deliveryMethods();

            // then — both methods map, including the enum, flag and country fields
            assertEquals(METHODS_SIZE, methods.size());
            DeliveryMethod courier = methods.get(0);
            assertEquals(COURIER_ID, courier.id());
            assertEquals(COURIER_NAME, courier.name());
            assertEquals(PaymentPolicy.IN_ADVANCE, courier.paymentPolicy());
            assertTrue(courier.allegroEndorsed());
            assertEquals(DISPATCH_COUNTRY, courier.dispatchCountry());
            assertEquals(List.of(COURIER_MARKETPLACE), courier.marketplaces());
            assertEquals(PaymentPolicy.CASH_ON_DELIVERY, methods.get(1).paymentPolicy());
            assertFalse(methods.get(1).allegroEndorsed());
            // third method omits the optional fields — null policy, empty markets, flag off
            DeliveryMethod minimal = methods.get(2);
            assertNull(minimal.paymentPolicy());
            assertTrue(minimal.marketplaces().isEmpty());
            assertFalse(minimal.allegroEndorsed());
            verify(1, getRequestedFor(urlEqualTo(DELIVERY_METHODS_PATH)));
        }
    }

    @Test
    void deliveryMethods_whenServerOmitsMethods_returnsEmptyList(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(EMPTY_METHODS)));

        try (AllegroClient allegro = client(wmInfo)) {

            // then — no NPE, a real empty list
            assertTrue(allegro.shipping().deliveryMethods().isEmpty());
        }
    }

    @Test
    void deliveryMethods_whenServerErrorPersists_retriesThenThrowsServerException(
            WireMockRuntimeInfo wmInfo) {
        // given — a GET is retryable; the root facade uses the same retry pipeline
        stubToken();
        stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RetryPolicy fastRetry = RetryPolicy.builder()
                .maxAttempts(MAX_ATTEMPTS_FAST).maxRetryAfterSeconds(0).build();

        try (AllegroClient allegro = client(wmInfo, fastRetry)) {
            var shipping = allegro.shipping();

            // then — retried to exhaustion, then the typed server failure
            assertThrows(AllegroServerException.class, shipping::deliveryMethods);
            verify(MAX_ATTEMPTS_FAST, getRequestedFor(urlEqualTo(DELIVERY_METHODS_PATH)));
        }
    }
}
