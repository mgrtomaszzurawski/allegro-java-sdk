/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.account.builder;

/**
 * Criteria for searching charity fundraising campaigns. A search phrase is
 * required; the result limit defaults to {@value #MAX_LIMIT} and must stay
 * within {@code 1..}{@value #MAX_LIMIT}.
 *
 * <pre>{@code
 * CharitySearch search = CharitySearch.builder()
 *         .phrase("children")
 *         .limit(20)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class CharitySearch {

    /** Maximum number of results the API returns. */
    public static final int MAX_LIMIT = 100;

    private static final int MIN_LIMIT = 1;
    private static final String ERR_PHRASE_REQUIRED = "phrase is required";
    private static final String ERR_LIMIT_RANGE =
            "limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT;

    private final String phrase;
    private final int limit;

    private CharitySearch(Builder builder) {
        this.phrase = builder.phrase;
        this.limit = builder.limit;
    }

    /** The search phrase (campaign or organization name prefix). */
    public String phrase() {
        return phrase;
    }

    /** The maximum number of results to return. */
    public int limit() {
        return limit;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-filled from this search. */
    public Builder toBuilder() {
        return new Builder().phrase(phrase).limit(limit);
    }

    /** Fluent builder for {@link CharitySearch}. */
    public static final class Builder {

        private String phrase;
        private int limit = MAX_LIMIT;

        /** The search phrase (required). */
        public Builder phrase(String phrase) {
            this.phrase = phrase;
            return this;
        }

        /** The result limit (1..{@value #MAX_LIMIT}); defaults to {@value #MAX_LIMIT}. */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /** Validate and build. */
        public CharitySearch build() {
            if (phrase == null || phrase.isBlank()) {
                throw new IllegalStateException(ERR_PHRASE_REQUIRED);
            }
            if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
                throw new IllegalStateException(ERR_LIMIT_RANGE);
            }
            return new CharitySearch(this);
        }
    }
}
