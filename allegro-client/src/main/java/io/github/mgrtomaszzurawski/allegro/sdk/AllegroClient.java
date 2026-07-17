/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.AllegroCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.UserAccount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.Orders;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account.UserAccountImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.orders.OrdersImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.auth.OAuth2TokenManager;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.AllegroHttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import java.net.http.HttpClient;
import java.util.Objects;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Single entry point of the Allegro Java SDK.
 *
 * <p>Pass credentials once — token acquisition, caching, proactive refresh,
 * refresh-token rotation, and 401 re-auth are handled internally. Domain
 * accessors ({@link #user()}, more per the task-division plan) expose
 * intent-named operations; no endpoint or transport detail leaks through.
 *
 * <pre>{@code
 * var credentials = DeviceCodeCredentials.of(clientId, clientSecret,
 *         auth -> System.out.println("Confirm at: " + auth.verificationUriComplete()));
 * try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
 *     CurrentUser currentUser = client.user().me();
 * }
 * }</pre>
 *
 * <p>User-scoped resources (including {@code user().me()}) need a user-context
 * grant (authorization-code or device); an app-only client-credentials token is
 * limited to public data.
 *
 * <p>Marked {@link org.apiguardian.api.API.Status#EXPERIMENTAL EXPERIMENTAL}:
 * the surface may break between {@code 0.x} releases until the {@code 1.0.0}
 * cut locks the contract.
 *
 * @since 0.1.0
 */
@API(status = API.Status.EXPERIMENTAL, since = "0.1.0")
public final class AllegroClient implements AutoCloseable {

    /**
     * Returned by {@link #sdkVersion()} when neither a module descriptor
     * version nor a JAR manifest is present (IDE/classes-dir runs).
     */
    private static final String VERSION_UNAVAILABLE = "unversioned";
    private static final String ERR_CREDENTIALS_NULL = "credentials must not be null";
    private static final String ERR_CONFIG_NULL = "config must not be null";
    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_CLOSED = "AllegroClient is closed";

    private final OAuth2TokenManager tokenManager;
    private final UserAccount userAccount;
    private final Orders orders;
    private volatile boolean closed;

    private AllegroClient(AllegroCredentials credentials, AllegroClientConfig config) {
        // Redirect.NEVER: every endpoint is a pinned Allegro host that never
        // legitimately redirects, and the JDK client would forward the
        // Authorization header to cross-host https targets under NORMAL.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        // JsonNullableModule is REQUIRED: generated *Raw DTOs wrap optional
        // fields in JsonNullable (live-probe finding 2026-07-17 — /me with a
        // company deserializes only with this module registered).
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.tokenManager = new OAuth2TokenManager(credentials, config.oauthBaseUrl(),
                httpClient, objectMapper, config.readTimeout());
        HttpRuntime runtime = new AllegroHttpRuntime(
                config.apiBaseUrl(),
                new RetryHandler(httpClient, config.retryPolicy()),
                objectMapper,
                config.readTimeout(),
                tokenManager,
                config.executionInterceptor());
        this.userAccount = new UserAccountImpl(runtime);
        this.orders = new OrdersImpl(runtime);
        // [append point: domain wiring] Each domain bucket appends its
        // accessor field construction here, one line per bucket, BACKLOG order.
    }

    /** Client with explicit configuration. */
    public static AllegroClient create(AllegroCredentials credentials, AllegroClientConfig config) {
        Objects.requireNonNull(credentials, ERR_CREDENTIALS_NULL);
        Objects.requireNonNull(config, ERR_CONFIG_NULL);
        return new AllegroClient(credentials, config);
    }

    /** Client with default configuration for the given environment. */
    public static AllegroClient create(AllegroCredentials credentials, AllegroEnvironment environment) {
        Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
        return create(credentials, AllegroClientConfig.defaults(environment));
    }

    /** Account and user information. */
    public UserAccount user() {
        ensureOpen();
        return userAccount;
    }

    /** Orders, payments and billing. */
    public Orders orders() {
        ensureOpen();
        return orders;
    }

    // [append point: domain accessors] Each domain bucket appends its public
    // accessor method here, one block per bucket, in BACKLOG order.

    /**
     * Refresh token currently held by the SDK (Allegro rotates it on every
     * refresh), or {@code null} for app-only credentials. Persist it between
     * sessions and pass it back via
     * {@code AuthorizationCodeCredentials.ofRefreshToken(...)} or
     * {@code DeviceCodeCredentials.ofRefreshToken(...)} to skip the browser or
     * device prompt next time.
     */
    public @Nullable String refreshToken() {
        ensureOpen();
        return tokenManager.currentRefreshToken();
    }

    /**
     * The SDK artefact version, read from the module descriptor (stamped by
     * the build) or the JAR manifest, both derived from the single version
     * source in {@code gradle.properties}.
     *
     * @return the semantic version of this SDK build (e.g. {@code 0.1.0-preview}),
     *     or {@code unversioned} when running outside a packaged JAR
     */
    public static String sdkVersion() {
        Package sdkPackage = AllegroClient.class.getPackage();
        return resolveVersion(
                AllegroClient.class.getModule().getDescriptor(),
                sdkPackage != null ? sdkPackage.getImplementationVersion() : null);
    }

    /** Pure resolution logic, testable with synthetic descriptors. */
    static String resolveVersion(java.lang.module.@Nullable ModuleDescriptor moduleDescriptor,
            @Nullable String implementationVersion) {
        if (moduleDescriptor != null) {
            var descriptorVersion = moduleDescriptor.version();
            if (descriptorVersion.isPresent()) {
                return descriptorVersion.get().toString();
            }
        }
        return implementationVersion != null ? implementationVersion : VERSION_UNAVAILABLE;
    }

    /**
     * Invalidates cached tokens and marks the client closed; subsequent
     * accessor calls throw {@link IllegalStateException}. The underlying JDK
     * {@code HttpClient} has no close() on Java 17 — its resources are
     * reclaimed by GC once unreferenced.
     */
    @Override
    public void close() {
        closed = true;
        tokenManager.invalidate();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_CLOSED);
        }
    }
}
