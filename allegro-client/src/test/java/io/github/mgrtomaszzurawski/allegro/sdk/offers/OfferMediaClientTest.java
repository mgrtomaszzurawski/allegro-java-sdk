/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ImageFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferImage;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.OfferMediaImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferMediaClientTest {

    private static final String TEST_TOKEN = "media-test-token";
    private static final String IMAGES_PATH = "/sale/images";
    private static final String ATTACHMENTS_PATH = "/sale/offer-attachments";
    private static final String ATTACHMENT_ID = "att-12345";
    private static final String ATTACHMENT_PATH = ATTACHMENTS_PATH + "/" + ATTACHMENT_ID;
    private static final byte[] IMAGE_BYTES = "ÿØÿ-fake-jpeg".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF_BYTES = "%PDF-1.4-fake".getBytes(StandardCharsets.UTF_8);
    private static final String IMAGE_LOCATION = "https://a.allegroimg.com/original/abc/keyboard.jpg";
    private static final String SOURCE_IMAGE_URL = "https://img.example/keyboard.jpg";
    private static final String EXPIRES_AT = "2026-08-01T10:00:00Z";
    private static final String FILE_NAME = "manual.pdf";
    private static final String FILE_URL = "https://a.allegroimg.com/original/att/manual.pdf";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private static final String IMAGE_RESPONSE = ("{\"location\":\"%s\",\"expiresAt\":\"%s\"}")
            .formatted(IMAGE_LOCATION, EXPIRES_AT);
    private static final String ATTACHMENT_RESPONSE = ("{\"id\":\"%s\",\"type\":\"USER_MANUAL\","
            + "\"file\":{\"name\":\"%s\",\"url\":\"%s\"}}").formatted(ATTACHMENT_ID, FILE_NAME, FILE_URL);
    private static final String ATTACHMENT_DECLARED = ("{\"id\":\"%s\",\"type\":\"USER_MANUAL\","
            + "\"file\":{\"name\":\"%s\"}}").formatted(ATTACHMENT_ID, FILE_NAME);
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"attachment not found\"}]}";
    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"INVALID_FILE\",\"message\":\"bad file\",\"path\":\"file.name\"}]}";
    private static final String URL_JSON_PATH = "$.url";
    private static final String TYPE_JSON_PATH = "$.type";
    private static final String FILE_NAME_JSON_PATH = "$.file.name";

    private static OfferMediaImpl media(WireMockRuntimeInfo wmInfo) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RetryHandler retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        HttpRuntime runtime = new HttpRuntime() {
            @Override public String baseUrl() {
                return wmInfo.getHttpBaseUrl();
            }

            @Override public RetryHandler retryHandler() {
                return retryHandler;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return AllegroExecutionInterceptor.noop();
            }

            @Override public ObjectMapper objectMapper() {
                return mapper;
            }

            @Override public Duration readTimeout() {
                return Duration.ofSeconds(5);
            }

            @Override public String requireToken() {
                return TEST_TOKEN;
            }

            @Override public void reauthenticate() {
                // no-op: 401 replay is covered in the transport suite
            }
        };
        return new OfferMediaImpl(runtime);
    }

    @Test
    void uploadImage_whenBinary_postsBytesWithFormatContentType(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(IMAGES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(IMAGE_RESPONSE)));

        // when
        OfferImage image = media(wmInfo).uploadImage(IMAGE_BYTES, ImageFormat.JPEG);

        // then — the bytes go up with the image content type; the hosted URL comes back
        verify(1, postRequestedFor(urlEqualTo(IMAGES_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(CONTENT_TYPE_JPEG))
                .withRequestBody(binaryEqualTo(IMAGE_BYTES)));
        assertEquals(IMAGE_LOCATION, image.location());
    }

    @Test
    void uploadImage_whenByUrl_postsJsonUrlBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(IMAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(IMAGE_RESPONSE)));

        // when
        OfferImage image = media(wmInfo).uploadImage(SOURCE_IMAGE_URL);

        // then
        verify(1, postRequestedFor(urlEqualTo(IMAGES_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(URL_JSON_PATH, equalTo(SOURCE_IMAGE_URL))));
        assertEquals(IMAGE_LOCATION, image.location());
    }

    @Test
    void createAttachment_declaresTypeAndFileName(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ATTACHMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(ATTACHMENT_DECLARED)));

        // when
        OfferAttachment attachment = media(wmInfo)
                .createAttachment(AttachmentDeclaration.of(AttachmentType.USER_MANUAL, FILE_NAME));

        // then — the declaration carries the type + file name; the id comes back to upload to
        verify(1, postRequestedFor(urlEqualTo(ATTACHMENTS_PATH))
                .withRequestBody(matchingJsonPath(TYPE_JSON_PATH, equalTo("USER_MANUAL")))
                .withRequestBody(matchingJsonPath(FILE_NAME_JSON_PATH, equalTo(FILE_NAME))));
        assertEquals(ATTACHMENT_ID, attachment.id());
        assertEquals(AttachmentType.USER_MANUAL, attachment.type());
        assertEquals(FILE_NAME, attachment.fileName());
    }

    @Test
    void uploadAttachment_putsBytesToTheAttachmentId(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(ATTACHMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.VND_ALLEGRO_V1)
                        .withBody(ATTACHMENT_RESPONSE)));

        // when
        OfferAttachment attachment = media(wmInfo).uploadAttachment(ATTACHMENT_ID, PDF_BYTES);

        // then — the file bytes go up as application/pdf; the hosted URL comes back
        verify(1, putRequestedFor(urlEqualTo(ATTACHMENT_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(CONTENT_TYPE_PDF))
                .withRequestBody(binaryEqualTo(PDF_BYTES)));
        assertEquals(FILE_URL, attachment.fileUrl());
    }

    @Test
    void getAttachment_whenExists_returnsMapped(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(ATTACHMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ATTACHMENT_RESPONSE)));

        // when
        OfferAttachment attachment = media(wmInfo).getAttachment(ATTACHMENT_ID);

        // then
        verify(1, getRequestedFor(urlEqualTo(ATTACHMENT_PATH)));
        assertEquals(ATTACHMENT_ID, attachment.id());
        assertEquals(AttachmentType.USER_MANUAL, attachment.type());
        assertEquals(FILE_URL, attachment.fileUrl());
    }

    @Test
    void getAttachment_whenMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(ATTACHMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class, () -> media(wmInfo).getAttachment(ATTACHMENT_ID));
    }

    @Test
    void createAttachment_whenServerRejects_throwsBadRequestWithFieldError(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(ATTACHMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(BAD_REQUEST_BODY)));

        // then
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> media(wmInfo).createAttachment(
                        AttachmentDeclaration.of(AttachmentType.USER_MANUAL, FILE_NAME)));
        assertEquals(1, failure.errors().size());
        assertEquals("file.name", failure.errors().get(0).path());
    }
}
