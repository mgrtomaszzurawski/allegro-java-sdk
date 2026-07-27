/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ConstraintCriteriaRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The constraint under which an additional-service configuration applies: a
 * country, a {@link ServiceConstraintType}, and — for delivery-realised services
 * — the ids of the delivery methods that can realise it.
 *
 * @param country ISO country code the configuration applies to, or {@code null}
 * @param type the constraint type, or {@code null}
 * @param deliveryMethodIds delivery-method ids (empty unless the type is
 *     {@code COUNTRY_DELIVERY_SAME_QUANTITY})
 *
 * @since 0.3.0
 */
public record ServiceConstraint(
        @Nullable String country,
        @Nullable ServiceConstraintType type,
        List<String> deliveryMethodIds) {

    /** Canonical constructor — defensively copies the delivery-method ids. */
    public ServiceConstraint {
        deliveryMethodIds = deliveryMethodIds == null ? List.of() : List.copyOf(deliveryMethodIds);
    }

    /** Map the generated Layer-1 DTO, or {@code null} when absent. */
    public static @Nullable ServiceConstraint from(@Nullable ConstraintCriteriaRaw raw) {
        if (raw == null) {
            return null;
        }
        ServiceConstraintType type = raw.getType() == null ? null : ServiceConstraintType.from(raw.getType());
        List<JustIdRaw> methods = raw.getDeliveryMethods() == null ? List.of() : raw.getDeliveryMethods();
        List<String> methodIds = methods.stream().map(JustIdRaw::getId).toList();
        return new ServiceConstraint(raw.getCountry(), type, methodIds);
    }
}
