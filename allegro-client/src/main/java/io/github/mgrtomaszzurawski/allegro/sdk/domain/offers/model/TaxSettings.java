/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.OfferTaxSettingsRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An offer's VAT settings: the per-country rates, the tax {@code subject} the goods fall
 * under, and any {@code exemption} that applies.
 *
 * <p>The same immutable value is used both ways: build one to set an offer's tax settings on
 * {@code CreateOfferRequest}, or read one back from an {@link Offer}. Every field is optional;
 * {@code rates} is never {@code null} (empty when the payload omits it).
 *
 * @param subject   the tax subject the goods fall under (Allegro's tax-subject code), or {@code null}
 * @param exemption the tax exemption that applies (Allegro's exemption code), or {@code null}
 * @param rates     the declared VAT rates per marketplace country (empty when none)
 * @since 0.5.0
 */
public record TaxSettings(
        @Nullable String subject,
        @Nullable String exemption,
        List<TaxRate> rates) {

    /** Canonical constructor; normalizes {@code rates} to an immutable copy. */
    public TaxSettings {
        rates = List.copyOf(rates);
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder().subject(subject).exemption(exemption).rates(rates);
    }

    /**
     * Project a generated tax-settings block onto the consumer value.
     *
     * @param raw the generated block (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    public static @Nullable TaxSettings from(@Nullable OfferTaxSettingsRaw raw) {
        if (raw == null) {
            return null;
        }
        return new TaxSettings(raw.getSubject(), raw.getExemption(), ratesOf(raw));
    }

    private static List<TaxRate> ratesOf(OfferTaxSettingsRaw raw) {
        var rates = raw.getRates();
        return rates == null ? List.of()
                : rates.stream().map(TaxRate::from).filter(Objects::nonNull).toList();
    }

    /** The generated tax-settings block for this value (only set fields are written). */
    public OfferTaxSettingsRaw toRaw() {
        OfferTaxSettingsRaw raw = new OfferTaxSettingsRaw();
        if (subject != null) {
            raw.subject(subject);
        }
        if (exemption != null) {
            raw.exemption(exemption);
        }
        if (!rates.isEmpty()) {
            raw.rates(rates.stream().map(TaxRate::toRaw).toList());
        }
        return raw;
    }

    /** Fluent builder for {@link TaxSettings}. */
    public static final class Builder {

        private @Nullable String subject;
        private @Nullable String exemption;
        private List<TaxRate> rates = List.of();

        /** Set the tax subject the goods fall under. */
        public Builder subject(@Nullable String subject) {
            this.subject = subject;
            return this;
        }

        /** Set the tax exemption that applies. */
        public Builder exemption(@Nullable String exemption) {
            this.exemption = exemption;
            return this;
        }

        /** Replace the declared VAT rates with the given list (non-null). */
        public Builder rates(List<TaxRate> rates) {
            this.rates = List.copyOf(rates);
            return this;
        }

        /** Build the tax settings. */
        public TaxSettings build() {
            return new TaxSettings(subject, exemption, rates);
        }
    }
}
