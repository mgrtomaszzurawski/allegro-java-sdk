/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueMessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable request to add a message to a post-purchase issue. Allegro requires
 * the message to carry text, at least one attachment, or both; the
 * {@linkplain IssueMessageType type} defaults to {@link IssueMessageType#REGULAR}.
 *
 * <pre>{@code
 * IssueMessageRequest reply = IssueMessageRequest.builder()
 *         .text("Thank you, we are shipping a replacement today.")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class IssueMessageRequest {

    /** Server limit on the message text length. */
    public static final int TEXT_MAX_LENGTH = 20_000;

    private static final String ERR_EMPTY =
            "a message must carry text, at least one attachment, or both";
    private static final String ERR_TEXT_TOO_LONG =
            "text must be at most " + TEXT_MAX_LENGTH + " characters";
    private static final String ERR_TYPE_NULL = "type must not be null";
    private static final String ERR_ATTACHMENT_NULL = "attachment id must not be null";

    private final @Nullable String text;
    private final IssueMessageType type;
    private final List<String> attachmentIds;

    private IssueMessageRequest(Builder builder) {
        this.text = builder.text;
        this.type = builder.type;
        this.attachmentIds = List.copyOf(builder.attachmentIds);
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The message text, or {@code null} when the message is attachment-only. */
    public @Nullable String text() {
        return text;
    }

    /** The message type; never {@code null}. */
    public IssueMessageType type() {
        return type;
    }

    /** The referenced attachment ids; never {@code null}, possibly empty. */
    public List<String> attachmentIds() {
        return attachmentIds;
    }

    /** A builder pre-filled from this request. */
    public Builder toBuilder() {
        Builder builder = new Builder().text(text).type(type);
        builder.attachmentIds = new ArrayList<>(attachmentIds);
        return builder;
    }

    /** Fluent, fail-fast builder for {@link IssueMessageRequest}. */
    public static final class Builder {

        private @Nullable String text;
        private IssueMessageType type = IssueMessageType.REGULAR;
        private List<String> attachmentIds = new ArrayList<>();

        private Builder() {
        }

        /** The message text (optional if at least one attachment is present). */
        public Builder text(@Nullable String messageText) {
            this.text = messageText;
            return this;
        }

        /** The message type (defaults to {@link IssueMessageType#REGULAR}). */
        public Builder type(IssueMessageType messageType) {
            this.type = Objects.requireNonNull(messageType, ERR_TYPE_NULL);
            return this;
        }

        /** Reference a previously uploaded attachment by its id. */
        public Builder attachment(String attachmentId) {
            Objects.requireNonNull(attachmentId, ERR_ATTACHMENT_NULL);
            this.attachmentIds.add(attachmentId);
            return this;
        }

        /** Validate and build. */
        public IssueMessageRequest build() {
            boolean hasText = text != null && !text.isBlank();
            if (!hasText && attachmentIds.isEmpty()) {
                throw new IllegalStateException(ERR_EMPTY);
            }
            if (text != null && text.length() > TEXT_MAX_LENGTH) {
                throw new IllegalStateException(ERR_TEXT_TOO_LONG);
            }
            return new IssueMessageRequest(this);
        }
    }
}
