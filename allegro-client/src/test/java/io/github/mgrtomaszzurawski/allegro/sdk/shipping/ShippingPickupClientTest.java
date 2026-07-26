/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Pickup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupProposals;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupProposalsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PickupTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the pickup operations on {@code shipping()} — the
 * pickup-proposals read, the asynchronous {@code requestPickup} command (submit →
 * poll → resolve → read the booked pickup), and the pickup read. Every stub pins
 * the {@code Authorization: Bearer} header and the method+path; writes pin the
 * request body. The async flow is proven by the poll count, the command-failure
 * path asserts the parsed {@code errors[]}, and the id guard is proven to fail
 * before any call.
 *
 * <p>Fixture provenance: {@code spec-derived} — the pickup write path needs a real
 * seeded order and shipment (WZA broker) to wire-verify; see
 * {@code KNOWN-SERVER-BEHAVIORS.md} and the bucket-C PR-2c note in the plan.
 */
@WireMockTest
class ShippingPickupClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String PROPOSALS_PATH = "/shipment-management/pickup-proposals";
    private static final String CREATE_PATH = "/shipment-management/pickups/create-commands";
    private static final String CREATE_POLL_PATH = CREATE_PATH + "/CMD-PICKUP-1";
    private static final String PICKUP_PATH = "/shipment-management/pickups/PICKUP-1001";
    private static final String PICKUP_PATH_PATTERN = "/shipment-management/pickups/.*";

    private static final String PICKUP_ID = "PICKUP-1001";
    private static final String SHIPMENT_ID = "SHIP-1001";
    private static final String CARRIER = "DPD";
    private static final String CARRIER_PICKUP_ID = "CARR-9";
    private static final String WAYBILL = "WB-555";
    private static final String SELLER_CITY = "Gdansk";
    private static final String SELLER_STREET = "Grunwaldzka 100";
    private static final String SELLER_POSTAL = "80-244";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String SELLER_PHONE = "+48500100100";
    private static final String PICKUP_DATE = "2026-07-28";
    private static final String MIN_TIME = "08:00";
    private static final String MAX_TIME = "16:00";
    private static final String ERROR_PATH = "pickupTime";

    private static final String POLL_SCENARIO = "poll-pickup";
    private static final String STATE_DONE = "done";
    private static final int EXPECTED_WINDOW_COUNT = 2;
    private static final int MIN_POLLS = 2;

    private static final String PROPOSALS_FIXTURE = "shipping/pickup-proposals.json";
    private static final String CREATE_ACCEPTED = "shipping/pickup-create-accepted.json";
    private static final String CREATE_IN_PROGRESS = "shipping/pickup-create-status-inprogress.json";
    private static final String CREATE_SUCCESS = "shipping/pickup-create-status-success.json";
    private static final String CREATE_ERROR = "shipping/pickup-create-status-error.json";
    private static final String PICKUP_FIXTURE = "shipping/pickup-get.json";

    private static final Duration TINY_TIMEOUT = Duration.ofMillis(200);

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    @Test
    void pickupProposals_whenRequested_mapsWindows(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(authed(post(urlEqualTo(PROPOSALS_PATH)))
                .withRequestBody(matchingJsonPath("$.shipmentIds[0]", equalTo(SHIPMENT_ID)))
                .withRequestBody(matchingJsonPath("$.address.city", equalTo(SELLER_CITY)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PROPOSALS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PickupProposals proposals = allegro.shipping().pickupProposals(sampleProposalsRequest());

            // then — the address and per-shipment windows map (deprecated proposal items ignored)
            assertEquals(SELLER_CITY, proposals.address().city());
            assertEquals(1, proposals.proposals().size());
            assertEquals(SHIPMENT_ID, proposals.proposals().get(0).shipmentId());
            List<PickupTime> windows = proposals.proposals().get(0).pickupTimes();
            assertEquals(EXPECTED_WINDOW_COUNT, windows.size());
            assertEquals(PICKUP_DATE, windows.get(0).date());
            assertEquals(MIN_TIME, windows.get(0).minTime());
            assertEquals(MAX_TIME, windows.get(0).maxTime());
        }

        verify(postRequestedFor(urlEqualTo(PROPOSALS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void requestPickup_whenCommandSucceeds_submitsPollsAndReadsPickup(WireMockRuntimeInfo wmInfo) {
        // given — submit accepted, poll resolves to SUCCESS, then the pickup is read back
        stubToken();
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .withRequestBody(matchingJsonPath("$.input.shipmentIds[0]", equalTo(SHIPMENT_ID)))
                .withRequestBody(matchingJsonPath("$.input.pickupTime.date", equalTo(PICKUP_DATE)))
                .withRequestBody(matchingJsonPath("$.input.address.city", equalTo(SELLER_CITY)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubPickupGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Pickup pickup = allegro.shipping().requestPickup(samplePickupRequest());

            // then — the booked pickup was read back and mapped
            assertEquals(PICKUP_ID, pickup.id());
            assertEquals(CARRIER, pickup.carrier());
            assertEquals(CARRIER_PICKUP_ID, pickup.carrierPickupId());
            assertEquals(List.of(SHIPMENT_ID), pickup.shipmentIds());
            assertEquals(List.of(WAYBILL), pickup.waybills());
            verify(1, postRequestedFor(urlEqualTo(CREATE_PATH)));
        }
    }

    @Test
    void requestPickup_whenPollInProgressThenSuccess_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first poll is IN_PROGRESS, the second SUCCESS
        stubToken();
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH))).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_IN_PROGRESS))
                .willSetStateTo(STATE_DONE));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH))).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_DONE)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubPickupGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Pickup pickup = allegro.shipping().requestPickup(samplePickupRequest());

            // then — the SDK polled more than once before resolving
            assertEquals(PICKUP_ID, pickup.id());
            verify(moreThanOrExactly(MIN_POLLS), getRequestedFor(urlEqualTo(CREATE_POLL_PATH)));
        }
    }

    @Test
    void requestPickup_whenTimeoutExceeded_throwsAsyncTimeout(WireMockRuntimeInfo wmInfo) {
        // given — the command never leaves IN_PROGRESS
        stubToken();
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_IN_PROGRESS)));

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();
            PickupRequest request = samplePickupRequest();

            // then — the Duration overload gives up with a timeout, never booking
            assertThrows(AllegroAsyncTimeoutException.class,
                    () -> shipping.requestPickup(request, TINY_TIMEOUT));
        }

        // and — the pickup was never booked (no read of the booked-pickup resource)
        verify(0, getRequestedFor(urlEqualTo(PICKUP_PATH)));
    }

    @Test
    void requestPickup_whenCommandEndsInError_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given — the command resolves to ERROR carrying a typed field error
        stubToken();
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();
            PickupRequest request = samplePickupRequest();

            // then — the command failure surfaces as a bad-request with the errors[] mapped
            AllegroBadRequestException thrown = assertThrows(AllegroBadRequestException.class,
                    () -> shipping.requestPickup(request));
            assertTrue(thrown.errors().stream()
                    .anyMatch(error -> ERROR_PATH.equals(error.path())));
        }
    }

    @Test
    void getPickup_whenExists_mapsPickup(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubPickupGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Pickup pickup = allegro.shipping().getPickup(PICKUP_ID);

            // then
            assertEquals(PICKUP_ID, pickup.id());
            assertEquals(SELLER_CITY, pickup.address().city());
            assertEquals(List.of(SHIPMENT_ID), pickup.shipmentIds());
        }
    }

    @Test
    void getPickup_whenIdBlank_throwsBeforeAnyCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then — the id guard fails fast without a network call
            assertThrows(IllegalArgumentException.class, () -> shipping.getPickup(" "));
        }

        verify(0, getRequestedFor(urlMatching(PICKUP_PATH_PATTERN)));
    }

    private static PostalAddress sellerAddress() {
        return PostalAddress.builder()
                .street(SELLER_STREET).postalCode(SELLER_POSTAL).city(SELLER_CITY)
                .email(SELLER_EMAIL).phone(SELLER_PHONE).build();
    }

    private static PickupRequest samplePickupRequest() {
        return PickupRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID))
                .pickupTime(PickupTime.of(PICKUP_DATE, MIN_TIME, MAX_TIME))
                .address(sellerAddress())
                .build();
    }

    private static PickupProposalsRequest sampleProposalsRequest() {
        return PickupProposalsRequest.builder()
                .shipmentIds(List.of(SHIPMENT_ID))
                .address(sellerAddress())
                .build();
    }

    private static void stubPickupGet() {
        stubFor(authed(get(urlEqualTo(PICKUP_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PICKUP_FIXTURE)));
    }

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static MappingBuilder authed(MappingBuilder stub) {
        return stub.withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN));
    }

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }
}
