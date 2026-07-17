/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ServerErrorParser;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
class HttpSupportAndErrorsTest {

    private static final String TEST_TOKEN = "support-test-token";
    private static final String TEST_PATH = "/support-target";
    private static final String OPERATION_PUT = "update resource";
    private static final String OPERATION_DELETE = "delete resource";
    private static final String OK_BODY = "{\"value\":\"ok\"}";
    private static final String JWT_SHAPED =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2lnbmF0dXJl";
    private static final String VALIDATION_BODY = """
            {"errors":[
              {"code":"VALIDATION_ERROR","message":"price too low",
               "userMessage":"Cena za niska","path":"sellingMode.price","details":"MinPrice"},
              {"code":"MISSING_PARAMETER","message":"ean required","userMessage":null,
               "path":"parameters.ean"}
            ]}
            """;

    /** Minimal runtime host for direct HttpSupport tests. */
    private static HttpRuntime runtime(WireMockRuntimeInfo wmInfo) {
        var mapper = new ObjectMapper();
        var retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        return new HttpRuntime() {
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
                // no-op in this fixture: 401 paths are covered in AllegroClientMeTest
            }
        };
    }

    @Test
    void putJsonAuthenticated_whenAccepted_sendsVendorContentTypeAndBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(TEST_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withRequestBody(containing("\"name\":\"payload\""))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // when
        Map<?, ?> mapped = support.putJsonAuthenticated(TEST_PATH,
                Map.of("name", "payload"), Map.class, OPERATION_PUT);

        // then
        assertEquals("ok", mapped.get("value"));
        verify(1, putRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void deleteAuthenticated_whenNoContent_verifiesWireCall(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(delete(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(204)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // when
        support.deleteAuthenticated(TEST_PATH, OPERATION_DELETE);

        // then
        verify(1, deleteRequestedFor(urlEqualTo(TEST_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
    }

    @Test
    void postJsonAuthenticated_when400_throwsBadRequestWithTypedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withBody(VALIDATION_BODY)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // then — both errors parsed, dot-paths preserved, null userMessage legal
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> support.postJsonAuthenticated(TEST_PATH, Map.of(), Map.class, OPERATION_PUT));
        List<AllegroFieldError> errors = failure.errors();
        assertEquals(2, errors.size());
        assertEquals("VALIDATION_ERROR", errors.get(0).code());
        assertEquals("sellingMode.price", errors.get(0).path());
        assertEquals("Cena za niska", errors.get(0).userMessage());
        assertEquals("parameters.ean", errors.get(1).path());
    }

    @Test
    void getAuthenticated_when429AfterRetries_throwsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — retry disabled in this fixture, so the 429 maps immediately
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, "7")));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // then
        AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                () -> support.getAuthenticated(TEST_PATH, Map.class, OPERATION_PUT));
        assertEquals(7L, failure.retryAfterSeconds());
    }

    @Test
    void parseErrors_whenBodyMalformed_returnsEmptyListNotException() {
        // given
        ServerErrorParser parser = new ServerErrorParser(new ObjectMapper());

        // when / then — proxy HTML instead of JSON must not blow up enrichment
        assertTrue(parser.parseErrors("<html>Bad Gateway</html>").isEmpty());
        assertTrue(parser.parseErrors(null).isEmpty());
        assertTrue(parser.parseErrors("  ").isEmpty());
    }

    @Test
    void safeResponseBody_whenBodyCarriesJwt_redactsToken() {
        // given
        AllegroException failure = new AllegroException("boom", 500,
                "{\"access_token\":\"" + JWT_SHAPED + "\"}");

        // then — raw body keeps it, safe body redacts it
        assertTrue(failure.responseBody().contains(JWT_SHAPED));
        assertFalse(failure.safeResponseBody().contains(JWT_SHAPED));
        assertTrue(failure.safeResponseBody().contains("eyJ***"));
    }

    @Test
    void subPath_whenJoiningSegments_normalizesSeparators() {
        // then
        assertEquals("/sale/offers/123", ApiPaths.subPath("/sale/offers", "123"));
        assertEquals("/sale/offers/123/tags", ApiPaths.subPath("/sale/offers/", "123", "tags"));
    }
}
