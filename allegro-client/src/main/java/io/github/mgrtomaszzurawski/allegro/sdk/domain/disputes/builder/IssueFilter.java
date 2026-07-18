/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Optional filter for streaming post-purchase issues. All fields are optional; an
 * empty filter streams every issue on the account.
 *
 * <pre>{@code
 * IssueFilter openDisputes = IssueFilter.builder()
 *         .status(IssueStatus.DISPUTE_ONGOING)
 *         .build();
 * }</pre>
 *
 * @since 0.2.0
 */
public final class IssueFilter {

    private static final String ERR_STATUS_NULL = "status must not be null";

    private final List<IssueStatus> statuses;
    private final @Nullable String checkoutFormId;

    private IssueFilter(Builder builder) {
        this.statuses = List.copyOf(builder.statuses);
        this.checkoutFormId = builder.checkoutFormId;
    }

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** An empty filter (no criteria). */
    public static IssueFilter none() {
        return new Builder().build();
    }

    /** Statuses to filter by (any-of); never {@code null}, possibly empty. */
    public List<IssueStatus> statuses() {
        return statuses;
    }

    /** Restrict to issues about this order (checkout form), or {@code null}. */
    public @Nullable String checkoutFormId() {
        return checkoutFormId;
    }

    /** A builder pre-filled from this filter. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.statuses = new ArrayList<>(statuses);
        builder.checkoutFormId = checkoutFormId;
        return builder;
    }

    /** Fluent builder for {@link IssueFilter}. */
    public static final class Builder {

        private List<IssueStatus> statuses = new ArrayList<>();
        private @Nullable String checkoutFormId;

        private Builder() {
        }

        /** Adds a status to filter by; issues matching any added status are returned. */
        public Builder status(IssueStatus status) {
            Objects.requireNonNull(status, ERR_STATUS_NULL);
            this.statuses.add(status);
            return this;
        }

        /** Restricts the result to issues about one order (checkout form id). */
        public Builder checkoutFormId(String orderId) {
            this.checkoutFormId = orderId;
            return this;
        }

        /** Builds the immutable filter. */
        public IssueFilter build() {
            return new IssueFilter(this);
        }
    }
}
