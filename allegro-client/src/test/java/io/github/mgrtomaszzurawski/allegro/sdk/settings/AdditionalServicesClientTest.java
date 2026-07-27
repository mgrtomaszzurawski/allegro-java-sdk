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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalService;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServiceCategory;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.GroupTranslations;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.ServiceConstraintType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.TranslationType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the additional-services read facade: definition-catalog,
 * group list + single read (with nested services/configurations), and group
 * translations mapping, plus a 404. Live read verification is done by the
 * {@code settings-additional-services} demo on the seller sandbox.
 */
@WireMockTest
class AdditionalServicesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String CATEGORIES_PATH = "/sale/offer-additional-services/categories";
    private static final String GROUPS_PATH = "/sale/offer-additional-services/groups";
    private static final String GROUP_ID = "gr-1";
    private static final String GROUP_PATH = GROUPS_PATH + "/" + GROUP_ID;
    private static final String TRANSLATIONS_PATH = GROUP_PATH + "/translations";

    private static final String PARAM_OFFSET = "offset";
    private static final String OFFSET_PAGE_0 = "0";
    private static final String DEFINITION_ID = "GIFT_WRAP";
    private static final String GROUP_NAME = "Gift wrap only";
    private static final String LANGUAGE = "pl-PL";
    private static final String DESCRIPTION = "Wrap product in nice paper";
    private static final String AMOUNT = "5.00";
    private static final String CURRENCY = "PLN";
    private static final String TRACE_ID = "4631702648f0524e";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String CATEGORIES_RESPONSE = """
            {"categories":[{"name":"Wrapping","definitions":[
              {"id":"%s","name":"Gift wrap","maxPrice":{"amount":"%s","currency":"%s"}}]}]}
            """.formatted(DEFINITION_ID, AMOUNT, CURRENCY);
    private static final String GROUP_RESPONSE = """
            {"id":"%s","name":"%s","language":"%s","managedByAllegro":false,
             "seller":{"id":"111332841"},
             "additionalServices":[{"definition":{"id":"%s"},"description":"%s",
               "configurations":[{"constraintCriteria":{"country":"PL","type":"COUNTRY_SAME_QUANTITY"},
                 "price":{"amount":"%s","currency":"%s"}}]}]}
            """.formatted(GROUP_ID, GROUP_NAME, LANGUAGE, DEFINITION_ID, DESCRIPTION, AMOUNT, CURRENCY);
    private static final String TRANSLATIONS_RESPONSE = """
            {"translations":[{"language":"%s","additionalServices":{"type":"MANUAL",
              "translation":[{"definition":{"id":"%s"},"description":"%s"}]}}]}
            """.formatted(LANGUAGE, DEFINITION_ID, DESCRIPTION);

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
    void categoryDefinitions_whenFetched_mapsCategoriesAndDefinitions(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(CATEGORIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CATEGORIES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            List<AdditionalServiceCategory> categories =
                    allegro.settings().additionalServices().categoryDefinitions();

            assertEquals(1, categories.size());
            assertEquals("Wrapping", categories.get(0).name());
            assertEquals(1, categories.get(0).definitions().size());
            assertEquals(DEFINITION_ID, categories.get(0).definitions().get(0).id());
            assertNotNull(categories.get(0).definitions().get(0).maxPrice());
            assertEquals(AMOUNT, categories.get(0).definitions().get(0).maxPrice().amount());
        }
    }

    @Test
    void streamGroups_whenPage_mapsGroups(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(GROUPS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"additionalServicesGroups\":[" + GROUP_RESPONSE + "]}")));

        try (AllegroClient allegro = client(wmInfo)) {
            List<AdditionalServicesGroup> groups = allegro.settings().additionalServices()
                    .streamGroups().toList();

            assertEquals(1, groups.size());
            assertEquals(GROUP_ID, groups.get(0).id());
            assertEquals(GROUP_NAME, groups.get(0).name());
            assertEquals(LANGUAGE, groups.get(0).language());
        }
    }

    @Test
    void streamGroups_whenNotConsumed_defersTheFetch(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(GROUPS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"additionalServicesGroups\":[]}")));

        try (AllegroClient allegro = client(wmInfo)) {
            var stream = allegro.settings().additionalServices().streamGroups();
            verify(0, getRequestedFor(urlPathEqualTo(GROUPS_PATH)));
            stream.findFirst();
            verify(1, getRequestedFor(urlPathEqualTo(GROUPS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
        }
    }

    @Test
    void group_whenFound_mapsNestedServicesAndConfigurations(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(GROUP_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(GROUP_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdditionalServicesGroup group = allegro.settings().additionalServices().group(GROUP_ID);

            assertEquals(GROUP_ID, group.id());
            assertEquals(1, group.services().size());
            AdditionalService service = group.services().get(0);
            assertEquals(DEFINITION_ID, service.definitionId());
            assertEquals(DESCRIPTION, service.description());
            assertEquals(1, service.configurations().size());
            assertNotNull(service.configurations().get(0).constraint());
            assertEquals("PL", service.configurations().get(0).constraint().country());
            assertEquals(ServiceConstraintType.COUNTRY_SAME_QUANTITY,
                    service.configurations().get(0).constraint().type());
            assertNotNull(service.configurations().get(0).price());
            assertEquals(AMOUNT, service.configurations().get(0).price().amount());
            verify(1, getRequestedFor(urlEqualTo(GROUP_PATH)));
        }
    }

    @Test
    void translations_whenFound_mapsLanguageTypeAndServiceTranslations(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(TRANSLATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TRANSLATIONS_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            GroupTranslations translations = allegro.settings().additionalServices().translations(GROUP_ID);

            assertEquals(1, translations.translations().size());
            assertEquals(LANGUAGE, translations.translations().get(0).language());
            assertEquals(TranslationType.MANUAL, translations.translations().get(0).type());
            assertEquals(1, translations.translations().get(0).services().size());
            assertEquals(DEFINITION_ID, translations.translations().get(0).services().get(0).definitionId());
            assertEquals(DESCRIPTION, translations.translations().get(0).services().get(0).description());
            verify(1, getRequestedFor(urlEqualTo(TRANSLATIONS_PATH)));
        }
    }

    @Test
    void group_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(GROUP_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var additionalServices = allegro.settings().additionalServices();
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> additionalServices.group(GROUP_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TRACE_ID, failure.traceId());
        }
    }
}
