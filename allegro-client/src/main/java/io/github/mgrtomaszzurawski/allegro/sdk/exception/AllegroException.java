/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.exception;

import java.io.Serial;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Base exception for all Allegro SDK errors. All subclasses are unchecked and
 * grouped by <em>remediation</em> — what the consumer can do about the failure
 * — not by HTTP status:
 *
 * <ul>
 *   <li>{@link AllegroBadRequestException} — fix the request (typed field errors)</li>
 *   <li>{@link AllegroAuthException} — fix credentials / re-authorize</li>
 *   <li>{@link AllegroRateLimitException} — slow down, retry after a delay</li>
 *   <li>{@link AllegroNotFoundException} — the referenced resource does not exist</li>
 *   <li>{@link AllegroServerException} — server-side/network trouble; retry later</li>
 *   <li>{@link AllegroAsyncTimeoutException} — polling gave up; the operation may
 *       still have succeeded server-side</li>
 *   <li>{@link AllegroConfigException} — fix the client configuration (fail-fast)</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class AllegroException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * JWT shape — three dot-separated base64url segments. The header always
     * begins with {@code eyJ} (base64url of a JSON object opening), which keeps
     * the pattern tight enough not to match unrelated dotted tokens.
     */
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final String BEARER_TOKEN_REPLACEMENT = "eyJ***";

    private final int statusCode;
    private final @Nullable String responseBody;
    private final @Nullable String traceId;

    public AllegroException(String message, @Nullable Throwable cause, int statusCode,
            @Nullable String responseBody, @Nullable String traceId) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.traceId = traceId;
    }

    public AllegroException(String message, @Nullable Throwable cause, int statusCode,
            @Nullable String responseBody) {
        this(message, cause, statusCode, responseBody, null);
    }

    public AllegroException(String message, int statusCode, @Nullable String responseBody) {
        this(message, null, statusCode, responseBody);
    }

    public AllegroException(String message, @Nullable Throwable cause) {
        this(message, cause, 0, null);
    }

    /** HTTP status of the failed call, or {@code 0} for non-HTTP failures. */
    public int statusCode() {
        return statusCode;
    }

    /**
     * The {@code trace-id} header Allegro attaches to error responses, or
     * {@code null} for failures without a server response. Quote it in
     * Allegro support tickets — it is how their team locates the request.
     */
    public @Nullable String traceId() {
        return traceId;
    }

    /**
     * Raw response body as returned by Allegro, or {@code null}. May contain
     * OAuth tokens — never log directly; use {@link #safeResponseBody()} for
     * diagnostic output.
     */
    public @Nullable String responseBody() {
        return responseBody;
    }

    /** Response body with JWT-shaped tokens redacted — safe for logs. */
    public @Nullable String safeResponseBody() {
        if (responseBody == null) {
            return null;
        }
        return BEARER_TOKEN.matcher(responseBody).replaceAll(BEARER_TOKEN_REPLACEMENT);
    }
}
