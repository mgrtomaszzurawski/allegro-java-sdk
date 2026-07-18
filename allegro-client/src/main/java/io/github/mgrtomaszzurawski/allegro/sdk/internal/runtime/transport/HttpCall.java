/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Fluent, single-use description of one authenticated Allegro request.
 *
 * <p>Every domain client verb funnels through this builder, so the whole SDK
 * shares one request-construction path: vendor media type, bearer header,
 * optional query parameters, {@code Accept-Language}, {@code If-Match}, a JSON
 * or binary body, and the terminal {@link #fetch(Class)} / {@link #send()}. The
 * retry, single-attempt 401 replay and typed error mapping all live in
 * {@link HttpSupport#exchange}; this class only assembles the request and, on
 * replay, re-reads the (possibly refreshed) access token because the builder is
 * rebuilt on every attempt.
 *
 * <p>Obtain one from {@link HttpSupport#request(String)}; not thread-safe and
 * not reusable across calls.
 *
 * @since 0.2.0
 */
public final class HttpCall {

    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final String IF_MATCH_HEADER = "If-Match";
    private static final String ETAG_HEADER = "ETag";
    private static final String LOCATION_HEADER = "Location";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_PATCH = "PATCH";
    private static final String METHOD_DELETE = "DELETE";

    private static final String ERR_NO_VERB = "HTTP verb not set - call get/post/put/patch/delete first";

    private final HttpSupport support;
    private final String operationName;

    private String method;
    private String path;
    private String absoluteUrl;
    private Query query = Query.create();
    private String acceptMediaType = HttpSupport.VND_ALLEGRO_V1;
    private String acceptLanguage;
    private String ifMatch;
    private String contentType;
    private HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

    HttpCall(HttpSupport support, String operationName) {
        this.support = support;
        this.operationName = operationName;
    }

    /** GET {@code path}. */
    public HttpCall get(String requestPath) {
        return verb(METHOD_GET, requestPath);
    }

    /** POST {@code path}. */
    public HttpCall post(String requestPath) {
        return verb(METHOD_POST, requestPath);
    }

    /** PUT {@code path}. */
    public HttpCall put(String requestPath) {
        return verb(METHOD_PUT, requestPath);
    }

    /** PATCH {@code path} (the JDK client has no {@code .PATCH()} shortcut). */
    public HttpCall patch(String requestPath) {
        return verb(METHOD_PATCH, requestPath);
    }

    /** DELETE {@code path}. */
    public HttpCall delete(String requestPath) {
        return verb(METHOD_DELETE, requestPath);
    }

    /**
     * PUT to an ABSOLUTE URL, bypassing the API base — for the attachment upload
     * host ({@code upload.allegro.pl}) returned in a declaration's {@code Location}
     * header. The Bearer token and Accept media type are still sent, so the upload
     * is authenticated; combine with {@link #binaryBody(byte[], String)}.
     */
    public HttpCall putAbsolute(String url) {
        this.method = METHOD_PUT;
        this.absoluteUrl = url;
        return this;
    }

    private HttpCall verb(String httpMethod, String requestPath) {
        this.method = httpMethod;
        this.path = requestPath;
        return this;
    }

    /** Attach encoded query parameters. */
    public HttpCall query(Query requestQuery) {
        this.query = requestQuery;
        return this;
    }

    /** Request the beta vendor media type instead of {@code public.v1}. */
    public HttpCall acceptBeta() {
        this.acceptMediaType = HttpSupport.VND_ALLEGRO_BETA_V1;
        return this;
    }

    /** Request an arbitrary media type (e.g. {@code application/pdf} downloads). */
    public HttpCall accept(String mediaType) {
        this.acceptMediaType = mediaType;
        return this;
    }

    /** Localize the response ({@code Accept-Language}); {@code null} omits it. */
    public HttpCall acceptLanguage(String language) {
        this.acceptLanguage = language;
        return this;
    }

    /** Guard a conditional write with the resource's {@code ETag}. */
    public HttpCall ifMatch(String etag) {
        this.ifMatch = etag;
        return this;
    }

    /** Serialize {@code body} as the vendor JSON request body. */
    public HttpCall jsonBody(Object body) {
        return fullJsonBody(body, HttpSupport.VND_ALLEGRO_V1);
    }

    /**
     * Serialize {@code body} as the vendor JSON request body, omitting null and
     * empty fields (null, empty strings, empty collections and maps) — for a
     * partial (PATCH) update where unset fields must be absent rather than sent
     * as {@code null}/{@code []} (which would reset them server-side).
     */
    public HttpCall jsonBodyPartial(Object body) {
        return partialJsonBody(body, HttpSupport.VND_ALLEGRO_V1);
    }

    /**
     * Serialize {@code body} as the BETA vendor JSON request body
     * ({@code application/vnd.allegro.beta.v1+json}). Beta write surfaces (e.g.
     * post-purchase issues, customer-return rejection) reject the {@code public.v1}
     * content type, so a beta write must set it on the request body — {@link
     * #acceptBeta()} only flips the {@code Accept} header.
     */
    public HttpCall betaJsonBody(Object body) {
        return fullJsonBody(body, HttpSupport.VND_ALLEGRO_BETA_V1);
    }

    /**
     * Beta counterpart of {@link #jsonBodyPartial(Object)}: a partial body (null
     * and empty fields omitted) with the beta vendor content type — beta write
     * DTOs are generated too, so they carry the same pre-initialized empty
     * collections that a full serialization would reset server-side.
     */
    public HttpCall betaJsonBodyPartial(Object body) {
        return partialJsonBody(body, HttpSupport.VND_ALLEGRO_BETA_V1);
    }

    private HttpCall fullJsonBody(Object body, String mediaType) {
        this.contentType = mediaType;
        this.bodyPublisher = HttpRequest.BodyPublishers.ofString(support.serialize(body));
        return this;
    }

    private HttpCall partialJsonBody(Object body, String mediaType) {
        this.contentType = mediaType;
        this.bodyPublisher = HttpRequest.BodyPublishers.ofString(support.serializePartial(body));
        return this;
    }

    /** Send raw bytes with the caller's content type (image/attachment upload). */
    public HttpCall binaryBody(byte[] bytes, String bodyContentType) {
        this.contentType = bodyContentType;
        this.bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(bytes);
        return this;
    }

    /** Execute and deserialize the response body to {@code responseType}. */
    public <T> T fetch(Class<T> responseType) {
        HttpResponse<String> response = support.exchange(this::buildRequest, operationName);
        return support.deserialize(response, responseType);
    }

    /** Execute a write whose response body is empty or ignored (204/200-void). */
    public void send() {
        support.exchange(this::buildRequest, operationName);
    }

    /**
     * Execute and return the raw response bytes (binary downloads: invoice PDFs,
     * message/dispute attachments). On a non-2xx status the vendor JSON error is
     * still decoded and mapped to a typed exception.
     */
    public byte[] fetchBytes() {
        HttpResponse<byte[]> response = support.exchangeFor(this::buildRequest, operationName,
                HttpResponse.BodyHandlers.ofByteArray(),
                bytes -> new String(bytes, StandardCharsets.UTF_8));
        return response.body();
    }

    /**
     * Execute and return the deserialized body together with the response
     * {@code ETag}, so a later {@link #ifMatch(String)} write can be guarded
     * against a concurrent modification.
     */
    public <T> Etagged<T> fetchWithETag(Class<T> responseType) {
        HttpResponse<String> response = support.exchangeFor(this::buildRequest, operationName,
                HttpResponse.BodyHandlers.ofString(), stringBody -> stringBody);
        T value = support.deserialize(response, responseType);
        return new Etagged<>(value, response.headers().firstValue(ETAG_HEADER).orElse(null));
    }

    /**
     * Execute and return the deserialized body together with the response
     * {@code Location} header — the absolute upload URL an attachment declaration
     * returns, to PUT the binary to via {@link #putAbsolute(String)}. The
     * {@code location} is {@code null} when the server sends no such header.
     */
    public <T> Located<T> fetchLocation(Class<T> responseType) {
        HttpResponse<String> response = support.exchangeFor(this::buildRequest, operationName,
                HttpResponse.BodyHandlers.ofString(), stringBody -> stringBody);
        T value = support.deserialize(response, responseType);
        return new Located<>(value, response.headers().firstValue(LOCATION_HEADER).orElse(null));
    }

    private HttpRequest.Builder buildRequest() {
        if (method == null) {
            throw new IllegalStateException(ERR_NO_VERB);
        }
        String fullPath = query.isEmpty() ? path : path + query.render();
        URI target = absoluteUrl != null ? URI.create(absoluteUrl) : support.uri(fullPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
                .timeout(support.runtime().readTimeout())
                .header(ACCEPT_HEADER, acceptMediaType)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + support.runtime().requireToken());
        if (acceptLanguage != null) {
            builder.header(ACCEPT_LANGUAGE_HEADER, acceptLanguage);
        }
        if (ifMatch != null) {
            builder.header(IF_MATCH_HEADER, ifMatch);
        }
        if (contentType != null) {
            builder.header(CONTENT_TYPE_HEADER, contentType);
        }
        return builder.method(method, bodyPublisher);
    }
}
