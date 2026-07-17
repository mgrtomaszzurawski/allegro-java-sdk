/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable request describing a seller contact card to create or update.
 *
 * <p>Built through the fluent {@link Builder}; every setter validates the
 * server-side limits fail-fast (name ≤ 250 characters, at most one e-mail
 * ≤ 128 characters, at most two phone numbers ≤ 250 characters each), so an
 * invalid card is rejected before the request leaves the process. The Allegro
 * contract marks no field as required, so an empty request is legal.
 *
 * <pre>{@code
 * ContactRequest request = ContactRequest.builder()
 *         .name("Main contact")
 *         .email("shop@example.com")
 *         .phone("+48512323495")
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class ContactRequest {

    private static final int NAME_MAX_LENGTH = 250;
    private static final int EMAIL_MAX_LENGTH = 128;
    private static final int PHONE_MAX_LENGTH = 250;
    private static final int MAX_PHONES = 2;

    private static final String ERR_NAME_NULL = "name must not be null";
    private static final String ERR_EMAIL_NULL = "email address must not be null";
    private static final String ERR_PHONE_NULL = "phone number must not be null";
    private static final String ERR_NAME_LENGTH =
            "name must be at most " + NAME_MAX_LENGTH + " characters";
    private static final String ERR_EMAIL_LENGTH =
            "email address must be at most " + EMAIL_MAX_LENGTH + " characters";
    private static final String ERR_PHONE_LENGTH =
            "phone number must be at most " + PHONE_MAX_LENGTH + " characters";
    private static final String ERR_TOO_MANY_PHONES =
            "a contact card accepts at most " + MAX_PHONES + " phone numbers";

    private final @Nullable String name;
    private final List<String> emails;
    private final List<String> phones;

    private ContactRequest(Builder builder) {
        this.name = builder.name;
        this.emails = List.copyOf(builder.emails);
        this.phones = List.copyOf(builder.phones);
    }

    /** A new, empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Display name, or {@code null} when unset. */
    public @Nullable String name() {
        return name;
    }

    /** E-mail addresses (zero or one); never {@code null}. */
    public List<String> emails() {
        return emails;
    }

    /** Phone numbers (zero to two); never {@code null}. */
    public List<String> phones() {
        return phones;
    }

    /** A builder pre-populated with this request's values, for edits. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.name = name;
        builder.emails = new ArrayList<>(emails);
        builder.phones = new ArrayList<>(phones);
        return builder;
    }

    /** Fluent, fail-fast builder for {@link ContactRequest}. */
    public static final class Builder {

        private @Nullable String name;
        private List<String> emails = new ArrayList<>();
        private List<String> phones = new ArrayList<>();

        private Builder() {
        }

        /** Sets the display name (≤ 250 characters). */
        public Builder name(String contactName) {
            Objects.requireNonNull(contactName, ERR_NAME_NULL);
            if (contactName.length() > NAME_MAX_LENGTH) {
                throw new IllegalArgumentException(ERR_NAME_LENGTH);
            }
            this.name = contactName;
            return this;
        }

        /**
         * Sets the single e-mail address (≤ 128 characters). The contract allows
         * at most one, so a later call replaces the previous value.
         */
        public Builder email(String address) {
            Objects.requireNonNull(address, ERR_EMAIL_NULL);
            if (address.length() > EMAIL_MAX_LENGTH) {
                throw new IllegalArgumentException(ERR_EMAIL_LENGTH);
            }
            this.emails = new ArrayList<>(List.of(address));
            return this;
        }

        /** Adds a phone number (≤ 250 characters); at most two are accepted. */
        public Builder phone(String number) {
            Objects.requireNonNull(number, ERR_PHONE_NULL);
            if (number.length() > PHONE_MAX_LENGTH) {
                throw new IllegalArgumentException(ERR_PHONE_LENGTH);
            }
            if (phones.size() >= MAX_PHONES) {
                throw new IllegalArgumentException(ERR_TOO_MANY_PHONES);
            }
            this.phones.add(number);
            return this;
        }

        /** Builds the immutable request. */
        public ContactRequest build() {
            return new ContactRequest(this);
        }
    }
}
