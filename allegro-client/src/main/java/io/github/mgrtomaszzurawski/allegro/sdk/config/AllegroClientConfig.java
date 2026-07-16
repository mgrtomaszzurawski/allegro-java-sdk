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
 *
 * @since 0.1.0
 */
public record AllegroClientConfig(
        AllegroEnvironment environment,
        RetryPolicy retryPolicy,
        Duration connectTimeout,
        Duration readTimeout) {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final String ERR_ENVIRONMENT_NULL = "environment must not be null";
    private static final String ERR_RETRY_POLICY_NULL = "retryPolicy must not be null";
    private static final String ERR_CONNECT_TIMEOUT_INVALID = "connectTimeout must be positive, got: ";
    private static final String ERR_READ_TIMEOUT_INVALID = "readTimeout must be positive, got: ";

    public AllegroClientConfig {
        Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
        Objects.requireNonNull(retryPolicy, ERR_RETRY_POLICY_NULL);
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException(ERR_CONNECT_TIMEOUT_INVALID + connectTimeout);
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException(ERR_READ_TIMEOUT_INVALID + readTimeout);
        }
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

        private Builder(AllegroEnvironment environment) {
            this.environment = Objects.requireNonNull(environment, ERR_ENVIRONMENT_NULL);
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

        public AllegroClientConfig build() {
            return new AllegroClientConfig(environment, retryPolicy, connectTimeout, readTimeout);
        }
    }
}
