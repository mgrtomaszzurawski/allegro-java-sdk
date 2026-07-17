/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import java.util.List;
import java.util.Objects;

/**
 * How a {@link PricingRule} adjusts the followed price: either by a fixed
 * monetary amount (possibly one per marketplace currency) or by a percentage.
 * The configuration is optional — built-in default rules and pure follow rules
 * carry none.
 *
 * <p>A sealed hierarchy with exactly two shapes; consumers match on the concrete
 * type:
 * <pre>{@code
 * if (config instanceof PricingRuleConfiguration.ChangeByPercentage percentage) {
 *     ...
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
public sealed interface PricingRuleConfiguration {

    /**
     * The direction of the price adjustment.
     *
     * <p>{@code ADD} and {@code SUBTRACT} are Allegro spec enum values kept
     * verbatim (spec identifiers are exempt from the naming rules); the short
     * {@code ADD} constant is suppressed for that reason only.
     */
    @SuppressWarnings("PMD.ShortVariableWithDomainExceptions")
    enum Operation {

        /** Subtract the amount or percentage from the followed price. */
        SUBTRACT,

        /** Add the amount or percentage to the followed price. */
        ADD
    }

    /**
     * Adjust the followed price by a fixed monetary amount. One amount may be
     * given per marketplace currency.
     *
     * @param operation whether the amounts are added or subtracted
     * @param values the adjustment amount in each configured currency
     * @since 0.2.0
     */
    record ChangeByAmount(Operation operation, List<Money> values)
            implements PricingRuleConfiguration {

        private static final String ERR_OPERATION = "operation must not be null";

        /** Rejects a missing operation and defensively copies the amounts. */
        public ChangeByAmount {
            Objects.requireNonNull(operation, ERR_OPERATION);
            values = List.copyOf(values);
        }
    }

    /**
     * Adjust the followed price by a percentage.
     *
     * @param operation whether the percentage is added or subtracted
     * @param value the percentage as a decimal string (e.g. {@code "10"}), the
     *     exact form Allegro expects and returns
     * @since 0.2.0
     */
    record ChangeByPercentage(Operation operation, String value)
            implements PricingRuleConfiguration {

        private static final String ERR_OPERATION = "operation must not be null";
        private static final String ERR_VALUE = "value must not be null";

        /** Rejects a missing operation or percentage value. */
        public ChangeByPercentage {
            Objects.requireNonNull(operation, ERR_OPERATION);
            Objects.requireNonNull(value, ERR_VALUE);
        }
    }
}
