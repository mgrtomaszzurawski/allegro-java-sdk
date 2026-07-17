/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountChangeByAmountValuesInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByAmountRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByPercentageChangeByPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationChangeByPercentageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleConfigurationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRulePostRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AutomaticPricingRuleTypeRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Package-private mapper between the pricing domain records and the Allegro
 * wire form.
 *
 * <p><strong>Writes</strong> use the generated Layer-1 request DTOs (they
 * serialize the {@code configuration} oneOf correctly). <strong>Reads</strong>
 * of the rule response are mapped from a {@link JsonNode} rather than the
 * generated response DTO: the generated {@code native} oneOf deserializer does
 * not validate schema constraints, so under the SDK's forward-compatible
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} mapper a {@code changeByPercentage}
 * payload also matches the {@code changeByAmount} branch and deserialization
 * fails with "2 classes match". Discriminating on the present property name is
 * both correct and immune to that generator flaw. See the shared BACKLOG core
 * item for the systemic fix that would let reads move back to the DTO.
 */
final class PricingMapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DEFAULT = "default";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_CONFIGURATION = "configuration";
    private static final String FIELD_CHANGE_BY_AMOUNT = "changeByAmount";
    private static final String FIELD_CHANGE_BY_PERCENTAGE = "changeByPercentage";
    private static final String FIELD_OPERATION = "operation";
    private static final String FIELD_VALUES = "values";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_CURRENCY = "currency";
    private static final String ERR_UNKNOWN_CONFIGURATION =
            "Unknown pricing rule configuration variant: ";

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

    /** Map a rule response (as raw JSON) to the public record. */
    static PricingRule toRule(JsonNode node) {
        return new PricingRule(
                node.get(FIELD_ID).asText(),
                PricingRuleType.valueOf(node.get(FIELD_TYPE).asText()),
                node.get(FIELD_NAME).asText(),
                node.get(FIELD_DEFAULT).asBoolean(),
                Instant.parse(node.get(FIELD_UPDATED_AT).asText()),
                configurationFrom(node.get(FIELD_CONFIGURATION)));
    }

    private static @Nullable PricingRuleConfiguration configurationFrom(@Nullable JsonNode configuration) {
        if (configuration == null || configuration.isNull()) {
            return null;
        }
        JsonNode amountNode = configuration.get(FIELD_CHANGE_BY_AMOUNT);
        if (amountNode != null) {
            List<Money> values = new ArrayList<>();
            for (JsonNode entry : amountNode.get(FIELD_VALUES)) {
                values.add(Money.of(entry.get(FIELD_AMOUNT).asText(), entry.get(FIELD_CURRENCY).asText()));
            }
            return new PricingRuleConfiguration.ChangeByAmount(
                    operationFrom(amountNode.get(FIELD_OPERATION)), values);
        }
        JsonNode percentageNode = configuration.get(FIELD_CHANGE_BY_PERCENTAGE);
        if (percentageNode != null) {
            return new PricingRuleConfiguration.ChangeByPercentage(
                    operationFrom(percentageNode.get(FIELD_OPERATION)),
                    percentageNode.get(FIELD_VALUE).asText());
        }
        // Forward-compat: a configuration shape the SDK does not model yet is
        // surfaced as "no adjustment" rather than failing the whole read.
        return null;
    }

    private static PricingRuleConfiguration.Operation operationFrom(JsonNode operation) {
        return PricingRuleConfiguration.Operation.valueOf(operation.asText());
    }

    private static AutomaticPricingRuleTypeRaw typeToRaw(PricingRuleType type) {
        return switch (type) {
            case EXCHANGE_RATE -> AutomaticPricingRuleTypeRaw.EXCHANGE_RATE;
            case FOLLOW_BY_ALLEGRO_MIN_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_ALLEGRO_MIN_PRICE;
            case FOLLOW_BY_MARKET_MIN_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_MARKET_MIN_PRICE;
            case FOLLOW_BY_TOP_OFFER_PRICE -> AutomaticPricingRuleTypeRaw.FOLLOW_BY_TOP_OFFER_PRICE;
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
