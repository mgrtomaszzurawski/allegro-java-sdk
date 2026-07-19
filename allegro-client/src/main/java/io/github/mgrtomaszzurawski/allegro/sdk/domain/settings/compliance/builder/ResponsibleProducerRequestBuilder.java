/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for {@link ResponsibleProducerRequest}. Enforces the required
 * {@code name} and the wire length caps ({@code name} ≤ 50, {@code tradeName}
 * ≤ 200) fail-fast at {@link #build()}; the finer field rules are owned by the
 * server.
 *
 * @since 0.3.0
 */
public final class ResponsibleProducerRequestBuilder {

    /** Server cap on the internal dictionary name length. */
    private static final int MAX_NAME_LENGTH = 50;
    /** Server cap on the trade-name length. */
    private static final int MAX_TRADE_NAME_LENGTH = 200;

    private static final String ERR_NAME_REQUIRED = "Responsible producer name is required";
    private static final String ERR_NAME_TOO_LONG =
            "Responsible producer name exceeds the " + MAX_NAME_LENGTH + "-character limit";
    private static final String ERR_TRADE_NAME_TOO_LONG =
            "Responsible producer tradeName exceeds the " + MAX_TRADE_NAME_LENGTH + "-character limit";
    private static final String ERR_CONTACT_CHANNEL =
            "Responsible producer contact requires at least one of email or formUrl";

    private @Nullable String name;
    private @Nullable String tradeName;
    private @Nullable ResponsiblePartyAddress address;
    private @Nullable ResponsiblePartyContact contact;

    ResponsibleProducerRequestBuilder() {
    }

    /** Set the internal dictionary label (required, at most 50 characters). */
    public ResponsibleProducerRequestBuilder name(@Nullable String dictionaryName) {
        this.name = dictionaryName;
        return this;
    }

    /** Set the producing company's name or trade name (at most 200 characters). */
    public ResponsibleProducerRequestBuilder tradeName(@Nullable String producerTradeName) {
        this.tradeName = producerTradeName;
        return this;
    }

    /** Set the producer's address. */
    public ResponsibleProducerRequestBuilder address(@Nullable ResponsiblePartyAddress producerAddress) {
        this.address = producerAddress;
        return this;
    }

    /** Set the producer's contact. */
    public ResponsibleProducerRequestBuilder contact(@Nullable ResponsiblePartyContact producerContact) {
        this.contact = producerContact;
        return this;
    }

    /**
     * Validate and build the immutable request.
     *
     * @throws IllegalStateException if {@code name} is missing or a length cap is exceeded
     */
    public ResponsibleProducerRequest build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(ERR_NAME_REQUIRED);
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException(ERR_NAME_TOO_LONG);
        }
        if (tradeName != null && tradeName.length() > MAX_TRADE_NAME_LENGTH) {
            throw new IllegalStateException(ERR_TRADE_NAME_TOO_LONG);
        }
        if (contact != null && isBlank(contact.email()) && isBlank(contact.formUrl())) {
            throw new IllegalStateException(ERR_CONTACT_CHANNEL);
        }
        return new ResponsibleProducerRequest(name, tradeName, address, contact);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
