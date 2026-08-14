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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
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
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for the shipment-management operations on
 * {@code shipping()} — the async create/cancel commands (submit → poll →
 * resolve), the shipment read, and the binary label/protocol renders. Every
 * stub pins the {@code Authorization: Bearer} header and the method+path; writes
 * also pin the request body and the {@code Accept} media type. The async flow is
 * proven by verifying the poll count, and command failures assert the parsed
 * {@code errors[]}.
 *
 * <p>The shared transport error-path table (401 replay, 404, 429, 5xx over the
 * JSON path) is exercised once for the shipping domain by
 * {@link ShippingPointsOfServiceClientTest}; this class adds the command-specific
 * failure paths (a command that ends in {@code ERROR}) and a server-error case on
 * the distinct binary ({@code fetchBytes}) path.
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
    private static final String CREDENTIALS_ID = "CRED-77";
    private static final String WAYBILL = "WB-555";
    private static final String EXPECTED_DELIVERY_METHOD_ID = "DM-9";
    private static final String EXPECTED_ADDITIONAL_SERVICE = "SATURDAY_DELIVERY";
    private static final String EXPECTED_TRANSPORT = "ROAD";
    private static final String PROP_SORTING_CODE = "sortingCode";
    private static final String EXPECTED_SORTING_CODE = "WAW-3";
    private static final String EXPECTED_CARRIER_WAYBILL = "CW-9001";
    private static final String RECEIVER_POINT = "POP-42";
    private static final String INSURANCE_AMOUNT = "199.99";
    private static final String IBAN = "PL61109010140000071219812874";
    private static final String CURRENCY = "PLN";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String POLL_SCENARIO = "poll-create";
    private static final String STATE_DONE = "done";

    private static final String SENDER_STREET = "Grunwaldzka 100";
    private static final String SENDER_POSTAL = "80-244";
    private static final String SENDER_CITY = "Gdansk";
    private static final String SENDER_EMAIL = "sender@example.com";
    private static final String SENDER_PHONE = "+48500100100";
    private static final String RECEIVER_STREET = "Marszalkowska 1";
    private static final String RECEIVER_POSTAL = "00-001";
    private static final String RECEIVER_CITY = "Warszawa";
    private static final String RECEIVER_EMAIL = "receiver@example.com";
    private static final String RECEIVER_PHONE = "+48500200200";

    private static final BigDecimal LENGTH_CM = new BigDecimal("30.0");
    private static final BigDecimal WIDTH_CM = new BigDecimal("20.0");
    private static final BigDecimal HEIGHT_CM = new BigDecimal("10.0");
    private static final BigDecimal WEIGHT_KG = new BigDecimal("2.5");

    private static final byte[] LABEL_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final byte[] PROTOCOL_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2E};

    private static final String CREATE_ACCEPTED = "shipping/shipment-create-accepted.json";
    private static final String CREATE_IN_PROGRESS = "shipping/shipment-create-status-inprogress.json";
    private static final String CREATE_SUCCESS = "shipping/shipment-create-status-success.json";
    private static final String CREATE_ERROR = "shipping/shipment-create-status-error.json";
    private static final String CANCEL_ACCEPTED = "shipping/shipment-cancel-accepted.json";
    private static final String CANCEL_SUCCESS = "shipping/shipment-cancel-status-success.json";
    private static final String CANCEL_ERROR = "shipping/shipment-cancel-status-error.json";
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

    /** Pins the bearer token every stub in this class shares. */
    private static MappingBuilder authed(MappingBuilder stub) {
        return stub.withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN));
    }

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    private static void stubShipmentGet() {
        stubFor(authed(get(urlEqualTo(SHIPMENT_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SHIPMENT_FIXTURE)));
    }

    private static void stubCreateAccepted() {
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
    }

    private static ShipmentRequest sampleRequest() {
        return ShipmentRequest.builder()
                .credentialsId(CREDENTIALS_ID)
                .sender(PostalAddress.builder()
                        .street(SENDER_STREET).postalCode(SENDER_POSTAL).city(SENDER_CITY)
                        .email(SENDER_EMAIL).phone(SENDER_PHONE).build())
                .receiver(PostalAddress.builder()
                        .street(RECEIVER_STREET).postalCode(RECEIVER_POSTAL).city(RECEIVER_CITY)
                        .email(RECEIVER_EMAIL).phone(RECEIVER_PHONE).build())
                .packages(List.of(ShipmentPackage.builder()
                        .type(PackageType.PACKAGE)
                        .lengthCm(LENGTH_CM).widthCm(WIDTH_CM)
                        .heightCm(HEIGHT_CM).weightKg(WEIGHT_KG)
                        .build()))
                .labelFormat(LabelFormat.PDF)
                .build();
    }

    @Test
    void createShipment_whenCommandSucceeds_submitsPollsAndReadsShipment(WireMockRuntimeInfo wmInfo) {
        // given — submit accepted, poll resolves to SUCCESS, then the shipment is read back
        stubToken();
        stubFor(authed(post(urlEqualTo(CREATE_PATH)))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.input.sender.city", equalTo(SENDER_CITY)))
                .withRequestBody(matchingJsonPath("$.input.receiver.city", equalTo(RECEIVER_CITY)))
                .withRequestBody(matchingJsonPath("$.input.packages[0].type", equalTo("PACKAGE")))
                .withRequestBody(matchingJsonPath("$.input.labelFormat", equalTo("PDF")))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubShipmentGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Shipment shipment = allegro.shipping().createShipment(sampleRequest());

            // then — the created shipment was read back and mapped
            assertEquals(SHIPMENT_ID, shipment.id());
            assertEquals(SENDER_CITY, shipment.sender().city());
            assertEquals(PackageType.PACKAGE, shipment.packages().get(0).type());
            verify(1, postRequestedFor(urlEqualTo(CREATE_PATH)));
        }
    }

    @Test
    void createShipment_whenCommandStillRunning_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first poll is IN_PROGRESS, the second SUCCESS
        stubToken();
        stubCreateAccepted();
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH))).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_IN_PROGRESS))
                .willSetStateTo(STATE_DONE));
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH))).inScenario(POLL_SCENARIO)
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
    void createShipment_whenTimeoutOverrideGiven_resolvesWithinBudget(WireMockRuntimeInfo wmInfo) {
        // given — the overload with an explicit timeout resolves the same way
        stubToken();
        stubCreateAccepted();
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_SUCCESS)));
        stubShipmentGet();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            Shipment shipment =
                    allegro.shipping().createShipment(sampleRequest(), Duration.ofSeconds(30));

            // then
            assertEquals(SHIPMENT_ID, shipment.id());
        }
    }

    @Test
    void createShipment_whenCommandNeverTerminal_throwsTimeout(WireMockRuntimeInfo wmInfo) {
        // given — the poll never leaves IN_PROGRESS and the timeout budget is tiny
        stubToken();
        stubCreateAccepted();
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_IN_PROGRESS)));

        try (AllegroClient allegro = client(wmInfo)) {
            ShipmentRequest request = sampleRequest();
            var shipping = allegro.shipping();
            Duration tinyBudget = Duration.ofMillis(1);

            // then — the poller gives up with a timeout, no shipment read
            assertThrows(AllegroAsyncTimeoutException.class,
                    () -> shipping.createShipment(request, tinyBudget));
            verify(0, getRequestedFor(urlEqualTo(SHIPMENT_PATH)));
        }
    }

    @Test
    void createShipment_whenCommandEndsInError_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given — the command reaches a terminal ERROR carrying errors[]
        stubToken();
        stubCreateAccepted();
        stubFor(authed(get(urlEqualTo(CREATE_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CREATE_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            ShipmentRequest request = sampleRequest();
            var shipping = allegro.shipping();

            // then — the command's typed error detail is surfaced, no shipment read
            AllegroBadRequestException failed = assertThrows(AllegroBadRequestException.class,
                    () -> shipping.createShipment(request));
            assertEquals("input.deliveryMethodId", failed.errors().get(0).path());
            assertEquals("DeliveryMethodNotSupported", failed.errors().get(0).code());
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
            assertEquals(CREDENTIALS_ID, shipment.credentialsId());
            assertEquals(RECEIVER_CITY, shipment.receiver().city());
            assertEquals(RECEIVER_POINT, shipment.receiver().point());
            ShipmentPackage parcel = shipment.packages().get(0);
            assertEquals(LENGTH_CM, parcel.lengthCm());
            assertEquals(WEIGHT_KG, parcel.weightKg());
            assertEquals(WAYBILL, parcel.waybill());
            assertEquals(Money.of(INSURANCE_AMOUNT, CURRENCY), shipment.insurance());
            assertEquals(IBAN, shipment.cashOnDelivery().iban());
            assertEquals(LabelFormat.PDF, shipment.labelFormat());
            assertNull(shipment.canceledDate());
            // enriched depth: delivery method, additional services/transport/properties,
            // and per-parcel transporting info
            assertEquals(EXPECTED_DELIVERY_METHOD_ID, shipment.deliveryMethodId());
            assertEquals(List.of(EXPECTED_ADDITIONAL_SERVICE), shipment.additionalServices());
            assertEquals(List.of(EXPECTED_TRANSPORT), shipment.transport());
            assertEquals(EXPECTED_SORTING_CODE, shipment.additionalProperties().get(PROP_SORTING_CODE));
            assertEquals(1, parcel.transportingInfo().size());
            assertEquals(EXPECTED_CARRIER_WAYBILL, parcel.transportingInfo().get(0).carrierWaybill());
        }
    }

    @Test
    void getShipment_whenIdBlank_throwsBeforeAnyCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then — the id guard fails fast without a network call
            assertThrows(IllegalArgumentException.class, () -> shipping.getShipment(" "));
        }
    }

    @Test
    void cancelShipment_whenCommandSucceeds_submitsShipmentIdAndPolls(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(authed(post(urlEqualTo(CANCEL_PATH)))
                .withRequestBody(matchingJsonPath("$.input.shipmentId", equalTo(SHIPMENT_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CANCEL_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CANCEL_POLL_PATH)))
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
    void cancelShipment_whenCommandEndsInError_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given — the cancel command reaches a terminal ERROR carrying errors[]
        stubToken();
        stubFor(authed(post(urlEqualTo(CANCEL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CANCEL_ACCEPTED)));
        stubFor(authed(get(urlEqualTo(CANCEL_POLL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(CANCEL_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then
            AllegroBadRequestException failed = assertThrows(AllegroBadRequestException.class,
                    () -> shipping.cancelShipment(SHIPMENT_ID));
            assertEquals("ShipmentAlreadyCanceled", failed.errors().get(0).code());
        }
    }

    @Test
    void cancelShipment_whenIdNull_throwsBeforeAnyCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then
            assertThrows(IllegalArgumentException.class, () -> shipping.cancelShipment(null));
        }
    }

    @Test
    void labels_whenRequested_postsBodyWithOctetAcceptAndReturnsBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(authed(post(urlEqualTo(LABEL_PATH)))
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
    void labels_whenServerError_mapsToServerExceptionOnBinaryPath(WireMockRuntimeInfo wmInfo) {
        // given — the binary (fetchBytes) path shares the transport error mapping
        stubToken();
        stubFor(authed(post(urlEqualTo(LABEL_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            LabelRequest request = LabelRequest.builder().shipmentIds(List.of(SHIPMENT_ID)).build();
            var shipping = allegro.shipping();

            // then
            assertThrows(AllegroServerException.class, () -> shipping.labels(request));
        }
    }

    @Test
    void protocol_whenGivenIds_postsIdsAndReturnsBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(authed(post(urlEqualTo(PROTOCOL_PATH)))
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

    @Test
    void postalAddress_toString_redactsPersonalContactFields() {
        // given — a mapped shipment carries the sender/receiver contact details
        PostalAddress address = PostalAddress.builder()
                .street(SENDER_STREET).postalCode(SENDER_POSTAL).city(SENDER_CITY)
                .email(SENDER_EMAIL).phone(SENDER_PHONE).build();

        // then — an accidental log never renders the e-mail or phone
        String rendered = address.toString();
        assertFalse(rendered.contains(SENDER_EMAIL));
        assertFalse(rendered.contains(SENDER_PHONE));
    }
}
