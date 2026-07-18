/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Etagged;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpCall;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Located;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
class HttpCallTest {

    private static final String TEST_TOKEN = "call-test-token";
    private static final String PATH = "/sale/target";
    private static final String OK_BODY = "{\"value\":\"ok\"}";
    private static final String OK_VALUE = "ok";
    private static final String OPERATION = "exercise call";
    private static final String PRESENT_VALUE = "here";
    private static final String LANGUAGE = "pl-PL";
    private static final String ETAG = "\"v3\"";
    private static final String IMAGE_CONTENT_TYPE = "image/png";
    private static final byte[] IMAGE_BYTES = {1, 2, 3, 4};
    private static final String LOCATION_HEADER = "Location";
    private static final String UPLOAD_PATH = "/sale/offer-attachments/abc-123";
    private static final String UPLOAD_URL = "http://upload.allegro.pl/sale/offer-attachments/abc-123";
    private static final String UPLOAD_URL_HTTPS = "https://upload.allegro.pl/sale/offer-attachments/abc-123";
    private static final String API_BASE = "https://api.allegro.pl";
    private static final String SANDBOX_BASE = "https://api.allegro.pl.allegrosandbox.pl";
    private static final String SANDBOX_UPLOAD_HTTP = "http://upload.allegro.pl.allegrosandbox.pl/x";
    private static final String SANDBOX_UPLOAD_HTTPS = "https://upload.allegro.pl.allegrosandbox.pl/x";
    private static final String FOREIGN_UPLOAD = "http://upload.evil.example.com/steal";
    private static final String LOCAL_BASE = "http://localhost:8080";
    private static final String LOCAL_UPLOAD = "http://localhost:8080/x";
    private static final String SAME_HOST_HTTP_UPLOAD = "http://api.allegro.pl/x";
    private static final String SAME_HOST_HTTPS_UPLOAD = "https://api.allegro.pl/x";
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
    void fetchLocation_whenResponseHasLocationHeader_returnsBodyAndLocation(WireMockRuntimeInfo wmInfo) {
        // given — an attachment declaration returns the absolute upload URL in Location
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(LOCATION_HEADER, UPLOAD_URL).withBody(OK_BODY)));

        // when
        Located<Map> located = support(wmInfo).request(OPERATION).post(PATH)
                .jsonBody(Map.of("declare", "attachment")).fetchLocation(Map.class);

        // then — the upload URL is exposed alongside the deserialized body
        assertEquals(UPLOAD_URL, located.location());
        assertEquals(OK_VALUE, located.value().get("value"));
    }

    @Test
    void putAbsolute_whenUploadHost_sendsBinaryToTheAbsoluteUrl(WireMockRuntimeInfo wmInfo) {
        // given — the absolute upload URL (here pointed back at WireMock) from a Location header
        stubFor(put(urlEqualTo(UPLOAD_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(IMAGE_CONTENT_TYPE))
                .withRequestBody(binaryEqualTo(IMAGE_BYTES))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));
        String uploadUrl = wmInfo.getHttpBaseUrl() + UPLOAD_PATH;

        // when — PUT the binary to the ABSOLUTE url, bypassing the API base
        Map<?, ?> mapped = support(wmInfo).request(OPERATION)
                .putAbsolute(uploadUrl).binaryBody(IMAGE_BYTES, IMAGE_CONTENT_TYPE).fetch(Map.class);

        // then — the request hit the absolute path (not base + path), authenticated, with the bytes
        assertEquals(OK_VALUE, mapped.get("value"));
        verify(1, putRequestedFor(urlEqualTo(UPLOAD_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withRequestBody(binaryEqualTo(IMAGE_BYTES)));
    }

    @Test
    void fetchLocation_whenNoLocationHeader_locationIsNull(WireMockRuntimeInfo wmInfo) {
        // given — a response without a Location header
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        Located<Map> located = support(wmInfo).request(OPERATION).post(PATH)
                .jsonBody(Map.of("declare", "attachment")).fetchLocation(Map.class);

        // then — the nullable location is null, the body still deserialized
        assertNull(located.location());
        assertEquals(OK_VALUE, located.value().get("value"));
    }

    @Test
    void secureUploadTarget_whenHttpAllegroHost_forcesHttps() {
        // Allegro returns the upload host as plaintext http; the Bearer must not be
        // sent in the clear, so an Allegro upload host is forced to https.
        assertEquals(URI.create(UPLOAD_URL_HTTPS), HttpCall.secureUploadTarget(UPLOAD_URL, API_BASE));
    }

    @Test
    void secureUploadTarget_whenSandboxUploadHost_forcesHttps() {
        assertEquals(URI.create(SANDBOX_UPLOAD_HTTPS),
                HttpCall.secureUploadTarget(SANDBOX_UPLOAD_HTTP, SANDBOX_BASE));
    }

    @Test
    void secureUploadTarget_whenNonAllegroHost_refusesToSendToken() {
        // never forward the access token to a host outside allegro.pl
        assertThrows(IllegalArgumentException.class,
                () -> HttpCall.secureUploadTarget(FOREIGN_UPLOAD, API_BASE));
    }

    @Test
    void secureUploadTarget_whenSameHostAsBase_keepsBaseScheme() {
        // the WireMock/local case: base and upload share the host, so the base's own
        // (here plaintext) scheme is used verbatim — no forced https to a dead port
        assertEquals(URI.create(LOCAL_UPLOAD), HttpCall.secureUploadTarget(LOCAL_UPLOAD, LOCAL_BASE));
    }

    @Test
    void secureUploadTarget_whenBaseHostButHttpAgainstHttpsBase_upgradesToBaseHttps() {
        // a Location naming the base host over http must still not downgrade the token
        assertEquals(URI.create(SAME_HOST_HTTPS_UPLOAD),
                HttpCall.secureUploadTarget(SAME_HOST_HTTP_UPLOAD, API_BASE));
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

        // then — the decoded error body reaches the typed exception, proving the
        // bytes→String error path ran (not just status-code inference)
        AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                () -> support(wmInfo).request(OPERATION).get(PATH).accept(PDF_MEDIA_TYPE).fetchBytes());
        assertTrue(failure.responseBody().contains("no such attachment"));
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
    void fetchWithETag_whenNoEtagHeader_returnsNullEtag(WireMockRuntimeInfo wmInfo) {
        // given — the server sent no ETag (the resource is not conditionally guardable)
        stubFor(get(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        Etagged<Map> result = support(wmInfo).request(OPERATION).get(PATH).fetchWithETag(Map.class);

        // then — value is still deserialized, etag is null
        assertEquals(OK_VALUE, result.value().get("value"));
        assertNull(result.etag());
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

    @Test
    void jsonBodyPartial_whenFieldNullOrEmpty_omitsThemFromBody(WireMockRuntimeInfo wmInfo) {
        // given — a partial payload with a null field and an empty collection
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).post(PATH)
                .jsonBodyPartial(new Payload(PRESENT_VALUE, null, List.of())).send();

        // then — only the set field survives (a strict match rejects any extra field,
        // so a serialized null OR an empty [] would fail this)
        verify(1, postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson("{\"present\":\"" + PRESENT_VALUE + "\"}", true, false)));
    }

    @Test
    void jsonBody_whenFieldNullOrEmpty_keepsThemInBody(WireMockRuntimeInfo wmInfo) {
        // given — the same payload sent with the plain serializer
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).post(PATH)
                .jsonBody(new Payload(PRESENT_VALUE, null, List.of())).send();

        // then — the default serializer keeps the null field and empty collection (the
        // contrast that makes jsonBodyPartial necessary for a PATCH)
        verify(1, postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson(
                        "{\"present\":\"" + PRESENT_VALUE + "\",\"absent\":null,\"items\":[]}", true, false)));
    }

    @Test
    void betaJsonBody_whenSet_sendsBetaVendorContentTypeWithFullBody(WireMockRuntimeInfo wmInfo) {
        // given — a beta write surface that rejects the public.v1 content type
        stubFor(post(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when — a payload with a null field and an empty collection
        support(wmInfo).request(OPERATION).post(PATH)
                .betaJsonBody(new Payload(PRESENT_VALUE, null, List.of())).send();

        // then — beta media type AND the FULL serializer keeps null/empty fields
        // (a strict match proves betaJsonBody did not delegate to the partial path)
        verify(1, postRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(equalToJson(
                        "{\"present\":\"" + PRESENT_VALUE + "\",\"absent\":null,\"items\":[]}", true, false)));
    }

    @Test
    void betaJsonBodyPartial_whenFieldNullOrEmpty_omitsThemWithBetaContentType(WireMockRuntimeInfo wmInfo) {
        // given — a partial beta payload with a null field and an empty collection
        stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        support(wmInfo).request(OPERATION).post(PATH)
                .betaJsonBodyPartial(new Payload(PRESENT_VALUE, null, List.of())).send();

        // then — beta content type AND only the set field survives (partial semantics)
        verify(1, postRequestedFor(urlEqualTo(PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(equalToJson("{\"present\":\"" + PRESENT_VALUE + "\"}", true, false)));
    }

    /** A partial payload with a nullable field and a collection (a PATCH-style body). */
    private record Payload(String present, String absent, List<String> items) {
    }
}
