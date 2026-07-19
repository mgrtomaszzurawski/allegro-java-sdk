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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelPageSize;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PostalAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Shipment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the shipment-management operations on
 * {@code shipping()} — the async create/cancel commands (submit → poll →
 * resolve), the shipment read, and the binary label/protocol renders. Stubs pin
 * the auth header, method+path, request body and the {@code Accept} media type;
 * the async flow is proven by verifying the poll count.
 *
 * <p>The shared transport error-path table (401 replay, 404, 429, 5xx) is
 * exercised once for the shipping domain by
 * {@link ShippingPointsOfServiceClientTest}; this class adds the command-specific
 * failure paths (a command that ends in {@code ERROR}).
 *
 * <p>Fixture provenance: {@code spec-derived} — the shipment-management write path
 * needs a real seeded order (WZA broker) to wire-verify; see
 * {@code KNOWN-SERVER-BEHAVIORS.md} and the bucket-C PR-2c note in the plan.
 */
@WireMockTest
class ShippingShipmentClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String CREATE_PATH = "/shipment-management/shipments/create-commands";
    private static final String CREATE_POLL_PATH = CREATE_PATH + "/CMD-CREATE-1";
    private static final String CANCEL_PATH = "/shipment-management/shipments/cancel-commands";
    private static final String CANCEL_POLL_PATH = CANCEL_PATH + "/CMD-CANCEL-1";
    private static final String SHIPMENT_PATH = "/shipment-management/shipments/SHIP-1001";
    private static final String LABEL_PATH = "/shipment-management/label";
    private static final String PROTOCOL_PATH = "/shipment-management/protocol";

    private static final String SHIPMENT_ID = "SHIP-1001";
    private static final String CITY_SENDER = "Gdansk";
    private static final String CITY_RECEIVER = "Warszawa";
    private static final String CURRENCY = "PLN";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String POLL_SCENARIO = "poll-create";
    private static final String STATE_DONE = "done";

    private static final byte[] LABEL_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final byte[] PROTOCOL_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2E};

    private static final String CREATE_ACCEPTED = "shipping/shipment-create-accepted.json";
    private static final String CREATE_IN_PROGRESS = "shipping/shipment-create-status-inprogress.json";
    private static final String CREATE_SUCCESS = "shipping/shipment-create-status-success.json";
    private static final String CREATE_ERROR = "shipping/shipment-create-status-error.json";
    private static final String CANCEL_ACCEPTED = "shipping/shipment-cancel-accepted.json";
    private static final String CANCEL_SUCCESS = "shipping/shipment-cancel-status-success.json";
    private static final String SHIPMENT_FIXTURE = "shipping/shipment-get.json";

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

    private static void stubShipmentGet() {
        stubFor(get(urlEqualTo(SHIPMENT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SHIPMENT_FIXTURE)));
    }

    private static ShipmentRequest sampleRequest() {
        return ShipmentRequest.builder()
                .sender(PostalAddress.builder()
                        .street("Grunwaldzka 100").postalCode("80-244").city(CITY_SENDER)
                        .email("sender@example.com").phone("+48500100100").build())
                .receiver(PostalAddress.builder()
                        .street("Marszalkowska 1").postalCode("00-001").city(CITY_RECEIVER)
                        .email("receiver@example.com").phone("+48500200200").build())
                .packages(List.of(ShipmentPackage.builder()
                        .type(PackageType.PACKAGE)
                        .lengthCm(new BigDecimal("30.0")).widthCm(new BigDecimal("20.0"))
                        .heightCm(new BigDecimal("10.0")).weightKg(new BigDecimal("2.5"))
                        .build()))
                .labelFormat(LabelFormat.PDF)
                .build();
    }

    @Test
    void createShipment_whenCommandSucceeds_submitsPollsAndReadsShipment(WireMockRuntimeInfo wmInfo) {
        // given — submit accepted, poll resolves to SUCCESS, then the shipment is read back
        stubToken();
        stubFor(post(urlEqualTo(CREATE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.input.sender.city", equalTo(CITY_SENDER)))
                .withRequestBody(matchingJsonPath("$.input.receiver.city", equalTo(CITY_RECEIVER)))
                .withRequestBody(matchingJsonPath("$.input.packages[0].type", equalTo("PACKAGE")))
                .withRequestBody(matchingJsonPath("$.input.labelFormat", equalTo("PDF")))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(get(urlEqualTo(CREATE_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubShipmentGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Shipment shipment = allegro.shipping().createShipment(sampleRequest());

            // then — the created shipment was read back and mapped
            assertEquals(SHIPMENT_ID, shipment.id());
            assertEquals(CITY_SENDER, shipment.sender().city());
            assertEquals(PackageType.PACKAGE, shipment.packages().get(0).type());
            verify(1, postRequestedFor(urlEqualTo(CREATE_PATH)));
        }
    }

    @Test
    void createShipment_whenCommandStillRunning_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first poll is IN_PROGRESS, the second SUCCESS
        stubToken();
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(get(urlEqualTo(CREATE_POLL_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_IN_PROGRESS))
                .willSetStateTo(STATE_DONE));
        stubFor(get(urlEqualTo(CREATE_POLL_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_DONE)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubShipmentGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Shipment shipment = allegro.shipping().createShipment(sampleRequest());

            // then — the poll loop ran twice before resolving
            assertEquals(SHIPMENT_ID, shipment.id());
            verify(2, getRequestedFor(urlEqualTo(CREATE_POLL_PATH)));
        }
    }

    @Test
    void createShipment_whenCommandEndsInError_throwsAllegroException(WireMockRuntimeInfo wmInfo) {
        // given — the command reaches a terminal ERROR status
        stubToken();
        stubFor(post(urlEqualTo(CREATE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(get(urlEqualTo(CREATE_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            ShipmentRequest request = sampleRequest();
            var shipping = allegro.shipping();

            // then — a non-success terminal status surfaces as an exception, no shipment read
            assertThrows(AllegroException.class, () -> shipping.createShipment(request));
            verify(0, getRequestedFor(urlEqualTo(SHIPMENT_PATH)));
        }
    }

    @Test
    void getShipment_whenExists_mapsEveryField(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubShipmentGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Shipment shipment = allegro.shipping().getShipment(SHIPMENT_ID);

            // then — nested addresses, package, insurance and cash-on-delivery all map
            assertEquals("CRED-77", shipment.credentialsId());
            assertEquals(CITY_RECEIVER, shipment.receiver().city());
            assertEquals("POP-42", shipment.receiver().point());
            ShipmentPackage parcel = shipment.packages().get(0);
            assertEquals(new BigDecimal("30.0"), parcel.lengthCm());
            assertEquals(new BigDecimal("2.5"), parcel.weightKg());
            assertEquals("WB-555", parcel.waybill());
            assertEquals(Money.of("199.99", CURRENCY), shipment.insurance());
            assertEquals("PL61109010140000071219812874", shipment.cashOnDelivery().iban());
            assertEquals(LabelFormat.PDF, shipment.labelFormat());
            assertNull(shipment.canceledDate());
        }
    }

    @Test
    void cancelShipment_whenCommandSucceeds_submitsShipmentIdAndPolls(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(CANCEL_PATH))
                .withRequestBody(matchingJsonPath("$.input.shipmentId", equalTo(SHIPMENT_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CANCEL_ACCEPTED)));
        stubFor(get(urlEqualTo(CANCEL_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CANCEL_SUCCESS)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.shipping().cancelShipment(SHIPMENT_ID);

            // then — the cancel command carried the shipment id and was polled to success
            verify(1, postRequestedFor(urlEqualTo(CANCEL_PATH)));
            verify(1, getRequestedFor(urlEqualTo(CANCEL_POLL_PATH)));
        }
    }

    @Test
    void labels_whenRequested_postsBodyWithOctetAcceptAndReturnsBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(LABEL_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(OCTET_STREAM))
                .withRequestBody(matchingJsonPath("$.shipmentIds[0]", equalTo(SHIPMENT_ID)))
                .withRequestBody(matchingJsonPath("$.pageSize", equalTo("A4")))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(LABEL_BYTES)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            byte[] label = allegro.shipping().labels(LabelRequest.builder()
                    .shipmentIds(List.of(SHIPMENT_ID))
                    .pageSize(LabelPageSize.A4)
                    .build());

            // then — the raw bytes come back and the Accept media type was octet-stream
            assertArrayEquals(LABEL_BYTES, label);
            verify(1, postRequestedFor(urlEqualTo(LABEL_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(OCTET_STREAM)));
        }
    }

    @Test
    void protocol_whenGivenIds_postsIdsAndReturnsBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(PROTOCOL_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(OCTET_STREAM))
                .withRequestBody(matchingJsonPath("$.shipmentIds[0]", equalTo(SHIPMENT_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PROTOCOL_BYTES)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            byte[] protocol = allegro.shipping().protocol(SHIPMENT_ID);

            // then
            assertArrayEquals(PROTOCOL_BYTES, protocol);
            verify(1, postRequestedFor(urlEqualTo(PROTOCOL_PATH)));
        }
    }

    @Test
    void protocol_whenNoIds_throwsIllegalArgumentBeforeAnyCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then — the empty-args guard fails fast without touching the network
            assertThrows(IllegalArgumentException.class, shipping::protocol);
            verify(0, postRequestedFor(urlEqualTo(PROTOCOL_PATH)));
        }
    }
}
