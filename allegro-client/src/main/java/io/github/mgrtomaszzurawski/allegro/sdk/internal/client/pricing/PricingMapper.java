/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountValuesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRulePostRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRulePutRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleTypeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRulesResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Package-private mapper between the pricing domain records and the Allegro
 * wire form.
 *
 * <p>Both directions use the generated Layer-1 DTOs. The {@code configuration}
 * field is a structural {@code oneOf} ({@code changeByAmount} vs
 * {@code changeByPercentage}); the shared {@code StrictOneOfModule} runtime core
 * resolves it by strict property matching, so reads deserialize straight into
 * {@link AutomaticPricingRuleConfigurationRaw} without the earlier hand-rolled
 * {@code JsonNode} discrimination.
 */
final class PricingMapper {

    private static final String OPERATION_ADD = "ADD";
    private static final String OPERATION_SUBTRACT = "SUBTRACT";
    private static final String ERR_UNKNOWN_CONFIGURATION =
            "Unknown pricing rule configuration variant: ";
    private static final String ERR_UNKNOWN_TYPE =
            "PricingRuleType.UNKNOWN is a read-only forward-compatibility value and "
                    + "cannot be used to create or edit a rule";

    private PricingMapper() {
    }

    /** Map a rule-creation request to the generated POST body DTO. */
    static AutomaticPricingRulePostRequestRaw toRaw(PricingRuleRequest request) {
        AutomaticPricingRulePostRequestRaw raw = new AutomaticPricingRulePostRequestRaw()
                .name(request.name())
                .type(typeToRaw(request.type()));
        PricingRuleConfiguration configuration = request.configuration();
        if (configuration != null) {
            raw.setConfiguration(configurationToRaw(configuration));
        }
        return raw;
    }

    /** Map a rule-edit request to the generated PUT body DTO (no {@code type}). */
    static AutomaticPricingRulePutRequestRaw editToRaw(PricingRuleEdit edit) {
        AutomaticPricingRulePutRequestRaw raw = new AutomaticPricingRulePutRequestRaw()
                .name(edit.name());
        PricingRuleConfiguration configuration = edit.configuration();
        if (configuration != null) {
            raw.setConfiguration(configurationToRaw(configuration));
        }
        return raw;
    }

    /** Map a rules-list response to public records. */
    static List<PricingRule> toRules(AutomaticPricingRulesResponseRaw response) {
        return response.getRules().stream().map(PricingMapper::toRule).toList();
    }

    /** Map a single rule response to the public record. */
    static PricingRule toRule(AutomaticPricingRuleResponseRaw raw) {
        return new PricingRule(
                raw.getId(),
                typeFrom(raw.getType()),
                raw.getName(),
                raw.getDefault(),
                raw.getUpdatedAt().toInstant(),
                configurationFrom(raw.getConfiguration()));
    }

    private static PricingRuleType typeFrom(@Nullable AutomaticPricingRuleTypeRaw raw) {
        if (raw == null) {
            // Defensive: `type` is spec-required, but a value absent from the
            // payload still degrades to UNKNOWN rather than NPEing the switch.
            return PricingRuleType.UNKNOWN;
        }
        return switch (raw) {
            case EXCHANGE_RATE -> PricingRuleType.EXCHANGE_RATE;
            case FOLLOW_BY_ALLEGRO_MIN_PRICE -> PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE;
            case FOLLOW_BY_MARKET_MIN_PRICE -> PricingRuleType.FOLLOW_BY_MARKET_MIN_PRICE;
            case FOLLOW_BY_TOP_OFFER_PRICE -> PricingRuleType.FOLLOW_BY_TOP_OFFER_PRICE;
            // A wire value this release does not model deserializes to the
            // generator's UNKNOWN_DEFAULT_OPEN_API sentinel; surface it as UNKNOWN.
            case UNKNOWN_DEFAULT_OPEN_API -> PricingRuleType.UNKNOWN;
        };
    }

