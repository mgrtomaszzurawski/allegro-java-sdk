/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAuthException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Maps a non-2xx Allegro response to the remediation-grouped exception
 * hierarchy, parsing the structured {@code errors[]} payload into typed
 * {@link AllegroFieldError}s where present.
 *
 * @since 0.1.0
 */
public final class ServerErrorParser {

    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MIN = 500;

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String TRACE_ID_HEADER = "trace-id";
    private static final String ERRORS_FIELD = "errors";
    private static final String CODE_FIELD = "code";
    private static final String MESSAGE_FIELD = "message";
    private static final String USER_MESSAGE_FIELD = "userMessage";
    private static final String PATH_FIELD = "path";
    private static final String DETAILS_FIELD = "details";

    private static final String MSG_BAD_REQUEST = "Allegro rejected the request as invalid";
    private static final String MSG_AUTH = "Allegro rejected the credentials or token";
    private static final String MSG_NOT_FOUND = "Requested Allegro resource does not exist";
    private static final String MSG_RATE_LIMIT = "Allegro rate limit exceeded";
    private static final String MSG_SERVER = "Allegro server-side error";
    private static final String MSG_UNEXPECTED = "Unexpected Allegro response status";
    private static final String OPERATION_PREFIX = " during ";

    private final ObjectMapper objectMapper;

    public ServerErrorParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Translate a non-2xx response into the matching typed exception. Never
     * returns normally — always throws.
     *
     * @param response the failed HTTP response
     * @param operationName human-readable operation label for the message
     */
    public AllegroException toException(HttpResponse<String> response, String operationName) {
        int status = response.statusCode();
        String body = response.body();
        String suffix = OPERATION_PREFIX + operationName;
        String traceId = traceId(response);
        if (status == HTTP_BAD_REQUEST || status == HTTP_UNPROCESSABLE) {
            return new AllegroBadRequestException(MSG_BAD_REQUEST + suffix, status, body,
                    parseErrors(body), traceId);
        }
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            return new AllegroAuthException(MSG_AUTH + suffix, status, body, traceId);
        }
        if (status == HTTP_NOT_FOUND) {
            return new AllegroNotFoundException(MSG_NOT_FOUND + suffix, status, body, traceId);
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return new AllegroRateLimitException(MSG_RATE_LIMIT + suffix, status, body,
                    parseRetryAfterSeconds(response), traceId);
        }
        if (status >= HTTP_SERVER_ERROR_MIN) {
            return new AllegroServerException(MSG_SERVER + suffix, status, body, traceId);
        }
        return new AllegroException(MSG_UNEXPECTED + " (" + status + ")" + suffix, null,
                status, body, traceId);
    }

    /** Parse the {@code errors[]} payload; empty list when absent/malformed. */
    public List<AllegroFieldError> parseErrors(@Nullable String body) {
        List<AllegroFieldError> fieldErrors = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return fieldErrors;
        }
        try {
            JsonNode errorsNode = objectMapper.readTree(body).path(ERRORS_FIELD);
            for (JsonNode errorNode : errorsNode) {
                fieldErrors.add(new AllegroFieldError(
                        errorNode.path(CODE_FIELD).asText(""),
                        errorNode.path(MESSAGE_FIELD).asText(""),
                        textOrNull(errorNode, USER_MESSAGE_FIELD),
                        textOrNull(errorNode, PATH_FIELD),
                        textOrNull(errorNode, DETAILS_FIELD)));
            }
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            // A non-JSON error body (proxy HTML, empty page) is legal input here:
            // the typed-error list is best-effort enrichment, the exception with
            // the raw body still surfaces. Nothing to recover.
            return fieldErrors;
        }
        return fieldErrors;
    }

    /** The {@code trace-id} header Allegro attaches to error responses, or {@code null}. */
    public static @Nullable String traceId(HttpResponse<String> response) {
        return response.headers().firstValue(TRACE_ID_HEADER).orElse(null);
    }

    /** {@code Retry-After} header in seconds; {@code 0} when absent/invalid. */
    public static long parseRetryAfterSeconds(HttpResponse<String> response) {
        return response.headers().firstValue(RETRY_AFTER_HEADER)
                .map(ServerErrorParser::parseLongOrZero)
                .orElse(0L);
    }

    private static long parseLongOrZero(String headerValue) {
        try {
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException e) {
            // HTTP-date variant of Retry-After (rare) — treated as "not sent";
            // the backoff strategy still applies its own delay.
            return 0L;
        }
    }

    private static @Nullable String textOrNull(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? null : valueNode.asText();
    }
}
