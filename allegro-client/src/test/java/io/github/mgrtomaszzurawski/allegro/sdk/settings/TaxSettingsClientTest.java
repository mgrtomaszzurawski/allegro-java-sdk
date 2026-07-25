/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.tax.model.TaxSettings;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the category tax-settings read: the {@code category.id}
 * query parameter on the wire, subject/rate/exemption mapping, and the 400
 * error path.
 *
 * <p>Response body is spec-derived; the live read is proved by the
 * {@code settings-size-tables} demo (which also reads a category's tax settings)
 * before this PR is merge-ready.
 */
@WireMockTest
class TaxSettingsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TAX_PATH = "/sale/tax-settings";
    private static final String PARAM_CATEGORY_ID = "category.id";
    private static final String PARAM_COUNTRY_CODE = "countryCode";
    private static final String CATEGORY_ID = "316194";
    private static final String COUNTRY = "PL";
    private static final String COUNTRY_CZ = "CZ";
    private static final String RATE_VALUE = "23.00";
    private static final String SUBJECT_VALUE = "GOODS";
    private static final String EXEMPTION_VALUE = "MARGIN_SCHEME";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String TAX_RESPONSE = """
            {"subjects":[{"label":"Goods","value":"GOODS"}],
             "rates":[{"countryCode":"PL","values":[
                {"label":"23%","value":"23.00","exemptionRequired":false},
                {"label":"Poza VAT / NP","value":"0","exemptionRequired":true}]}],
             "exemptions":[{"label":"Procedura marży","value":"MARGIN_SCHEME"}]}
            """;
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationException","message":"category.id is required",
              "userMessage":"Kategoria jest wymagana","path":"category.id"}]}
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
    void taxSettings_whenCategoryGiven_sendsCategoryIdAndMaps(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(TAX_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withQueryParam(PARAM_CATEGORY_ID, equalTo(CATEGORY_ID))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAX_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            TaxSettings settings = allegro.settings().taxSettings(CATEGORY_ID);

            assertEquals(1, settings.subjects().size());
            assertEquals(SUBJECT_VALUE, settings.subjects().get(0).value());
            assertEquals(1, settings.rates().size());
            assertEquals(COUNTRY, settings.rates().get(0).countryCode());
            assertEquals(RATE_VALUE, settings.rates().get(0).values().get(0).value());
            assertTrue(settings.rates().get(0).values().get(1).exemptionRequired());
            assertEquals(EXEMPTION_VALUE, settings.exemptions().get(0).value());
            verify(1, getRequestedFor(urlPathEqualTo(TAX_PATH))
                    .withQueryParam(PARAM_CATEGORY_ID, equalTo(CATEGORY_ID)));
        }
    }

    @Test
    void taxSettings_whenCountryCodesGiven_sendsRepeatedCountryParam(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(TAX_PATH))
                .withQueryParam(PARAM_CATEGORY_ID, equalTo(CATEGORY_ID))
                .withQueryParam(PARAM_COUNTRY_CODE, equalTo(COUNTRY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAX_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            allegro.settings().taxSettings(CATEGORY_ID, List.of(COUNTRY, COUNTRY_CZ));

            verify(1, getRequestedFor(urlPathEqualTo(TAX_PATH))
                    .withQueryParam(PARAM_CATEGORY_ID, equalTo(CATEGORY_ID))
                    .withQueryParam(PARAM_COUNTRY_CODE, equalTo(COUNTRY))
                    .withQueryParam(PARAM_COUNTRY_CODE, equalTo(COUNTRY_CZ)));
        }
    }

    @Test
    void taxSettings_whenBadRequest_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(TAX_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            assertThrows(AllegroBadRequestException.class,
                    () -> allegro.settings().taxSettings(CATEGORY_ID));
        }
    }
}
