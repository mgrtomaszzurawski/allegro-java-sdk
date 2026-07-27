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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryLimits;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryOption;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryPayment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryProposal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.DeliveryType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.LabelFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.PackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentPackage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShipmentRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract test for {@code shipping().deliveryOptionsFor(orderId)} — the
 * order's delivery proposal (the supported successor to the deprecated
 * delivery-services resource). The stub pins the {@code Authorization: Bearer}
 * header and the GET path; the assertions prove the whole tree maps: the order
 * id, the ready-to-submit {@link ShipmentRequest} reconstructed from the suggested
 * input, and one delivery option with its enums, limits and additional services.
 *
 * <p>Fixture provenance: {@code spec-derived} — a live read needs a real seeded
 * order behind the WZA broker; see {@code KNOWN-SERVER-BEHAVIORS.md} and the
 * bucket-C PR-2c note in the plan.
 */
@WireMockTest
class ShippingDeliveryProposalsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String ORDER_ID = "ORDER-9001";
    private static final String PROPOSALS_PATH = "/shipment-management/delivery-proposals/" + ORDER_ID;
    private static final String PROPOSALS_FIXTURE = "shipping/delivery-proposals.json";

    private static final String SENDER_CITY = "Gdansk";
    private static final String RECEIVER_POINT = "POP-42";
    private static final String CREDENTIALS_ID = "CRED-77";
    private static final String CURRENCY = "PLN";
    private static final String INSURANCE_AMOUNT = "199.99";
    private static final String IBAN = "PL61109010140000071219812874";
    private static final String COD_LIMIT = "5000.00";
    private static final String INSURANCE_LIMIT = "20000.00";
    private static final String WEIGHT_LIMIT = "30.0";
    private static final String LENGTH_LIMIT = "120.0";
    private static final String WIDTH_LIMIT = "80.0";
    private static final String ORIGIN_COUNTRY = "PL";
    private static final String SERVICE_ID = "SAT_DELIVERY";
    private static final String SERVICE_NAME = "Saturday delivery";
    private static final String PROPERTY_ID = "REF_NUMBER";
    private static final String PROPOSALS_PATH_PATTERN = "/shipment-management/delivery-proposals/.*";
    private static final BigDecimal WEIGHT_KG = new BigDecimal("2.5");

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    @Test
    void deliveryOptionsFor_whenOrderExists_mapsProposalAndSuggestedInput(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(authed(get(urlEqualTo(PROPOSALS_PATH)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PROPOSALS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            DeliveryProposal proposal = allegro.shipping().deliveryOptionsFor(ORDER_ID);

            // then — the order id and the ready-to-submit suggested input reconstruct
            assertEquals(ORDER_ID, proposal.orderId());
            ShipmentRequest suggested = proposal.suggestedInput();
            assertEquals(CREDENTIALS_ID, suggested.credentialsId());
            assertEquals(SENDER_CITY, suggested.sender().city());
            assertEquals(RECEIVER_POINT, suggested.receiver().point());
            assertEquals(LabelFormat.PDF, suggested.labelFormat());
            assertEquals(Money.of(INSURANCE_AMOUNT, CURRENCY), suggested.insurance());
            assertEquals(IBAN, suggested.cashOnDelivery().iban());
            ShipmentPackage parcel = suggested.packages().get(0);
            assertEquals(PackageType.PACKAGE, parcel.type());
            assertEquals(WEIGHT_KG, parcel.weightKg());

            // then — the single delivery option maps enums, limits and services
            assertEquals(1, proposal.deliveryOptions().size());
            DeliveryOption option = proposal.deliveryOptions().get(0);
            assertEquals(DeliveryType.DOOR, option.deliveryType());
            assertEquals(DeliveryPayment.PREPAID, option.paymentType());
            assertEquals(PackageType.PACKAGE, option.packageType());
            assertEquals(ORIGIN_COUNTRY, option.originCountry());
            DeliveryLimits limits = option.limits();
            assertEquals(Money.of(COD_LIMIT, CURRENCY), limits.cashOnDelivery());
            assertEquals(Money.of(INSURANCE_LIMIT, CURRENCY), limits.insurance());
            assertEquals(WEIGHT_LIMIT, limits.weight().value());
            assertEquals(LENGTH_LIMIT, limits.dimensions().length().value());
            assertEquals(WIDTH_LIMIT, limits.dimensions().width().value());
            assertEquals(SERVICE_ID, option.additionalServices().get(0).id());
            assertEquals(SERVICE_NAME, option.additionalServices().get(0).name());
            assertEquals(PROPERTY_ID, option.additionalProperties().get(0).id());
            assertFalse(option.additionalProperties().get(0).required());
        }

        // and — the read went out authenticated on the order-keyed path
        verify(getRequestedFor(urlEqualTo(PROPOSALS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void deliveryOptionsFor_whenOrderIdBlank_throwsBeforeAnyCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();

        try (AllegroClient allegro = client(wmInfo)) {
            var shipping = allegro.shipping();

            // then — the id guard fails fast without a network call
            assertThrows(IllegalArgumentException.class, () -> shipping.deliveryOptionsFor(" "));
        }

        // and — no proposal request ever left the client
        verify(0, getRequestedFor(urlMatching(PROPOSALS_PATH_PATTERN)));
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
