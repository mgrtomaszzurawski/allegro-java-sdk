/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.core.JacksonException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared HTTP plumbing for domain clients: builds authenticated requests with
 * the vendor media type, executes them through the retry handler, performs the
 * single-attempt 401 re-auth, maps non-2xx responses to typed exceptions, and
 * deserializes bodies to generated {@code *Raw} DTOs.
 *
 * <p>Logs operation names and status codes only — never request/response
 * bodies or tokens.
 *
 * @since 0.1.0
 */
public final class HttpSupport {

    /** Allegro versions resources via this vendor media type, not URL paths. */
    public static final String VND_ALLEGRO_V1 = "application/vnd.allegro.public.v1+json";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSupport.class);
    private static final String LOG_CALL = "[{}] {} {} -> {}";

    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final String ERR_SERIALIZE = "Failed to serialize request body";
    private static final String ERR_DESERIALIZE = "Failed to deserialize Allegro response";

    private final HttpRuntime runtime;
    private final ServerErrorParser errorParser;

    public HttpSupport(HttpRuntime runtime) {
        this.runtime = runtime;
        this.errorParser = new ServerErrorParser(runtime.objectMapper());
    }

    /** Resolve an {@link ApiPaths} path against the environment base URL. */
    public URI uri(String path) {
        return URI.create(runtime.baseUrl() + path);
    }

    /** Authenticated GET, response deserialized to {@code responseType}. */
    public <T> T getAuthenticated(String path, Class<T> responseType, String operationName) {
        HttpResponse<String> response = exchange(() -> HttpRequest.newBuilder(uri(path))
                .timeout(runtime.readTimeout())
                .header(ACCEPT_HEADER, VND_ALLEGRO_V1)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + runtime.requireToken())
                .GET(), operationName);
        return deserialize(response, responseType);
    }

    /** Authenticated POST with a JSON body, response deserialized. */
    public <T> T postJsonAuthenticated(String path, Object body, Class<T> responseType,
            String operationName) {
        String json = serialize(body);
        HttpResponse<String> response = exchange(() -> HttpRequest.newBuilder(uri(path))
                .timeout(runtime.readTimeout())
                .header(ACCEPT_HEADER, VND_ALLEGRO_V1)
                .header(CONTENT_TYPE_HEADER, VND_ALLEGRO_V1)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + runtime.requireToken())
                .POST(HttpRequest.BodyPublishers.ofString(json)), operationName);
        return deserialize(response, responseType);
    }

    /** Authenticated PUT with a JSON body, response deserialized. */
    public <T> T putJsonAuthenticated(String path, Object body, Class<T> responseType,
            String operationName) {
        String json = serialize(body);
        HttpResponse<String> response = exchange(() -> HttpRequest.newBuilder(uri(path))
                .timeout(runtime.readTimeout())
                .header(ACCEPT_HEADER, VND_ALLEGRO_V1)
                .header(CONTENT_TYPE_HEADER, VND_ALLEGRO_V1)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + runtime.requireToken())
                .PUT(HttpRequest.BodyPublishers.ofString(json)), operationName);
        return deserialize(response, responseType);
    }

    /** Authenticated DELETE expecting no content. */
    public void deleteAuthenticated(String path, String operationName) {
        exchange(() -> HttpRequest.newBuilder(uri(path))
                .timeout(runtime.readTimeout())
                .header(ACCEPT_HEADER, VND_ALLEGRO_V1)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + runtime.requireToken())
                .DELETE(), operationName);
    }

    /**
     * Execute with retry, single-attempt 401 re-auth, and typed error mapping.
     * The request builder is a supplier because the retry-after-reauth attempt
     * must re-read the (new) access token.
     */
    private HttpResponse<String> exchange(Supplier<HttpRequest.Builder> requestBuilder,
            String operationName) {
        HttpRequest request = requestBuilder.get().build();
        HttpResponse<String> response = runtime.retryHandler().send(request);
        if (response.statusCode() == HTTP_UNAUTHORIZED) {
            // Single attempt: token may simply have been revoked server-side;
            // re-acquire once and replay. A second 401 is a real auth failure.
            runtime.reauthenticate();
            response = runtime.retryHandler().send(requestBuilder.get().build());
        }
        LOGGER.debug(LOG_CALL, operationName, request.method(), request.uri(), response.statusCode());
        if (response.statusCode() < HTTP_OK_MIN || response.statusCode() > HTTP_OK_MAX) {
            throw errorParser.toException(response, operationName);
        }
        return response;
    }

    private String serialize(Object body) {
        try {
            return runtime.objectMapper().writeValueAsString(body);
        } catch (JacksonException e) {
            throw new AllegroException(ERR_SERIALIZE, e);
        }
    }

    private <T> T deserialize(HttpResponse<String> response, Class<T> responseType) {
        try {
            return runtime.objectMapper().readValue(response.body(), responseType);
        } catch (JacksonException e) {
            throw new AllegroServerException(ERR_DESERIALIZE, e);
        }
    }
}
