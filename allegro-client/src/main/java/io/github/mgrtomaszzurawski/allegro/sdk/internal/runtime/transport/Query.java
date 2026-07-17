/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordered, percent-encoded collection of query parameters for a single request.
 *
 * <p>Domain clients never hand-roll query strings — they build a {@code Query}
 * and pass it to {@link HttpCall#query(Query)}, so encoding (spaces, reserved
 * characters, repeated parameters like {@code offer.id}) is applied once, in
 * one place. Insertion order is preserved and repeated names are kept as
 * separate pairs, matching Allegro's repeated-parameter filters.
 *
 * @since 0.2.0
 */
public final class Query {

    private static final String ENCODED_SPACE = "%20";
    private static final char QUERY_START = '?';
    private static final char PAIR_SEPARATOR = '&';
    private static final char VALUE_SEPARATOR = '=';

    private final List<String> names = new ArrayList<>();
    private final List<String> values = new ArrayList<>();

    private Query() {
    }

    /** A new, empty query. */
    public static Query create() {
        return new Query();
    }

    /**
     * Append one parameter. A {@code null} value is skipped so optional filter
     * fields drop out of the URL rather than serializing as {@code name=null}.
     *
     * @param name  parameter name (required)
     * @param value parameter value; {@code null} means "omit this parameter"
     * @return this query, for chaining
     */
    public Query add(String name, Object value) {
        if (value != null) {
            names.add(name);
            values.add(value.toString());
        }
        return this;
    }

    /** Append the same {@code name} once per value, skipping {@code null}s. */
    public Query addAll(String name, List<?> repeatedValues) {
        for (Object value : repeatedValues) {
            add(name, value);
        }
        return this;
    }

    /** {@code true} when no parameter carries a value. */
    public boolean isEmpty() {
        return names.isEmpty();
    }

    /**
     * Render as a leading-{@code ?} encoded query string, or the empty string
     * when there is nothing to append. The result is safe to concatenate onto
     * an {@link ApiPaths} path.
     */
    public String render() {
        if (names.isEmpty()) {
            return "";
        }
        StringBuilder rendered = new StringBuilder().append(QUERY_START);
        for (int pair = 0; pair < names.size(); pair++) {
            if (pair > 0) {
                rendered.append(PAIR_SEPARATOR);
            }
            rendered.append(encode(names.get(pair)))
                    .append(VALUE_SEPARATOR)
                    .append(encode(values.get(pair)));
        }
        return rendered.toString();
    }

    private static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", ENCODED_SPACE);
    }
}
