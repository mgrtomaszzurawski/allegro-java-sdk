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
 * {@link ServiceConstraintType type}, for services realised in delivery the ids of
 * the delivery methods that can realise the service, and for category-limited
 * services the ids of the offer categories it is available in.
 *
 * <p>Allegro requires a matching constraint for every configuration: services realised
 * before shipping (e.g. {@code GIFT_WRAP}) use {@link #beforeShipping(String)}, services
 * realised in delivery (e.g. {@code CARRY_IN}) use {@link #inDelivery(String, List)}, and
 * delivery services limited to offer categories (e.g. {@code ALLEGRO_TV_CARRY_IN}) use
 * {@link #inDeliveryForCategories(String, List, List)}.
 *
 * @param country the ISO country code the price applies to
 * @param type the constraint type
 * @param deliveryMethodIds delivery-method ids (empty unless the type is
 *     {@link ServiceConstraintType#COUNTRY_DELIVERY_SAME_QUANTITY} or
 *     {@link ServiceConstraintType#COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY})
 * @param categoryIds offer-category ids (empty unless the type is
 *     {@link ServiceConstraintType#COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY})
 * @since 0.3.0
 */
public record ServiceConstraintRequest(
        String country,
        ServiceConstraintType type,
        List<String> deliveryMethodIds,
        List<String> categoryIds) {

    private static final String ERR_CATEGORIES_REQUIRED =
            "categoryIds must hold at least one id for a COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY constraint";
    private static final String ERR_CATEGORIES_NOT_ALLOWED =
            "categoryIds apply only to a COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY constraint";
    private static final String ERR_DELIVERY_METHODS_NOT_ALLOWED =
            "deliveryMethodIds apply only to a COUNTRY_DELIVERY_SAME_QUANTITY"
                    + " or COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY constraint";

    /**
     * Rejects a null country/type, defensively copies the id lists, and replicates the
     * spec's shape rules fail-fast: delivery methods only on the two delivery-realised
     * types, categories only on the category-limited type (which needs at least one —
     * {@code minItems: 1}).
     */
    public ServiceConstraintRequest {
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(type, "type");
        deliveryMethodIds = deliveryMethodIds == null ? List.of() : List.copyOf(deliveryMethodIds);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        if (type == ServiceConstraintType.COUNTRY_SAME_QUANTITY && !deliveryMethodIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_DELIVERY_METHODS_NOT_ALLOWED);
        }
        boolean categoryLimited = type == ServiceConstraintType.COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY;
        if (categoryLimited && categoryIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_CATEGORIES_REQUIRED);
        }
        if (!categoryLimited && !categoryIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_CATEGORIES_NOT_ALLOWED);
        }
    }

    /** A constraint without category limits (the shape before category-limited services existed). */
    public ServiceConstraintRequest(String country, ServiceConstraintType type,
            List<String> deliveryMethodIds) {
        this(country, type, deliveryMethodIds, List.of());
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

    /**
     * Constraint for a service realised in delivery and limited to specific offer
     * categories (e.g. {@code ALLEGRO_TV_CARRY_IN}):
     * {@link ServiceConstraintType#COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY} with the
     * delivery methods that can realise the service and the categories it is
     * available in (at least one).
     *
     * @param country the ISO country code the price applies to
     * @param deliveryMethodIds the delivery-method ids that can realise the service
     * @param categoryIds the offer-category ids the service is available in
     * @return the constraint
     */
    public static ServiceConstraintRequest inDeliveryForCategories(String country,
            List<String> deliveryMethodIds, List<String> categoryIds) {
        return new ServiceConstraintRequest(country,
                ServiceConstraintType.COUNTRY_DELIVERY_CATEGORY_SAME_QUANTITY,
                deliveryMethodIds, categoryIds);
    }

    /** Project onto the generated Layer-1 request DTO. */
    public ConstraintCriteriaRaw toRaw() {
        ConstraintCriteriaRaw raw = new ConstraintCriteriaRaw();
        raw.setCountry(country);
        raw.setType(type.toRaw());
        if (!deliveryMethodIds.isEmpty()) {
            raw.setDeliveryMethods(refs(deliveryMethodIds));
        }
        if (!categoryIds.isEmpty()) {
            raw.setCategories(refs(categoryIds));
        }
        return raw;
    }

    private static List<JustIdRaw> refs(List<String> identifiers) {
        return identifiers.stream()
                .map(id -> {
                    JustIdRaw reference = new JustIdRaw();
                    reference.setId(id);
                    return reference;
                })
                .toList();
    }
}
