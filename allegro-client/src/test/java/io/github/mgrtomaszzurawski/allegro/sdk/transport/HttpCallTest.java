/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Etagged;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
class HttpCallTest {

    private static final String TEST_TOKEN = "call-test-token";
    private static final String PATH = "/sale/target";
    private static final String OK_BODY = "{\"value\":\"ok\"}";
    private static final String OK_VALUE = "ok";
    private static final String OPERATION = "exercise call";
    private static final String LANGUAGE = "pl-PL";
    private static final String ETAG = "\"v3\"";
    private static final String IMAGE_CONTENT_TYPE = "image/png";
    private static final byte[] IMAGE_BYTES = {1, 2, 3, 4};
    private static final String PDF_MEDIA_TYPE = "application/pdf";
    private static final byte[] PDF_BYTES = {37, 80, 68, 70, 45, 49, 46, 52};
    private static final String ETAG_HEADER = "ETag";
    private static final String ETAG_VALUE = "\"rev-42\"";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"no such attachment\"}]}";

    private static HttpSupport support(WireMockRuntimeInfo wmInfo) {
        var mapper = new ObjectMapper();
        var retryHandler = new RetryHandler(HttpClient.newHttpClient(),
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
                // no-op: 401 replay covered in AllegroClientMeTest
            }
        };
        return new HttpSupport(runtime);
    }

    @Test
    void get_whenQueryParams_appendsEncodedQueryString(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH + "?name=red%20shoes&limit=20"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));
        Query query = Query.create().add("name", "red shoes").add("limit", 20);

        // when
        Map<?, ?> mapped = support(wmInfo).request(OPERATION).get(PATH).query(query).fetch(Map.class);

        // then
        assertEquals(OK_VALUE, mapped.get("value"));
        verify(1, getRequestedFor(urlEqualTo(PATH + "?name=red%20shoes&limit=20")));
    }

    @Test
    void patch_whenJsonBody_sendsPatchVerbWithVendorContentType(WireMockRuntimeInfo wmInfo) {
        // given — the JDK client has no .PATCH() shortcut; HttpCall uses method("PATCH", ...)
        stubFor(patch(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(containing("\"language\":\"en-US\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        Map<?, ?> mapped = support(wmInfo).request(OPERATION).patch(PATH)
                .jsonBody(Map.of("language", "en-US")).fetch(Map.class);

        // then
        assertEquals(OK_VALUE, mapped.get("value"));
        verify(1, patchRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void send_whenNoContentWrite_verifiesCallWithoutResponseBody(WireMockRuntimeInfo wmInfo) {
        // given — a 204 write (tag rename / assign) that returns nothing
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        // when
        support(wmInfo).request(OPERATION).post(PATH).jsonBody(Map.of("k", "v")).send();

        // then
        verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void acceptBeta_whenSet_sendsBetaVendorMediaType(WireMockRuntimeInfo wmInfo) {
        // given — charity/affiliate/bulk endpoints require the beta media type
        stubFor(get(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).get(PATH).acceptBeta().fetch(Map.class);

        // then
        verify(1, getRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1)));
    }

    @Test
    void binaryBody_whenUpload_sendsRawBytesWithCallerContentType(WireMockRuntimeInfo wmInfo) {
        // given — image upload PUTs bytes, not vendor JSON
        stubFor(put(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(IMAGE_CONTENT_TYPE))
                .withRequestBody(binaryEqualTo(IMAGE_BYTES))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        Map<?, ?> mapped = support(wmInfo).request(OPERATION).put(PATH)
                .binaryBody(IMAGE_BYTES, IMAGE_CONTENT_TYPE).fetch(Map.class);

        // then
        assertEquals(OK_VALUE, mapped.get("value"));
        verify(1, putRequestedFor(urlEqualTo(PATH))
                .withRequestBody(binaryEqualTo(IMAGE_BYTES)));
    }

    @Test
    void acceptLanguage_whenSet_sendsAcceptLanguageHeader(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlPathEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).get(PATH).acceptLanguage(LANGUAGE).fetch(Map.class);

        // then
        verify(1, getRequestedFor(urlPathEqualTo(PATH))
                .withHeader(TestHttpConstants.ACCEPT_LANGUAGE_HEADER, equalTo(LANGUAGE)));
    }

    @Test
    void ifMatch_whenConditionalWrite_sendsIfMatchHeader(WireMockRuntimeInfo wmInfo) {
        // given — optimistic-concurrency write guarded by the resource ETag
        stubFor(put(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).put(PATH).ifMatch(ETAG)
                .jsonBody(Map.of("k", "v")).fetch(Map.class);

        // then
        verify(1, putRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.IF_MATCH_HEADER, equalTo(ETAG)));
    }

    @Test
    void fetchBytes_whenBinaryResponse_returnsRawBytesWithAcceptHeader(WireMockRuntimeInfo wmInfo) {
        // given — an attachment/PDF download returns bytes, not vendor JSON
        stubFor(get(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(PDF_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PDF_BYTES)));

        // when
        byte[] downloaded = support(wmInfo).request(OPERATION).get(PATH)
                .accept(PDF_MEDIA_TYPE).fetchBytes();

        // then — exact bytes returned, negotiated media type on the wire
        assertArrayEquals(PDF_BYTES, downloaded);
        verify(1, getRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(PDF_MEDIA_TYPE)));
    }

    @Test
    void fetchBytes_whenErrorStatus_mapsVendorJsonErrorToTypedException(WireMockRuntimeInfo wmInfo) {
        // given — the success body would be bytes, but a 404 carries the vendor
        // JSON error, which the binary path must still decode and map
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class,
                () -> support(wmInfo).request(OPERATION).get(PATH).accept(PDF_MEDIA_TYPE).fetchBytes());
    }

    @Test
    void fetchWithETag_whenResponseCarriesEtag_returnsValueAndEtag(WireMockRuntimeInfo wmInfo) {
        // given — a resource read whose ETag guards a later conditional write
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(ETAG_HEADER, ETAG_VALUE).withBody(OK_BODY)));

        // when
        Etagged<Map> result = support(wmInfo).request(OPERATION).get(PATH).fetchWithETag(Map.class);

        // then — both the deserialized body and the ETag are captured
        assertEquals(OK_VALUE, result.value().get("value"));
        assertEquals(ETAG_VALUE, result.etag());
    }

    @Test
    void jsonBody_whenPayloadHasDiacritics_survivesAsUtf8OnTheWire(WireMockRuntimeInfo wmInfo) {
        // given — a field with real Polish diacritics (ł, ą, ę, ó) that only
        // round-trip correctly if the request body is UTF-8 encoded end to end.
        // WireMock decodes the received body as UTF-8, so a match on the exact
        // string proves the bytes on the wire were UTF-8, not a mangled charset.
        String diacritics = "Cena zbyt niska — wartość ≥ 10 zł";
        stubFor(post(urlEqualTo(PATH))
                .withRequestBody(containing(diacritics))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).post(PATH).jsonBody(Map.of("msg", diacritics)).send();

        // then — the wire saw the diacritics intact (would fail on ISO-8859-1)
        verify(1, postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(containing(diacritics)));
    }
}
