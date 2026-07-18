/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.account;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.SmartClassification;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Maps the Smart! classification response from a {@link JsonNode} rather than the
 * generated Layer-1 DTO.
 *
 * <p>A condition's {@code value}/{@code threshold} is polymorphic on the wire — a
 * number for a metric condition (e.g. {@code 1.5} days) but a boolean for a
 * pass/fail condition — while the generated
 * {@code SmartSellerClassificationReportConditionsInnerRaw} types both as
 * {@code BigDecimal}. Jackson therefore aborts the WHOLE response with a
 * {@code MismatchedInputException} the moment a boolean value appears
 * (live-caught 2026-07-18 on the sandbox seller; see
 * {@code KNOWN-SERVER-BEHAVIORS.md}). Reading the tree lets the SDK keep the
 * numeric value typed and drop a non-numeric one to {@code null} — the pass/fail
 * outcome is already carried by the condition's {@code fulfilled} flag.
 */
final class SmartClassificationMapper {

    private static final String FIELD_CLASSIFICATION = "classification";
    private static final String FIELD_FULFILLED = "fulfilled";
    private static final String FIELD_LAST_CHANGED = "lastChanged";
    private static final String FIELD_CONDITIONS = "conditions";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_THRESHOLD = "threshold";
    private static final String FIELD_REQUIRED = "required";
    private static final String FIELD_EXCLUDED_DELIVERY_METHODS = "excludedDeliveryMethods";
    private static final String FIELD_ID = "id";

    private SmartClassificationMapper() {
    }

    static SmartClassification toSmartClassification(JsonNode root) {
        JsonNode classification = root.path(FIELD_CLASSIFICATION);
        boolean fulfilled = classification.path(FIELD_FULFILLED).asBoolean(false);
        OffsetDateTime lastChanged = offsetDateTimeOrNull(classification.get(FIELD_LAST_CHANGED));

        List<SmartClassification.Condition> conditions = new ArrayList<>();
        for (JsonNode condition : arrayOrEmpty(root.get(FIELD_CONDITIONS))) {
            conditions.add(toCondition(condition));
        }

        List<String> excludedDeliveryMethodIds = new ArrayList<>();
        for (JsonNode method : arrayOrEmpty(root.get(FIELD_EXCLUDED_DELIVERY_METHODS))) {
            String id = textOrNull(method.get(FIELD_ID));
            if (id != null) {
                excludedDeliveryMethodIds.add(id);
            }
        }
        return new SmartClassification(fulfilled, lastChanged, conditions, excludedDeliveryMethodIds);
    }

    private static SmartClassification.Condition toCondition(JsonNode node) {
        return new SmartClassification.Condition(
                textOrNull(node.get(FIELD_CODE)),
                textOrNull(node.get(FIELD_NAME)),
                textOrNull(node.get(FIELD_DESCRIPTION)),
                numericOrNull(node.get(FIELD_VALUE)),
                numericOrNull(node.get(FIELD_THRESHOLD)),
                node.path(FIELD_FULFILLED).asBoolean(false),
                node.path(FIELD_REQUIRED).asBoolean(false));
    }

    /** The node's decimal value when it is a JSON number, otherwise {@code null}. */
    private static @Nullable BigDecimal numericOrNull(@Nullable JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private static @Nullable OffsetDateTime offsetDateTimeOrNull(@Nullable JsonNode node) {
        String text = textOrNull(node);
        return text == null ? null : OffsetDateTime.parse(text);
    }

    private static @Nullable String textOrNull(@Nullable JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Iterable<JsonNode> arrayOrEmpty(@Nullable JsonNode node) {
        return node != null && node.isArray() ? node : List.<JsonNode>of();
    }
}
