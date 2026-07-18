/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ClaimStatus;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable request to change the formal status of a claim — accept it (optionally
 * with a partial refund) or reject it with a documented reason. Valid only for
 * claims, never for disputes.
 *
 * <pre>{@code
 * ClaimStatusChange accept = ClaimStatusChange.builder()
 *         .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
 *         .message("We agree to a partial refund.")
 *         .partialRefund(Money.of("12.50", "PLN"))
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ClaimStatusChange {

    private static final String ERR_STATUS_NULL = "status must not be null";
    private static final String ERR_MESSAGE_REQUIRED = "message is required";

    private final ClaimStatus status;
    private final String message;
    private final @Nullable Money partialRefund;

    private ClaimStatusChange(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.partialRefund = builder.partialRefund;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The target status; never {@code null}. */
    public ClaimStatus status() {
        return status;
    }

    /** The message accompanying the change; never {@code null}. */
    public String message() {
        return message;
    }

    /** The partial-refund amount (for {@link ClaimStatus#ACCEPTED_PARTIAL_REFUND}), or {@code null}. */
    public @Nullable Money partialRefund() {
        return partialRefund;
    }

    /** A builder pre-filled from this change. */
    public Builder toBuilder() {
        return new Builder().status(status).message(message).partialRefund(partialRefund);
    }

    /** Fluent, fail-fast builder for {@link ClaimStatusChange}. */
    public static final class Builder {

        private @Nullable ClaimStatus status;
        private @Nullable String message;
        private @Nullable Money partialRefund;

        private Builder() {
        }

        /** The target status (required). */
        public Builder status(ClaimStatus targetStatus) {
            this.status = targetStatus;
            return this;
        }

        /** The message accompanying the status change (required). */
        public Builder message(String changeMessage) {
            this.message = changeMessage;
            return this;
        }

        /** The partial-refund amount; set it with {@link ClaimStatus#ACCEPTED_PARTIAL_REFUND}. */
        public Builder partialRefund(@Nullable Money amount) {
            this.partialRefund = amount;
            return this;
        }

        /** Validate the required fields and build. */
        public ClaimStatusChange build() {
            Objects.requireNonNull(status, ERR_STATUS_NULL);
            if (message == null || message.isBlank()) {
                throw new IllegalStateException(ERR_MESSAGE_REQUIRED);
            }
            return new ClaimStatusChange(this);
        }
    }
}
