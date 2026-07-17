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
    private static final String OPAQUE_TOKEN = "opaque-rotating-refresh-token-value";
    private static final String RETRY_AFTER_VALUE = "7";
    private static final long RETRY_AFTER_SECONDS = 7L;
    private static final String OPERATION_GET = "read resource";
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final String RETRY_AFTER_SHORT_VALUE = "1";
    private static final long RETRY_AFTER_SHORT_SECONDS = 1L;
    // spec-derived: not yet wire-verified (shape per the errors[] contract;
    // to be confirmed by a captured 400 during a bucket exploration pass)
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
        return runtime(wmInfo, RetryPolicy.builder().enabled(false).build(),
                AllegroExecutionInterceptor.noop());
    }

    private static HttpRuntime runtime(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy,
            AllegroExecutionInterceptor interceptor) {
        var mapper = new ObjectMapper();
        var retryHandler = new RetryHandler(HttpClient.newHttpClient(), retryPolicy);
        return new HttpRuntime() {
            @Override public String baseUrl() {
                return wmInfo.getHttpBaseUrl();
            }

            @Override public RetryHandler retryHandler() {
                return retryHandler;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return interceptor;
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
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));
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
        Map<String, String> emptyPayload = Map.of();

        // then — both errors parsed, dot-paths preserved, null userMessage legal
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> support.postJsonAuthenticated(TEST_PATH, emptyPayload, Map.class, OPERATION_PUT));
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
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_VALUE)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // then
        AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                () -> support.getAuthenticated(TEST_PATH, Map.class, OPERATION_GET));
        assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
    }

    @Test
    void getAuthenticated_when429PersistsWithRetriesEnabled_retriesThenThrowsRateLimit(
            WireMockRuntimeInfo wmInfo) {
        // given — persistent 429 with a short Retry-After and ENABLED retries
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SHORT_VALUE)));
        HttpSupport support = new HttpSupport(runtime(wmInfo,
                RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).build(),
                AllegroExecutionInterceptor.noop()));

        // then — the wire saw every allowed attempt, then the typed rate limit
        AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                () -> support.getAuthenticated(TEST_PATH, Map.class, OPERATION_GET));
        assertEquals(RETRY_AFTER_SHORT_SECONDS, failure.retryAfterSeconds());
        verify(RETRY_MAX_ATTEMPTS,
                com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void getAuthenticated_when500WithRetryDisabled_throwsServerExceptionWithBody(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withBody(VALIDATION_BODY)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // then — 5xx maps to the server-trouble type, body preserved
        var failure = assertThrows(
                io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException.class,
                () -> support.getAuthenticated(TEST_PATH, Map.class, OPERATION_GET));
        assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, failure.statusCode());
        assertTrue(failure.responseBody().contains("VALIDATION_ERROR"));
    }

    @Test
    void getAuthenticated_when403_throwsAuthException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_FORBIDDEN)));
        HttpSupport support = new HttpSupport(runtime(wmInfo));

        // then — 403 folds into the auth-remediation type
        assertThrows(io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException.class,
                () -> support.getAuthenticated(TEST_PATH, Map.class, OPERATION_GET));
    }

    @Test
    void putJsonAuthenticated_whenSucceeds_firesBeforeAndAfterExecutionOnce(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));
        List<String> lifecycle = new java.util.ArrayList<>();
        var recording = new AllegroExecutionInterceptor() {
            @Override public void beforeExecution(
                    io.github.mgrtomaszzurawski.allegro.sdk.config.CallContext context) {
                lifecycle.add("before:" + context.attempt());
            }

            @Override public void afterExecution(
                    io.github.mgrtomaszzurawski.allegro.sdk.config.CallContext context) {
                lifecycle.add("after:" + context.statusCode());
            }
        };
        HttpSupport support = new HttpSupport(runtime(wmInfo,
                RetryPolicy.builder().enabled(false).build(), recording));

        // when
        support.putJsonAuthenticated(TEST_PATH, Map.of("name", "payload"), Map.class, OPERATION_PUT);

        // then — execution-level callbacks fire exactly once, attempt 0
        assertEquals(List.of("before:0", "after:" + TestHttpConstants.HTTP_OK), lifecycle);
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
    void safeResponseBody_whenBodyCarriesTokens_redactsJwtAndTokenFields() {
        // given — a JWT inside a token field AND an opaque (non-JWT) refresh
        // token: both must vanish from the safe view
        AllegroException failure = new AllegroException("boom", TestHttpConstants.HTTP_SERVER_ERROR,
                "{\"access_token\":\"" + JWT_SHAPED + "\","
                        + "\"refresh_token\":\"" + OPAQUE_TOKEN + "\","
                        + "\"note\":\"" + JWT_SHAPED + "\"}");

        // then — raw body keeps everything, safe body redacts every token form
        assertTrue(failure.responseBody().contains(JWT_SHAPED));
        String safeBody = failure.safeResponseBody();
        assertFalse(safeBody.contains(JWT_SHAPED));
        assertFalse(safeBody.contains(OPAQUE_TOKEN));
        assertTrue(safeBody.contains("\"access_token\":\"***\""));
        assertTrue(safeBody.contains("\"refresh_token\":\"***\""));
        assertTrue(safeBody.contains("eyJ***"));
    }

    @Test
    void subPath_whenJoiningSegments_normalizesSeparators() {
        // then
        assertEquals("/sale/offers/123", ApiPaths.subPath("/sale/offers", "123"));
        assertEquals("/sale/offers/123/tags", ApiPaths.subPath("/sale/offers/", "123", "tags"));
    }
}
