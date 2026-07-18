/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.orders.model.ReturnRejectionCode;
import org.jspecify.annotations.Nullable;

/**
 * A seller's rejection of a customer return's refund, passed to
 * {@code orders().returns().rejectRefund(...)}. The {@link ReturnRejectionCode}
 * is required; the free-text reason is optional.
 *
 * @since 0.6.0
 */
public final class RejectionRequest {

    private static final String ERR_CODE = "code is required";

    private final ReturnRejectionCode code;
    private final @Nullable String reason;

    private RejectionRequest(Builder builder) {
        if (builder.code == null) {
            throw new IllegalStateException(ERR_CODE);
        }
        this.code = builder.code;
        this.reason = builder.reason;
    }

    /** The rejection code. */
    public ReturnRejectionCode code() {
        return code;
    }

    /** Optional free-text reason, or {@code null}. */
    public @Nullable String reason() {
        return reason;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder().code(code).reason(reason);
    }

    /** Fluent builder for {@link RejectionRequest}. */
    public static final class Builder {

        private @Nullable ReturnRejectionCode code;
        private @Nullable String reason;

        /** Set the rejection code (required). */
        public Builder code(@Nullable ReturnRejectionCode value) {
            this.code = value;
            return this;
        }

        /** Set the optional free-text reason. */
        public Builder reason(@Nullable String value) {
            this.reason = value;
            return this;
        }

        /**
         * Build the request.
         *
         * @throws IllegalStateException if the code is missing
         */
        public RejectionRequest build() {
            return new RejectionRequest(this);
        }
    }
}
