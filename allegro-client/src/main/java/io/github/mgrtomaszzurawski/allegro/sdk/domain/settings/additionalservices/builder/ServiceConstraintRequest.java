/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.builder;

import io.github.mgrtomaszzurawski.allegro.client.model.ConstraintCriteriaRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.JustIdRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.additionalservices.model.ServiceConstraintType;
import java.util.List;
import java.util.Objects;

/**
 * The constraint under which a {@link ServiceConfigurationRequest} price applies, in a
 * group create/update request: the {@code country} the price applies to, the
 * {@link ServiceConstraintType type}, and — only for
 * {@link ServiceConstraintType#COUNTRY_DELIVERY_SAME_QUANTITY} services realised in
 * delivery — the ids of the delivery methods that can realise the service.
 *
 * <p>Allegro requires a matching constraint for every configuration: services realised
 * before shipping (e.g. {@code GIFT_WRAP}) use {@link #beforeShipping(String)}, services
 * realised in delivery (e.g. {@code CARRY_IN}) use
 * {@link #inDelivery(String, List)}.
 *
 * @param country the ISO country code the price applies to
 * @param type the constraint type
 * @param deliveryMethodIds delivery-method ids (empty unless the type is
 *     {@link ServiceConstraintType#COUNTRY_DELIVERY_SAME_QUANTITY})
 * @since 0.3.0
 */
public record ServiceConstraintRequest(
        String country,
        ServiceConstraintType type,
        List<String> deliveryMethodIds) {

    /** Rejects a null country/type and defensively copies the delivery-method ids. */
    public ServiceConstraintRequest {
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(type, "type");
        deliveryMethodIds = deliveryMethodIds == null ? List.of() : List.copyOf(deliveryMethodIds);
    }

    /**
     * Constraint for a service realised before shipping (e.g. {@code GIFT_WRAP}):
     * {@link ServiceConstraintType#COUNTRY_SAME_QUANTITY}, no delivery methods.
     *
     * @param country the ISO country code the price applies to
     * @return the constraint
     */
    public static ServiceConstraintRequest beforeShipping(String country) {
        return new ServiceConstraintRequest(country, ServiceConstraintType.COUNTRY_SAME_QUANTITY, List.of());
    }

    /**
     * Constraint for a service realised in delivery (e.g. {@code CARRY_IN}):
     * {@link ServiceConstraintType#COUNTRY_DELIVERY_SAME_QUANTITY} with the delivery
     * methods that can realise the service.
     *
     * @param country the ISO country code the price applies to
     * @param deliveryMethodIds the delivery-method ids that can realise the service
     * @return the constraint
     */
    public static ServiceConstraintRequest inDelivery(String country, List<String> deliveryMethodIds) {
        return new ServiceConstraintRequest(
                country, ServiceConstraintType.COUNTRY_DELIVERY_SAME_QUANTITY, deliveryMethodIds);
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ConstraintCriteriaRaw toRaw() {
        ConstraintCriteriaRaw raw = new ConstraintCriteriaRaw();
        raw.setCountry(country);
        raw.setType(type.toRaw());
        if (!deliveryMethodIds.isEmpty()) {
            raw.setDeliveryMethods(deliveryMethodIds.stream()
                    .map(id -> {
                        JustIdRaw method = new JustIdRaw();
                        method.setId(id);
                        return method;
                    })
                    .toList());
        }
        return raw;
    }
}
