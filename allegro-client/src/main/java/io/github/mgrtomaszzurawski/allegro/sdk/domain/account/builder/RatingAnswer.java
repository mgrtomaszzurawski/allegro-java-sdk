/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder;

/**
 * Request to publish a seller's answer to a received rating, built fluently.
 *
 * <pre>{@code
 * RatingAnswer answer = RatingAnswer.builder()
 *         .message("Thank you for your feedback!")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class RatingAnswer {

    /** Server limit on the answer message length. */
    public static final int MAX_MESSAGE_LENGTH = 500;

    private static final String ERR_MESSAGE_REQUIRED = "message is required";
    private static final String ERR_MESSAGE_TOO_LONG =
            "message must be at most " + MAX_MESSAGE_LENGTH + " characters";

    private final String message;

    private RatingAnswer(Builder builder) {
        this.message = builder.message;
    }

    /** The answer text. */
    public String message() {
        return message;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        return new Builder().message(message);
    }

    /** Fluent builder for {@link RatingAnswer}. */
    public static final class Builder {

        private String message;

        /** The answer text (required, at most {@value #MAX_MESSAGE_LENGTH} characters). */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /** Validate and build. */
        public RatingAnswer build() {
            if (message == null || message.isBlank()) {
                throw new IllegalStateException(ERR_MESSAGE_REQUIRED);
            }
            if (message.length() > MAX_MESSAGE_LENGTH) {
                throw new IllegalStateException(ERR_MESSAGE_TOO_LONG);
            }
            return new RatingAnswer(this);
        }
    }
}
