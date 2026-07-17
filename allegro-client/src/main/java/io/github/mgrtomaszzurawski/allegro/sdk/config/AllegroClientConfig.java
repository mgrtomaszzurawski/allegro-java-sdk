/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.config;

import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration of an {@code AllegroClient} instance — everything
 * except the credentials (which are passed separately to the client builder).
 *
 * @param environment target environment (production or sandbox)
 * @param retryPolicy retry behaviour for transient failures
 * @param connectTimeout TCP/TLS connection establishment timeout
 * @param readTimeout per-request response timeout
 * @param executionInterceptor hook around every SDK HTTP execution (metrics
 *     seam; defaults to a no-op)
 * @param apiBaseUrl REST API base URL — defaults to the environment's; override
 *     only for tests or proxies (no trailing slash)
 * @param oauthBaseUrl OAuth2 endpoint base — defaults to the environment's;
 *     override only for tests or proxies (no trailing slash)
 *
 * @since 0.1.0
 */
public record AllegroClientConfig(
        AllegroEnvironment environment,
        RetryPolicy retryPolicy,
        Duration connectTimeout,
        Duration readTimeout,
        AllegroExecutionInterceptor executionInterceptor,
        String apiBaseUrl,
        String oauthBaseUrl) {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_RETRY_POLICY_NULL = "retryPolicy must not be null";
    private static final String ERR_CONNECT_TIMEOUT_INVALID = "connectTimeout must be positive, got: ";
    private static final String ERR_READ_TIMEOUT_INVALID = "readTimeout must be positive, got: ";
    private static final String ERR_INTERCEPTOR_NULL = "executionInterceptor must not be null";
    private static final String ERR_API_BASE_NULL = "apiBaseUrl must not be null";
    private static final String ERR_OAUTH_BASE_NULL = "oauthBaseUrl must not be null";

    public AllegroClientConfig {
        Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
        Objects.requireNonNull(retryPolicy, ERR_RETRY_POLICY_NULL);
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException(ERR_CONNECT_TIMEOUT_INVALID + connectTimeout);
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException(ERR_READ_TIMEOUT_INVALID + readTimeout);
        }
        Objects.requireNonNull(executionInterceptor, ERR_INTERCEPTOR_NULL);
        Objects.requireNonNull(apiBaseUrl, ERR_API_BASE_NULL);
        Objects.requireNonNull(oauthBaseUrl, ERR_OAUTH_BASE_NULL);
    }

    /** Configuration with all defaults for the given environment. */
    public static AllegroClientConfig defaults(AllegroEnvironment environment) {
        return builder(environment).build();
    }

    public static Builder builder(AllegroEnvironment environment) {
        return new Builder(environment);
    }

    public static final class Builder {

        private final AllegroEnvironment environment;
        private RetryPolicy retryPolicy = RetryPolicy.defaults();
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private AllegroExecutionInterceptor executionInterceptor = AllegroExecutionInterceptor.noop();
        private String apiBaseUrl;
        private String oauthBaseUrl;

        private Builder(AllegroEnvironment environment) {
            this.environment = Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
            this.apiBaseUrl = environment.apiBaseUrl();
            this.oauthBaseUrl = environment.oauthBaseUrl();
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder executionInterceptor(AllegroExecutionInterceptor executionInterceptor) {
            this.executionInterceptor = executionInterceptor;
            return this;
        }

        /** Test/proxy hook — points REST calls somewhere else (e.g. WireMock). */
        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        /** Test/proxy hook — points OAuth2 calls somewhere else (e.g. WireMock). */
        public Builder oauthBaseUrl(String oauthBaseUrl) {
            this.oauthBaseUrl = oauthBaseUrl;
            return this;
        }

        public AllegroClientConfig build() {
            return new AllegroClientConfig(environment, retryPolicy, connectTimeout, readTimeout,
                    executionInterceptor, apiBaseUrl, oauthBaseUrl);
        }
    }
}
