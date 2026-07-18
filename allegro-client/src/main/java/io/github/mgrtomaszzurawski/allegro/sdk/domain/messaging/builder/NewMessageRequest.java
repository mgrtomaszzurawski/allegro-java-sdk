/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable request that opens a new message thread. The Allegro contract
 * requires a recipient, an order context, and the message text; attachments
 * (declared and uploaded beforehand) are optional.
 *
 * <pre>{@code
 * NewMessageRequest request = NewMessageRequest.builder()
 *         .recipientLogin("buyer-login")
 *         .orderId("a8f8b6e0-...")
 *         .text("Thanks for your order — it ships today.")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class NewMessageRequest {

    /** Server limit on the message body length. */
    public static final int TEXT_MAX_LENGTH = 2000;

    private static final String ERR_RECIPIENT_REQUIRED = "recipientLogin is required";
    private static final String ERR_ORDER_REQUIRED = "orderId is required";
    private static final String ERR_TEXT_REQUIRED = "text is required";
    private static final String ERR_TEXT_TOO_LONG =
            "text must be at most " + TEXT_MAX_LENGTH + " characters";
    private static final String ERR_ATTACHMENT_NULL = "attachment id must not be null";

    private final String recipientLogin;
    private final String orderId;
    private final String text;
    private final List<String> attachmentIds;

    private NewMessageRequest(Builder builder) {
        this.recipientLogin = builder.recipientLogin;
        this.orderId = builder.orderId;
        this.text = builder.text;
        this.attachmentIds = List.copyOf(builder.attachmentIds);
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The recipient's public login. */
    public String recipientLogin() {
        return recipientLogin;
    }

    /** The order (checkout form) id the thread is opened against. */
    public String orderId() {
        return orderId;
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
        builder.recipientLogin = recipientLogin;
        builder.orderId = orderId;
        builder.text = text;
        builder.attachmentIds = new ArrayList<>(attachmentIds);
        return builder;
    }

    /** Fluent, fail-fast builder for {@link NewMessageRequest}. */
    public static final class Builder {

        private String recipientLogin;
        private String orderId;
        private String text;
        private List<String> attachmentIds = new ArrayList<>();

        private Builder() {
        }

        /** The recipient's public login (required). */
        public Builder recipientLogin(String login) {
            this.recipientLogin = login;
            return this;
        }

        /** The order (checkout form) id the thread relates to (required). */
        public Builder orderId(String checkoutFormId) {
            this.orderId = checkoutFormId;
            return this;
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

        /** Validate the required fields and build. */
        public NewMessageRequest build() {
            if (recipientLogin == null || recipientLogin.isBlank()) {
                throw new IllegalStateException(ERR_RECIPIENT_REQUIRED);
            }
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalStateException(ERR_ORDER_REQUIRED);
            }
            if (text == null || text.isBlank()) {
                throw new IllegalStateException(ERR_TEXT_REQUIRED);
            }
            if (text.length() > TEXT_MAX_LENGTH) {
                throw new IllegalStateException(ERR_TEXT_TOO_LONG);
            }
            return new NewMessageRequest(this);
        }
    }
}
