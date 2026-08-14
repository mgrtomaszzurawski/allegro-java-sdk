/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.AdditionalServiceRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.AdditionalServicesGroupRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.GroupTranslationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.ServiceConfigurationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder.ServiceConstraintRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.AdditionalServicesGroup;
import java.util.List;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the additional-services group WRITE surface: create (POST)
 * and update (PUT) send the group name + service definition id + configuration price
 * and map the response to {@link AdditionalServicesGroup}; the per-language
 * translation upsert (PATCH) and delete send/verify their requests; the builders
 * reject an empty group / missing name fail-fast.
 *
 * <p>Fixtures are {@code spec-derived} (write→read tracked in the E2E debt ledger).
 */
@WireMockTest
class AdditionalServicesWriteClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String GROUPS_PATH = "/sale/offer-additional-services/groups";
    private static final String GROUP_ID = "gr-1";
    private static final String GROUP_PATH = GROUPS_PATH + "/" + GROUP_ID;
    private static final String LANGUAGE = "pl-PL";
    private static final String TRANSLATION_PATH = GROUP_PATH + "/translations/" + LANGUAGE;

    private static final String DEFINITION_ID = "GIFT_WRAP";
    private static final String CARRY_IN_DEFINITION_ID = "CARRY_IN";
    private static final String GROUP_NAME = "Gift wrap only";
    private static final String DESCRIPTION = "Wrap product in nice paper";
    private static final String AMOUNT = "5.00";
    private static final String CURRENCY = "PLN";
    private static final String CONSTRAINT_COUNTRY = "PL";
    private static final String CONSTRAINT_TYPE_BEFORE_SHIPPING = "COUNTRY_SAME_QUANTITY";
    private static final String CONSTRAINT_TYPE_IN_DELIVERY = "COUNTRY_DELIVERY_SAME_QUANTITY";
    private static final String DELIVERY_METHOD_ID = "6d5f38c3-e05c-4a5e-b8da-1234567890ab";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","token_type":"bearer","expires_in":%d,"scope":"sale:settings:write"}
            """;

    private static final String GROUP_RESPONSE = """
            {"id":"%s","name":"%s","language":"%s","managedByAllegro":false,
             "seller":{"id":"111332841"},
             "additionalServices":[{"definition":{"id":"%s"},"description":"%s",
               "configurations":[{"constraintCriteria":{"country":"PL","type":"COUNTRY_SAME_QUANTITY"},
                 "price":{"amount":"%s","currency":"%s"}}]}]}
            """.formatted(GROUP_ID, GROUP_NAME, LANGUAGE, DEFINITION_ID, DESCRIPTION, AMOUNT, CURRENCY);

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

    private static AdditionalServicesGroupRequest sampleGroup() {
        return AdditionalServicesGroupRequest.builder()
                .name(GROUP_NAME)
                .language(LANGUAGE)
                .addService(AdditionalServiceRequest.of(DEFINITION_ID, DESCRIPTION,
                        ServiceConfigurationRequest.of(Money.of(AMOUNT, CURRENCY),
                                ServiceConstraintRequest.beforeShipping(CONSTRAINT_COUNTRY))))
                .build();
    }

    @Test
    void createGroup_whenAccepted_mapsGroupAndSendsNameServicePrice(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlPathEqualTo(GROUPS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(GROUP_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdditionalServicesGroup group = allegro.settings().additionalServices().createGroup(sampleGroup());

            // then
            assertEquals(GROUP_ID, group.id());
            assertEquals(GROUP_NAME, group.name());
            verify(1, postRequestedFor(urlPathEqualTo(GROUPS_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(GROUP_NAME)))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].definition.id", equalTo(DEFINITION_ID)))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].configurations[0].price.amount", equalTo(AMOUNT)))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].configurations[0].constraintCriteria.country",
                            equalTo(CONSTRAINT_COUNTRY)))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].configurations[0].constraintCriteria.type",
                            equalTo(CONSTRAINT_TYPE_BEFORE_SHIPPING))));
        }
    }

    @Test
    void createGroup_whenInDeliveryConstraint_sendsTypeAndDeliveryMethods(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlPathEqualTo(GROUPS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(GROUP_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.settings().additionalServices().createGroup(
                    AdditionalServicesGroupRequest.builder()
                            .name(GROUP_NAME)
                            .language(LANGUAGE)
                            .addService(AdditionalServiceRequest.of(CARRY_IN_DEFINITION_ID, DESCRIPTION,
                                    ServiceConfigurationRequest.of(Money.of(AMOUNT, CURRENCY),
                                            ServiceConstraintRequest.inDelivery(
                                                    CONSTRAINT_COUNTRY, List.of(DELIVERY_METHOD_ID)))))
                            .build());

            // then
            verify(1, postRequestedFor(urlPathEqualTo(GROUPS_PATH))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].configurations[0].constraintCriteria.type",
                            equalTo(CONSTRAINT_TYPE_IN_DELIVERY)))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices[0].configurations[0].constraintCriteria.deliveryMethods[0].id",
                            equalTo(DELIVERY_METHOD_ID))));
        }
    }

    @Test
    void updateGroup_whenAccepted_putsGroupById(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(put(urlPathEqualTo(GROUP_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(GROUP_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdditionalServicesGroup group =
                    allegro.settings().additionalServices().updateGroup(GROUP_ID, sampleGroup());

            // then
            assertEquals(GROUP_ID, group.id());
            verify(1, putRequestedFor(urlPathEqualTo(GROUP_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(GROUP_NAME))));
        }
    }

    @Test
    void upsertTranslation_whenAccepted_patchesLanguageWithDescriptions(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(patch(urlPathEqualTo(TRANSLATION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.settings().additionalServices().upsertTranslation(GROUP_ID, LANGUAGE,
                    GroupTranslationRequest.builder()
                            .addTranslation(DEFINITION_ID, DESCRIPTION)
                            .build());

            // then
            verify(1, patchRequestedFor(urlPathEqualTo(TRANSLATION_PATH))
                    .withRequestBody(matchingJsonPath(
                            "$.additionalServices.translation[0].description", equalTo(DESCRIPTION))));
        }
    }

    @Test
    void deleteTranslation_whenAccepted_deletesLanguage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(delete(urlPathEqualTo(TRANSLATION_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.settings().additionalServices().deleteTranslation(GROUP_ID, LANGUAGE);

            // then
            verify(1, deleteRequestedFor(urlPathEqualTo(TRANSLATION_PATH)));
        }
    }

    @Test
    void groupBuilder_whenNameBlank_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> AdditionalServicesGroupRequest.builder()
                        .addService(AdditionalServiceRequest.of(DEFINITION_ID)).build());
    }

    @Test
    void groupBuilder_whenNoService_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> AdditionalServicesGroupRequest.builder().name(GROUP_NAME).build());
    }

    @Test
    void translationBuilder_whenEmpty_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> GroupTranslationRequest.builder().build());
    }
}
