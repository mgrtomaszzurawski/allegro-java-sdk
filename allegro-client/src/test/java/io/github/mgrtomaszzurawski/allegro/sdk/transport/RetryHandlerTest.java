/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.transport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.CallContext;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
class RetryHandlerTest {

    private static final String TEST_PATH = "/retry-target";
    private static final String OPERATION = "test operation";
    private static final String SCENARIO_RECOVERY = "recovery";
    private static final String STATE_RECOVERED = "recovered";
    private static final String OK_BODY = "{\"ok\":true}";
    private static final String BUSY_BODY = "{\"errors\":[]}";
    private static final int MAX_ATTEMPTS = 3;

    private static RetryHandler handler() {
        return new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().maxAttempts(MAX_ATTEMPTS).build());
    }

    private static HttpRequest getRequest(WireMockRuntimeInfo wmInfo) {
        return HttpRequest.newBuilder(URI.create(wmInfo.getHttpBaseUrl() + TEST_PATH)).GET().build();
    }

    private static HttpRequest postRequest(WireMockRuntimeInfo wmInfo) {
        return HttpRequest.newBuilder(URI.create(wmInfo.getHttpBaseUrl() + TEST_PATH))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
    }

    @Test
    void send_when429ThenOk_retriesHonouringRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH)).inScenario(SCENARIO_RECOVERY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, "1")
                        .withBody(BUSY_BODY))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(TEST_PATH)).inScenario(SCENARIO_RECOVERY)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(OK_BODY)));

        // when
        HttpResponse<String> response = handler().send(getRequest(wmInfo), OPERATION,
                AllegroExecutionInterceptor.noop());

        // then
        assertEquals(TestHttpConstants.HTTP_OK, response.statusCode());
        verify(2, getRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void send_when500Persists_returnsLastResponseAfterMaxAttempts(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        // when
        HttpResponse<String> response = handler().send(getRequest(wmInfo), OPERATION,
                AllegroExecutionInterceptor.noop());

        // then — retried to exhaustion, caller maps the final 500
        assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, response.statusCode());
        verify(MAX_ATTEMPTS, getRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void send_whenPostGets500_doesNotRetryByDefault(WireMockRuntimeInfo wmInfo) {
        // given — writes are not idempotent; retryPost defaults to false
        stubFor(post(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        // when
        HttpResponse<String> response = handler().send(postRequest(wmInfo), OPERATION,
                AllegroExecutionInterceptor.noop());

        // then
        assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, response.statusCode());
        verify(1, postRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void send_whenDisabledPolicy_singleAttemptOnly(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RetryHandler disabledRetry = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());

        // when
        disabledRetry.send(getRequest(wmInfo), OPERATION, AllegroExecutionInterceptor.noop());

        // then
        verify(1, getRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    void send_whenNetworkFailure_throwsServerExceptionWithZeroStatus() {
        // given — nothing listens on this port
        HttpRequest unroutable = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:1/never")).GET().build();
        RetryHandler singleAttempt = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        AllegroExecutionInterceptor noopInterceptor = AllegroExecutionInterceptor.noop();

        // then — transport failure is distinguishable: status 0, cause present
        AllegroServerException failure = assertThrows(AllegroServerException.class,
                () -> singleAttempt.send(unroutable, OPERATION, noopInterceptor));
        assertEquals(0, failure.statusCode());
    }

    @Test
    void send_whenRetrying_interceptorSeesEveryAttempt(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(TEST_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        List<CallContext> attempts = new ArrayList<>();
        AllegroExecutionInterceptor recording = new AllegroExecutionInterceptor() {
            @Override
            public void afterAttempt(CallContext context) {
                attempts.add(context);
            }
        };

        // when
        handler().send(getRequest(wmInfo), OPERATION, recording);

        // then — one afterAttempt per wire attempt, numbered from 1
        assertEquals(MAX_ATTEMPTS, attempts.size());
        assertEquals(1, attempts.get(0).attempt());
        assertEquals(MAX_ATTEMPTS, attempts.get(attempts.size() - 1).attempt());
        assertEquals(TestHttpConstants.HTTP_SERVER_ERROR, attempts.get(0).statusCode());
    }
}
