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
 * {@linkplain OfferParameterKind kind} each takes:
 *
 * <ul>
 *   <li>{@link OfferParameterKind#DICTIONARY} — one or more {@code valuesIds} chosen
 *       from the category's value dictionary (build with {@link #dictionary(String, String...)});</li>
 *   <li>{@link OfferParameterKind#FREE_TEXT} — one or more free-text {@code values}
 *       (build with {@link #freeText(String, String...)});</li>
 *   <li>{@link OfferParameterKind#RANGE} — a numeric or date {@code rangeValue}
 *       (build with {@link #range(String, String, String)}).</li>
 * </ul>
 *
 * <p>The same immutable value is used both ways: build one to set a parameter on
 * {@code CreateOfferRequest}, or read one back from an {@link Offer}. Call
 * {@link #kind()} to tell the kinds apart rather than inspecting which list is
 * populated — because <strong>on read a dictionary parameter carries both its
 * {@code valuesIds} (the ids) and {@code values} (the human-readable labels of those
 * ids)</strong>. On write only the ids of a dictionary parameter are sent (the labels
 * are read-only; echoing them back is rejected by Allegro), so a read parameter can be
 * fed straight back into a create.
 *
 * <p>The parameter {@code id} (from the category definition) is required; {@code name}
 * is populated on read and, when present, sent on write (the server keys on the id).
 *
 * @param id         the category parameter id (required)
 * @param name       the parameter name (populated on read), or {@code null}
 * @param values     free-text values of a free-text parameter, or the read-only labels
 *                   of a dictionary parameter's ids; empty for a range parameter
 * @param valuesIds  dictionary value ids; empty for a free-text or range parameter
 * @param rangeValue the range value, or {@code null} when the parameter is not a range one
 * @since 0.3.0
 */
public record OfferParameter(
        String id,
        @Nullable String name,
        List<String> values,
        List<String> valuesIds,
        @Nullable ParameterRange rangeValue) {

    private static final String ERR_DICTIONARY_EMPTY = "a dictionary parameter needs at least one value id";
    private static final String ERR_FREE_TEXT_EMPTY = "a free-text parameter needs at least one value";

    /**
     * Canonical constructor. Prefer the {@link #dictionary}/{@link #freeText}/{@link #range}
     * factories; this normalizes the value lists to immutable copies and requires the id.
     * It stays permissive about which lists are populated so a read parameter (a dictionary
     * one carries both {@code values} and {@code valuesIds}) maps without rejection.
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
        if (valuesIds.isEmpty()) {
            throw new IllegalArgumentException(ERR_DICTIONARY_EMPTY);
        }
        return new OfferParameter(id, null, List.of(), valuesIds, null);
    }

    /** A free-form parameter carrying the given free-text values. */
    public static OfferParameter freeText(String id, String... values) {
        return freeText(id, List.of(values));
    }

    /** A free-form parameter carrying the given free-text values. */
    public static OfferParameter freeText(String id, List<String> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(ERR_FREE_TEXT_EMPTY);
        }
        return new OfferParameter(id, null, values, List.of(), null);
    }

    /** A range parameter spanning {@code lowerBound}&#8230;{@code upperBound} (both inclusive). */
    public static OfferParameter range(String id, String lowerBound, String upperBound) {
        return new OfferParameter(id, null, List.of(), List.of(), ParameterRange.of(lowerBound, upperBound));
    }

    /**
     * The kind of this parameter, derived from the value it carries: a range if it has a
     * {@code rangeValue}, otherwise a dictionary if it has any {@code valuesIds} (on read a
     * dictionary parameter also carries the {@code values} labels), otherwise free-text.
     */
    public OfferParameterKind kind() {
        if (rangeValue != null) {
            return OfferParameterKind.RANGE;
        }
        return valuesIds.isEmpty() ? OfferParameterKind.FREE_TEXT : OfferParameterKind.DICTIONARY;
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
     * The generated request parameter for this value. Only the writable value kind is sent:
     * a dictionary parameter is written with its {@code valuesIds} only — the {@code values}
     * a read dictionary parameter also carries are read-only labels, and echoing them back
     * alongside the ids is rejected by Allegro ({@code InvalidDictionaryParameter}). The
     * {@code name} is sent when present but the server keys on the {@code id}.
     */
    public ParameterProductOfferRequestRaw toRaw() {
        ParameterProductOfferRequestRaw raw = new ParameterProductOfferRequestRaw().id(id);
        if (name != null) {
            raw.name(name);
        }
        // Write only the value kind: a dictionary parameter by its ids (empty labels are
        // dropped), a free-text one by its values, a range one by its bounds. An empty list
        // set here is omitted from the wire by the transport's jsonBodyPartial (NON_EMPTY).
        return switch (kind()) {
            case DICTIONARY -> raw.valuesIds(valuesIds);
            case FREE_TEXT -> raw.values(values);
            case RANGE -> raw.rangeValue(Objects.requireNonNull(rangeValue).toRaw());
        };
    }

    private static @Nullable ParameterRange rangeValueOf(@Nullable ParameterRangeValueRaw raw) {
        return raw == null ? null : ParameterRange.of(raw.getFrom(), raw.getTo());
    }
}
