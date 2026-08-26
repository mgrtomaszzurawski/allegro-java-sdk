/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the after-sale warranty-document attachment upload: the SDK
 * declares the attachment (POST) then PUTs the file bytes to the returned id, and
 * maps the response to {@link AfterSalesAttachment}.
 *
 * <p>Fixtures are {@code spec-derived} (a live upload is tracked in the E2E debt ledger).
 */
@WireMockTest
class AfterSaleAttachmentClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String ATTACHMENTS_PATH = "/after-sales-service-conditions/attachments";
    private static final String ATTACHMENT_ID = "11111111-1111-4111-8111-111111111111";
    private static final String ATTACHMENT_PATH = ATTACHMENTS_PATH + "/" + ATTACHMENT_ID;
    private static final String ATTACHMENT_NAME = "manual.pdf";
    private static final String ATTACHMENT_URL = "https://cdn.example/manual.pdf";
    private static final String CONTENT_TYPE = "application/pdf";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","token_type":"bearer","expires_in":%d,"scope":"sale:settings:write"}
            """;

    private static final String DECLARE_RESPONSE = """
            {"id":"%s"}
            """.formatted(ATTACHMENT_ID);

    private static final String UPLOAD_RESPONSE = """
            {"id":"%s","name":"%s","url":"%s"}
            """.formatted(ATTACHMENT_ID, ATTACHMENT_NAME, ATTACHMENT_URL);

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
    void uploadAttachment_whenDeclaredAndUploaded_mapsAttachmentAndPutsBytesToId(WireMockRuntimeInfo wmInfo) {
        // given — declare returns an id, upload returns the hosted attachment
        stubToken();
        stubFor(post(urlPathEqualTo(ATTACHMENTS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(DECLARE_RESPONSE)));
        stubFor(put(urlPathEqualTo(ATTACHMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(UPLOAD_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            byte[] pdf = "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8);
            AfterSalesAttachment uploaded =
                    allegro.settings().afterSale().uploadAttachment(ATTACHMENT_NAME, pdf, CONTENT_TYPE);

            // then — response mapped
            assertEquals(ATTACHMENT_ID, uploaded.id());
            assertEquals(ATTACHMENT_NAME, uploaded.name());
            assertEquals(ATTACHMENT_URL, uploaded.url());

            // and the two-step upload hit declare (carrying the file name) then the
            // id-scoped PUT with the file bytes
            verify(1, postRequestedFor(urlPathEqualTo(ATTACHMENTS_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(ATTACHMENT_NAME))));
            verify(1, putRequestedFor(urlPathEqualTo(ATTACHMENT_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(CONTENT_TYPE)));
        }
    }
}
