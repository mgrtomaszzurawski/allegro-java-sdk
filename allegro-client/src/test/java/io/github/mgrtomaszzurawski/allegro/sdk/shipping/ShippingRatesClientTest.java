/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.shipping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.RateSetType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSet;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.ShippingRateSetSummary;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.shipping.model.Weight;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract tests for {@code shipping().rates()} — list, get, create and
 * (PUT-semantics) update of shipping-rate sets. Pins the auth header, path, the
 * write bodies (nested rate rows), and the response mapping. The full transport
 * error-path table (401 replay, 404, 429, 5xx) is exercised once for the shipping
 * domain by {@link ShippingPointsOfServiceClientTest}; this class adds the write
 * bad-request field-error path.
 *
 * <p>Fixture provenance: the read shape is wire-verified 2026-07-18 (sandbox) —
 * the {@code shipping-rates} demo probe read 7 live sets and a 47-row set in
 * full — and re-verified 2026-11-01 on the re-vendored spec (no
 * {@code nextItemRate}; {@code type}/{@code dispatchCountry} on the shared set
 * schema). The write shape is live-verified 2026-11-01: the probe PUT a
 * seller-editable set under a marker name, read it back and restored it through
 * the SDK; see {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
@WireMockTest
class ShippingRatesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String RATES_PATH = "/sale/shipping-rates";
    private static final String RATE_SET_ID = "rate-set-1";
    private static final String RATE_SET_PATH = RATES_PATH + "/" + RATE_SET_ID;
    private static final String LIST_FIXTURE = "shipping/shipping-rates-list.json";
    private static final String SET_FIXTURE = "shipping/shipping-rate-set.json";

    private static final int LIST_SIZE = 2;
    private static final String SET_NAME = "Domestic rates";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String METHOD_ID = "method-courier";
    private static final String FIRST_AMOUNT = "12.99";
    private static final String CURRENCY = "PLN";
    private static final String WEIGHT_VALUE = "30.0";
    private static final String WEIGHT_UNIT = "KILOGRAMS";
    private static final int MAX_QUANTITY = 10;
    private static final String DISPATCH_COUNTRY = "PL";
    private static final String TYPE_PHYSICAL = "PHYSICAL";
    private static final String NEW_SET_NAME = "New rates";

    // spec-derived: a legacy physical set returned without `type`/`dispatchCountry`
    private static final String LEGACY_SET_WITHOUT_TYPE = """
            {"id":"%s","name":"%s","lastModified":"2026-07-18T12:00:00Z",
             "rates":[{"deliveryMethod":{"id":"%s"},"maxQuantityPerPackage":%d,
               "firstItemRate":{"amount":"%s","currency":"%s"}}],
             "features":{"managedByAllegro":false,"isFulfillment":false}}
            """.formatted(RATE_SET_ID, SET_NAME, METHOD_ID, MAX_QUANTITY, FIRST_AMOUNT, CURRENCY);

    private static final String BAD_REQUEST_BODY = """
            {"errors":[{"code":"RateInvalid","message":"first item rate is invalid",\
            "userMessage":"Podaj poprawną stawkę","path":"rates[0].firstItemRate.amount"}]}
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

    private static ShippingRate courierRate() {
        return ShippingRate.builder()
                .deliveryMethodId(METHOD_ID)
                .firstItemRate(Money.of(FIRST_AMOUNT, CURRENCY))
                .maxQuantityPerPackage(MAX_QUANTITY)
                .maxPackageWeight(new Weight(WEIGHT_VALUE, WEIGHT_UNIT))
                .build();
    }

    @Test
    void list_whenSetsExist_mapsSummaries(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(RATES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(LIST_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<ShippingRateSetSummary> sets = allegro.shipping().rates().list();

            // then
            assertEquals(LIST_SIZE, sets.size());
            ShippingRateSetSummary first = sets.get(0);
            assertEquals(RATE_SET_ID, first.id());
            assertEquals(SET_NAME, first.name());
            assertEquals(List.of(MARKETPLACE_PL), first.marketplaces());
            assertFalse(first.features().managedByAllegro());
            // the managed set carries the flags and an empty marketplace list
            assertTrue(sets.get(1).features().managedByAllegro());
            assertTrue(sets.get(1).marketplaces().isEmpty());
        }
    }

    @Test
    void get_whenSetExists_mapsSetAndNestedRateRows(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlEqualTo(RATE_SET_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            ShippingRateSet rateSet = allegro.shipping().rates().get(RATE_SET_ID);

            // then — set metadata and the single rate row's nested fields all map
            assertEquals(RateSetType.PHYSICAL, rateSet.type());
            assertEquals(DISPATCH_COUNTRY, rateSet.dispatchCountry());
            assertEquals(1, rateSet.rates().size());
            ShippingRate rate = rateSet.rates().get(0);
            assertEquals(METHOD_ID, rate.deliveryMethodId());
            assertEquals(Money.of(FIRST_AMOUNT, CURRENCY), rate.firstItemRate());
            assertEquals(MAX_QUANTITY, rate.maxQuantityPerPackage());
            assertEquals(WEIGHT_UNIT, rate.maxPackageWeight().unit());
            assertEquals("24", rate.shippingTime().fromTime());
        }
    }

    @Test
    void get_whenLegacySetWithoutType_mapsTypeToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — spec-derived: the pre-2026-11 spec documented physical sets created before the
        // `type` field existed as returned without it (the field is required since 2026-11)
        stubToken();
        stubFor(get(urlEqualTo(RATE_SET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(LEGACY_SET_WITHOUT_TYPE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            ShippingRateSet rateSet = allegro.shipping().rates().get(RATE_SET_ID);

            // then — the absent type reads fail-soft as UNKNOWN, the rest of the set still maps
            assertEquals(RateSetType.UNKNOWN, rateSet.type());
            assertNull(rateSet.dispatchCountry());
            assertEquals(SET_NAME, rateSet.name());
            assertEquals(1, rateSet.rates().size());
            assertEquals(METHOD_ID, rateSet.rates().get(0).deliveryMethodId());
            verify(1, getRequestedFor(urlEqualTo(RATE_SET_PATH)));
        }
    }

    @Test
    void create_whenValidRequest_postsSetBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(RATES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(NEW_SET_NAME)))
                .withRequestBody(matchingJsonPath("$.type", equalTo(TYPE_PHYSICAL)))
                .withRequestBody(matchingJsonPath("$.dispatchCountry", equalTo(DISPATCH_COUNTRY)))
                .withRequestBody(matchingJsonPath("$.rates[0].deliveryMethod.id", equalTo(METHOD_ID)))
                .withRequestBody(matchingJsonPath("$.rates[0].firstItemRate.amount",
                        equalTo(FIRST_AMOUNT)))
                .withRequestBody(matchingJsonPath("$.rates[0].maxQuantityPerPackage",
                        equalTo(String.valueOf(MAX_QUANTITY))))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBodyFile(SET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            ShippingRateSet created = allegro.shipping().rates().create(
                    ShippingRateSetRequest.builder()
                            .name(NEW_SET_NAME)
                            .type(RateSetType.PHYSICAL)
                            .dispatchCountry(DISPATCH_COUNTRY)
                            .rates(List.of(courierRate()))
                            .build());

            // then — the create response maps through the shared set DTO like update does
            assertEquals(RATE_SET_ID, created.id());
            assertEquals(RateSetType.PHYSICAL, created.type());
            assertEquals(DISPATCH_COUNTRY, created.dispatchCountry());
            assertEquals(1, created.rates().size());
            verify(1, postRequestedFor(urlEqualTo(RATES_PATH))
                    // the partial writer keeps the server-assigned `id` and `lastModified` off a create body
                    .withRequestBody(matchingJsonPath("$.id", absent()))
                    .withRequestBody(matchingJsonPath("$.lastModified", absent())));
        }
    }

    @Test
    void update_whenValidRequest_putsSetWithBodyIdAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(put(urlEqualTo(RATE_SET_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withRequestBody(matchingJsonPath("$.id", equalTo(RATE_SET_ID)))
                .withRequestBody(matchingJsonPath("$.name", equalTo(NEW_SET_NAME)))
                .withRequestBody(matchingJsonPath("$.type", equalTo(TYPE_PHYSICAL)))
                .withRequestBody(matchingJsonPath("$.dispatchCountry", equalTo(DISPATCH_COUNTRY)))
                .withRequestBody(matchingJsonPath("$.rates[0].deliveryMethod.id", equalTo(METHOD_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(SET_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            ShippingRateSet updated = allegro.shipping().rates().update(RATE_SET_ID,
                    ShippingRateSetRequest.builder()
                            .name(NEW_SET_NAME)
                            .type(RateSetType.PHYSICAL)
                            .dispatchCountry(DISPATCH_COUNTRY)
                            .rates(List.of(courierRate()))
                            .build());

            // then — the PUT body carried the path id, as Allegro requires, and the
            // create/update responses map through the shared set DTO
            assertEquals(RATE_SET_ID, updated.id());
            assertEquals(RateSetType.PHYSICAL, updated.type());
            assertEquals(DISPATCH_COUNTRY, updated.dispatchCountry());
            assertEquals(1, updated.rates().size());
            verify(1, putRequestedFor(urlEqualTo(RATE_SET_PATH)));
        }
    }

    @Test
    void create_whenServerRejectsBody_throwsBadRequestWithFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlEqualTo(RATES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(BAD_REQUEST_BODY)));

        try (AllegroClient allegro = client(wmInfo)) {
            ShippingRateSetRequest request = ShippingRateSetRequest.builder()
                    .name(NEW_SET_NAME).type(RateSetType.PHYSICAL).dispatchCountry(DISPATCH_COUNTRY)
                    .rates(List.of(courierRate())).build();
            var rates = allegro.shipping().rates();

            // then — the parsed field error names the offending rate path
            AllegroBadRequestException rejected = assertThrows(AllegroBadRequestException.class,
                    () -> rates.create(request));
            assertEquals("rates[0].firstItemRate.amount", rejected.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(RATES_PATH)));
        }
    }
}
