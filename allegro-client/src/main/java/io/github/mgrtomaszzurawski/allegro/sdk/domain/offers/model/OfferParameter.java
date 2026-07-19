/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterProductOfferResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ParameterRangeValueRaw;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One category parameter of an offer. A category dictates its parameters and which
 * shape each takes, so a single parameter carries exactly one of three value kinds:
 *
 * <ul>
 *   <li>dictionary — one or more {@code valuesIds} chosen from the category's value
 *       dictionary (build with {@link #dictionary(String, String...)});</li>
 *   <li>free-form — one or more free-text {@code values}
 *       (build with {@link #freeText(String, String...)});</li>
 *   <li>range — a numeric or date {@code rangeValue}
 *       (build with {@link #range(String, String, String)}).</li>
 * </ul>
 *
 * <p>The same immutable value is used both ways: build one to set a parameter on
 * {@code CreateOfferRequest}, or read one back from an {@link Offer}. The parameter
 * {@code id} (from the category definition) is required; {@code name} is populated
 * on read and ignored on write (the server keys on the id).
 *
 * @param id         the category parameter id (required)
 * @param name       the parameter name (populated on read; ignored on write), or {@code null}
 * @param values     free-text values (empty when the parameter is a dictionary or range one)
 * @param valuesIds  dictionary value ids (empty when the parameter is free-form or a range one)
 * @param rangeValue the range value, or {@code null} when the parameter is not a range one
 * @since 0.3.0
 */
public record OfferParameter(
        String id,
        @Nullable String name,
        List<String> values,
        List<String> valuesIds,
        @Nullable ParameterRange rangeValue) {

    /**
     * Canonical constructor. Prefer the {@link #dictionary}/{@link #freeText}/{@link #range}
     * factories; this normalizes the value lists to immutable copies and requires the id.
     */
    public OfferParameter {
        Objects.requireNonNull(id, "id");
        values = List.copyOf(values);
        valuesIds = List.copyOf(valuesIds);
    }

    /** A dictionary parameter carrying the given value ids from the category's dictionary. */
    public static OfferParameter dictionary(String id, String... valuesIds) {
        return dictionary(id, List.of(valuesIds));
    }

    /** A dictionary parameter carrying the given value ids from the category's dictionary. */
    public static OfferParameter dictionary(String id, List<String> valuesIds) {
        return new OfferParameter(id, null, List.of(), valuesIds, null);
    }

    /** A free-form parameter carrying the given free-text values. */
    public static OfferParameter freeText(String id, String... values) {
        return freeText(id, List.of(values));
    }

    /** A free-form parameter carrying the given free-text values. */
    public static OfferParameter freeText(String id, List<String> values) {
        return new OfferParameter(id, null, values, List.of(), null);
    }

    /** A range parameter spanning {@code lowerBound}&#8230;{@code upperBound} (both inclusive). */
    public static OfferParameter range(String id, String lowerBound, String upperBound) {
        return new OfferParameter(id, null, List.of(), List.of(), ParameterRange.of(lowerBound, upperBound));
    }

    /**
     * Project a generated response parameter onto the consumer value.
     *
     * @param raw the generated parameter
     * @return the mapped value (absent value lists degrade to empty, an absent range to {@code null})
     */
    public static OfferParameter from(ParameterProductOfferResponseRaw raw) {
        return new OfferParameter(
                raw.getId(),
                raw.getName(),
                raw.getValues() == null ? List.of() : raw.getValues(),
                raw.getValuesIds() == null ? List.of() : raw.getValuesIds(),
                rangeValueOf(raw.getRangeValue()));
    }

    /**
     * The generated request parameter for this value. Only the value kind actually set is
     * written; the {@code name} is sent when present but the server keys on the {@code id}.
     */
    public ParameterProductOfferRequestRaw toRaw() {
        ParameterProductOfferRequestRaw raw = new ParameterProductOfferRequestRaw().id(id);
        if (name != null) {
            raw.name(name);
        }
        if (!values.isEmpty()) {
            raw.values(values);
        }
        if (!valuesIds.isEmpty()) {
            raw.valuesIds(valuesIds);
        }
        if (rangeValue != null) {
            raw.rangeValue(rangeValue.toRaw());
        }
        return raw;
    }

    private static @Nullable ParameterRange rangeValueOf(@Nullable ParameterRangeValueRaw raw) {
        return raw == null ? null : ParameterRange.of(raw.getFrom(), raw.getTo());
    }
}
