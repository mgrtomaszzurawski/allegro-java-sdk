/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.builder.SizeTableRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTable;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.sizetables.model.SizeTableTemplate;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the size-tables facade: list/get/templates reads, the
 * create ({@code POST}, template-bearing) and update ({@code PUT}, template-less)
 * writes, the vendor media type on the wire, and the error-path table (404/400).
 *
 * <p>Response bodies are spec-derived; the live write→read round-trip is proved
 * by the {@code settings-size-tables} demo before this PR is merge-ready.
 */
@WireMockTest
class SizeTablesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String TABLES_PATH = "/sale/size-tables";
    private static final String TEMPLATES_PATH = "/sale/size-tables-templates";
    private static final String TABLE_ID = "e02bc505-4311-11e8-b7fd-6c3be5b54a70";
    private static final String TABLE_PATH = TABLES_PATH + "/" + TABLE_ID;
    private static final String TEMPLATE_ID = "5637592a-0a24-4771-b527-d89b2767d821";

    private static final String NAME = "Nike shoes size table";
    private static final String HEADER_SIZE = "Size";
    private static final String HEADER_LENGTH = "Length";
    private static final String CELL_SIZE = "M";
    private static final String CELL_LENGTH = "38";
    private static final String UPDATED_NAME = "Nike shoes size table v2";
    private static final String IMAGE_URL = "https://a.allegroimg.com/original/aa/table";
    private static final String CAPTION_VALUE = "chest";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String TABLE_RESPONSE = """
            {"id":"%s","name":"%s","template":{"id":"%s"},
             "headers":[{"name":"%s"},{"name":"%s"}],
             "values":[{"cells":["%s","%s"]}]}
            """.formatted(TABLE_ID, NAME, TEMPLATE_ID, HEADER_SIZE, HEADER_LENGTH, CELL_SIZE, CELL_LENGTH);
    private static final String TABLES_RESPONSE = "{\"tables\":[" + TABLE_RESPONSE + "]}";
    private static final String TEMPLATES_RESPONSE = """
            {"templates":[{"id":"%s","name":"%s",
              "image":{"url":"%s","captions":[{"index":"1","value":"%s"}]},
              "headers":[{"name":"%s"}],
              "values":[{"cells":["%s"]}]}]}
            """.formatted(TEMPLATE_ID, NAME, IMAGE_URL, CAPTION_VALUE, HEADER_SIZE, CELL_SIZE);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"table not found","path":"tableId"}]}
            """;
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationException","message":"headers must not be empty",
              "userMessage":"Nagłówki są wymagane","path":"headers"}]}
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

    private static SizeTableRequest sampleRequest() {
        return SizeTableRequest.builder()
                .name(NAME)
                .templateId(TEMPLATE_ID)
                .header(HEADER_SIZE)
                .header(HEADER_LENGTH)
                .row(List.of(CELL_SIZE, CELL_LENGTH))
                .build();
    }

    // ---- reads ----

    @Test
    void list_whenTablesReturned_mapsHeadersAndRows(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(TABLES_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TABLES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            List<SizeTable> tables = allegro.settings().sizeTables().list();

            assertEquals(1, tables.size());
            SizeTable table = tables.get(0);
            assertEquals(TABLE_ID, table.id());
            assertEquals(NAME, table.name());
            assertEquals(TEMPLATE_ID, table.templateId());
            assertEquals(List.of(HEADER_SIZE, HEADER_LENGTH), table.headers());
            assertEquals(List.of(CELL_SIZE, CELL_LENGTH), table.rows().get(0).cells());
        }
    }

    @Test
    void get_whenTableExists_mapsTable(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(TABLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TABLE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            SizeTable table = allegro.settings().sizeTables().get(TABLE_ID);

            assertEquals(TABLE_ID, table.id());
            assertEquals(2, table.headers().size());
        }
    }

    @Test
    void templates_whenReturned_mapsImageAndCaptions(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(TEMPLATES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TEMPLATES_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            List<SizeTableTemplate> templates = allegro.settings().sizeTables().templates();

            assertEquals(1, templates.size());
            SizeTableTemplate template = templates.get(0);
            assertEquals(TEMPLATE_ID, template.id());
            assertEquals(1, template.headers().size());
            assertEquals(IMAGE_URL, template.image().url());
            assertEquals(CAPTION_VALUE, template.image().captions().get(0).value());
        }
    }

    // ---- writes ----

    @Test
    void create_whenValidRequest_postsTemplateBodyAndMaps(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(TABLES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(TABLE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            SizeTable created = allegro.settings().sizeTables().create(sampleRequest());

            assertEquals(TABLE_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(TABLES_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.template.id", equalTo(TEMPLATE_ID)))
                    .withRequestBody(matchingJsonPath("$.headers[0].name", equalTo(HEADER_SIZE)))
                    .withRequestBody(matchingJsonPath("$.values[0].cells[0]", equalTo(CELL_SIZE))));
        }
    }

    @Test
    void create_whenTemplateIdMissing_throwsBeforeCall(WireMockRuntimeInfo wmInfo) {
        stubToken();
        SizeTableRequest noTemplate = SizeTableRequest.builder()
                .name(NAME).header(HEADER_SIZE).row(List.of(CELL_SIZE)).build();

        try (AllegroClient allegro = client(wmInfo)) {
            assertThrows(IllegalArgumentException.class,
                    () -> allegro.settings().sizeTables().create(noTemplate));
            verify(0, postRequestedFor(urlEqualTo(TABLES_PATH)));
        }
    }

    @Test
    void update_whenValidRequest_putsBodyWithoutTemplate(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(put(urlEqualTo(TABLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TABLE_RESPONSE)));
        SizeTableRequest request = sampleRequest().toBuilder().name(UPDATED_NAME).build();

        try (AllegroClient allegro = client(wmInfo)) {
            SizeTable updated = allegro.settings().sizeTables().update(TABLE_ID, request);

            assertEquals(TABLE_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(TABLE_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(UPDATED_NAME)))
                    .withRequestBody(matchingJsonPath("$.headers[0].name", equalTo(HEADER_SIZE)))
                    .withRequestBody(notMatching("(?s).*template.*")));
        }
    }

    // ---- error table ----

    @Test
    void get_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(TABLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            assertThrows(AllegroNotFoundException.class,
                    () -> allegro.settings().sizeTables().get(TABLE_ID));
        }
    }

    @Test
    void create_whenBadRequest_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(TABLES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            assertThrows(AllegroBadRequestException.class,
                    () -> allegro.settings().sizeTables().create(sampleRequest()));
        }
    }
}
