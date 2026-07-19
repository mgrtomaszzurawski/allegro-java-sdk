/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link ResponsiblePersonRequest}. Enforces the required
 * {@code name} and the wire length caps ({@code name} ≤ 50, {@code personName}
 * ≤ 200) fail-fast at {@link #build()}; the finer field rules are owned by the
 * server.
 *
 * @since 0.3.0
 */
public final class ResponsiblePersonRequestBuilder {

    /** Server cap on the internal dictionary name length. */
    private static final int MAX_NAME_LENGTH = 50;
    /** Server cap on the responsible person's name length. */
    private static final int MAX_PERSON_NAME_LENGTH = 200;

    private static final String ERR_NAME_REQUIRED = "Responsible person name is required";
    private static final String ERR_NAME_TOO_LONG =
            "Responsible person name exceeds the " + MAX_NAME_LENGTH + "-character limit";
    private static final String ERR_PERSON_NAME_TOO_LONG =
            "Responsible person's personName exceeds the " + MAX_PERSON_NAME_LENGTH + "-character limit";

    private @Nullable String name;
    private @Nullable String personName;
    private @Nullable ResponsiblePartyAddress address;
    private @Nullable ResponsiblePartyContact contact;

    ResponsiblePersonRequestBuilder() {
    }

    /** Set the internal dictionary label (required, at most 50 characters). */
    public ResponsiblePersonRequestBuilder name(@Nullable String dictionaryName) {
        this.name = dictionaryName;
        return this;
    }

    /** Set the responsible person's name (at most 200 characters). */
    public ResponsiblePersonRequestBuilder personName(@Nullable String responsiblePersonName) {
        this.personName = responsiblePersonName;
        return this;
    }

    /** Set the responsible person's address. */
    public ResponsiblePersonRequestBuilder address(@Nullable ResponsiblePartyAddress personAddress) {
        this.address = personAddress;
        return this;
    }

    /** Set the responsible person's contact. */
    public ResponsiblePersonRequestBuilder contact(@Nullable ResponsiblePartyContact personContact) {
        this.contact = personContact;
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if {@code name} is missing or a length cap is exceeded
     */
    public ResponsiblePersonRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (personName != null && personName.length() > MAX_PERSON_NAME_LENGTH) {
            throw new IllegalStateException(ERR_PERSON_NAME_TOO_LONG);
        }
        return new ResponsiblePersonRequest(name, personName, address, contact);
    }
}
