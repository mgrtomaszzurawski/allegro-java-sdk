/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.AdditionalEmail;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facade test for {@code client.user().additionalEmails()} — list/get/add/delete
 * with write-body verification and typed error mapping.
 */
@WireMockTest
class AdditionalEmailsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String EMAILS_PATH = "/account/additional-emails";
    private static final String EMAIL_ID = "8fe19442-d0c0-4a84-873f-42fc40a7bcf0";
    private static final String EMAIL_BY_ID_PATH = EMAILS_PATH + "/" + EMAIL_ID;
    private static final String EMAIL_ADDRESS = "extra@example.com";
    private static final String CREATED_AT = "2011-12-03T10:15:30Z";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String EMAIL_JSON = """
            {"id":"%s","email":"%s","createdAt":"%s"}
            """.formatted(EMAIL_ID, EMAIL_ADDRESS, CREATED_AT);
    private static final String EMAIL_LIST_JSON = """
            {"additional-emails":[{"id":"%s","email":"%s","createdAt":"%s"}]}
            """.formatted(EMAIL_ID, EMAIL_ADDRESS, CREATED_AT);
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String VALIDATION_JSON = """
            {"errors":[{"code":"ValidationException","message":"invalid email",
              "userMessage":"Niepoprawny adres","path":"email"}]}
            """;
    private static final String NOT_FOUND_JSON = """
            {"errors":[{"code":"NotFound","message":"not found","userMessage":"Brak","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    @Test
    void list_whenEmailsReturned_mapsEntries(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(EMAILS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EMAIL_LIST_JSON)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AdditionalEmail> emails = allegro.user().additionalEmails().list();

            // then
            assertEquals(1, emails.size());
            assertEquals(EMAIL_ID, emails.get(0).id());
            assertEquals(EMAIL_ADDRESS, emails.get(0).email());
            assertEquals(CREATED_AT, emails.get(0).createdAt());
        }
    }

    @Test
    void get_whenEmailExists_mapsEntry(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(EMAIL_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(EMAIL_JSON)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AdditionalEmail email = allegro.user().additionalEmails().get(EMAIL_ID);

            // then
            assertEquals(EMAIL_ADDRESS, email.email());
        }
    }

    @Test
    void add_whenValidAddress_postsEmailBodyAndMapsResult(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(EMAILS_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(containing("\"email\":\"" + EMAIL_ADDRESS + "\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(EMAIL_JSON)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AdditionalEmail created = allegro.user().additionalEmails().add(EMAIL_ADDRESS);

            // then — the address was sent in the body and the created entry mapped back
            assertEquals(EMAIL_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(EMAILS_PATH))
                    .withRequestBody(containing("\"email\":\"" + EMAIL_ADDRESS + "\"")));
        }
    }

    @Test
    void delete_whenEmailExists_sendsDeleteRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(EMAIL_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.user().additionalEmails().delete(EMAIL_ID);

            // then
            verify(1, deleteRequestedFor(urlEqualTo(EMAIL_BY_ID_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void add_when400_throwsBadRequestWithParsedFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(EMAILS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(VALIDATION_JSON)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdditionalEmails emails = allegro.user().additionalEmails();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> emails.add(EMAIL_ADDRESS));
            assertEquals("email", failure.errors().get(0).path());
        }
    }

    @Test
    void get_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(EMAIL_BY_ID_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_JSON)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdditionalEmails emails = allegro.user().additionalEmails();

            // then
            assertThrows(AllegroNotFoundException.class, () -> emails.get(EMAIL_ID));
        }
    }
}