    private static @Nullable PricingRuleConfiguration configurationFrom(
            @Nullable AutomaticPricingRuleConfigurationRaw raw) {
        if (raw == null) {
            return null;
        }
        Object actual = raw.getActualInstance();
        if (actual instanceof AutomaticPricingRuleConfigurationChangeByAmountRaw amountWrapper) {
            AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw amount =
                    amountWrapper.getChangeByAmount();
            PricingRuleConfiguration.Operation operation = operationFrom(
                    amount.getOperation() == null ? null : amount.getOperation().getValue());
            if (operation == null) {
                return null;
            }
            List<Money> values = amount.getValues().stream()
                    .map(value -> Money.of(value.getAmount(), value.getCurrency()))
                    .toList();
            return new PricingRuleConfiguration.ChangeByAmount(operation, values);
        }
        if (actual instanceof AutomaticPricingRuleConfigurationChangeByPercentageRaw percentageWrapper) {
            AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw percentage =
                    percentageWrapper.getChangeByPercentage();
            PricingRuleConfiguration.Operation operation = operationFrom(
                    percentage.getOperation() == null ? null : percentage.getOperation().getValue());
            if (operation == null) {
                return null;
            }
            return new PricingRuleConfiguration.ChangeByPercentage(operation, percentage.getValue());
        }
        // Forward-compat: a configuration shape the SDK does not model yet is
        // surfaced as "no adjustment" rather than failing the whole read.
        return null;
    }

    private static PricingRuleConfiguration.@Nullable Operation operationFrom(
            @Nullable String wireOperation) {
        if (wireOperation == null) {
            return null;
        }
        return switch (wireOperation) {
            case OPERATION_ADD -> PricingRuleConfiguration.Operation.ADD;
            case OPERATION_SUBTRACT -> PricingRuleConfiguration.Operation.SUBTRACT;
            default -> null;
        };
    }

    private static AutomaticPricingRuleTypeRaw typeToRaw(PricingRuleType type) {
        return switch (type) {
            case EXCHANGE_RATE -> AutomaticPricingRuleTypeRaw.EXCHANGE_RATE;
            case FOLLOW_BY_ALLEGRO_MIN_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_ALLEGRO_MIN_PRICE;
            case FOLLOW_BY_MARKET_MIN_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_MARKET_MIN_PRICE;
            case FOLLOW_BY_TOP_OFFER_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_TOP_OFFER_PRICE;
            case UNKNOWN -> throw new IllegalArgumentException(ERR_UNKNOWN_TYPE);
        };
    }

    private static AutomaticPricingRuleConfigurationRaw configurationToRaw(
            PricingRuleConfiguration configuration) {
        if (configuration instanceof PricingRuleConfiguration.ChangeByAmount amount) {
            AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw amountInner =
                    new AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw()
                            .operation(AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw
                                    .OperationEnum.valueOf(amount.operation().name()))
                            .values(amount.values().stream().map(PricingMapper::amountToRaw).toList());
            return new AutomaticPricingRuleConfigurationRaw(
                    new AutomaticPricingRuleConfigurationChangeByAmountRaw().changeByAmount(amountInner));
        }
        if (!(configuration instanceof PricingRuleConfiguration.ChangeByPercentage percentage)) {
            // Unreachable while the sealed hierarchy has two variants; guards a
            // future variant with a clear failure instead of an unchecked cast.
            throw new IllegalStateException(
                    ERR_UNKNOWN_CONFIGURATION + configuration.getClass().getName());
        }
        AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw percentageInner =
                new AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw()
                        .operation(AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw
                                .OperationEnum.valueOf(percentage.operation().name()))
                        .value(percentage.value());
        return new AutomaticPricingRuleConfigurationRaw(
                new AutomaticPricingRuleConfigurationChangeByPercentageRaw()
                        .changeByPercentage(percentageInner));
    }

    private static AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountValuesInnerRaw amountToRaw(
            Money money) {
        return new AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountValuesInnerRaw()
                .amount(money.amountAsDecimal())
                .currency(money.currency());
    }
}
