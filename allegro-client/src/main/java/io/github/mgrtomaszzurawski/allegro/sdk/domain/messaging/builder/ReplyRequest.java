/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable request that adds a message to an existing thread. Only the text is
 * required; attachments (declared and uploaded beforehand) are optional.
 *
 * <pre>{@code
 * ReplyRequest reply = ReplyRequest.builder()
 *         .text("Your parcel is on the way.")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ReplyRequest {

    /** Server limit on the message body length. */
    public static final int TEXT_MAX_LENGTH = 2000;

    private static final String ERR_TEXT_REQUIRED = "text is required";
    private static final String ERR_TEXT_TOO_LONG =
            "text must be at most " + TEXT_MAX_LENGTH + " characters";
    private static final String ERR_ATTACHMENT_NULL = "attachment id must not be null";

    private final String text;
    private final List<String> attachmentIds;

    private ReplyRequest(Builder builder) {
        this.text = builder.text;
        this.attachmentIds = List.copyOf(builder.attachmentIds);
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The message body. */
    public String text() {
        return text;
    }

    /** Ids of previously declared/uploaded attachments; never {@code null}. */
    public List<String> attachmentIds() {
        return attachmentIds;
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.text = text;
        builder.attachmentIds = new ArrayList<>(attachmentIds);
        return builder;
    }

    /** Fluent, fail-fast builder for {@link ReplyRequest}. */
    public static final class Builder {

        private String text;
        private List<String> attachmentIds = new ArrayList<>();

        private Builder() {
        }

        /** The message body (required, at most {@value #TEXT_MAX_LENGTH} characters). */
        public Builder text(String body) {
            this.text = body;
            return this;
        }

        /** Adds the id of a previously declared/uploaded attachment. */
        public Builder attachment(String attachmentId) {
            Objects.requireNonNull(attachmentId, ERR_ATTACHMENT_NULL);
            this.attachmentIds.add(attachmentId);
            return this;
        }

        /** Validate the required field and build. */
        public ReplyRequest build() {
            if (text == null || text.isBlank()) {
                throw new IllegalStateException(ERR_TEXT_REQUIRED);
            }
            if (text.length() > TEXT_MAX_LENGTH) {
                throw new IllegalStateException(ERR_TEXT_TOO_LONG);
            }
            return new ReplyRequest(this);
        }
    }
}
