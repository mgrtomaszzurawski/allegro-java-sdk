/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyRaw;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The after-sales conditions attached to an offer: the implied-warranty statement,
 * the return policy, and the (commercial) warranty. Each references a template the
 * seller has configured under after-sales-service-conditions, {@linkplain NamedReference
 * by its id or its name}.
 *
 * <p>The same immutable value is used both ways: build one to attach conditions on
 * {@code CreateOfferRequest}, or read the attached references back from an {@link Offer}
 * (resolved to their id). Every field is optional. When a reference is given
 * {@linkplain NamedReference#byId(String) by id} the id must be a UUID; this is validated
 * fail-fast on the builder ({@link IllegalArgumentException} on a malformed id), so a bad id
 * surfaces at construction rather than deep in the request. A reference
 * {@linkplain NamedReference#byName(String) by name} is not UUID-checked.
 *
 * @param impliedWarranty the seller's implied-warranty statement (by id or name), or {@code null}
 * @param returnPolicy    the seller's return policy (by id or name), or {@code null}
 * @param warranty        the seller's commercial warranty (by id or name), or {@code null}
 * @since 0.3.0
 */
public record AfterSalesServices(
        @Nullable NamedReference impliedWarranty,
        @Nullable NamedReference returnPolicy,
        @Nullable NamedReference warranty) {

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder()
                .impliedWarranty(impliedWarranty)
                .returnPolicy(returnPolicy)
                .warranty(warranty);
    }

    /**
     * Project a generated after-sales response onto the consumer value.
     *
     * @param raw the generated after-sales block (may be {@code null})
     * @return the mapped value, or {@code null} if {@code raw} is {@code null}
     */
    public static @Nullable AfterSalesServices from(@Nullable AfterSalesServicesRaw raw) {
        if (raw == null) {
            return null;
        }
        ImpliedWarrantyRaw impliedWarranty = raw.getImpliedWarranty();
        ReturnPolicyRaw returnPolicy = raw.getReturnPolicy();
        WarrantyRaw warranty = raw.getWarranty();
        return builder()
                .impliedWarranty(refById(impliedWarranty == null ? null : impliedWarranty.getId()))
                .returnPolicy(refById(returnPolicy == null ? null : returnPolicy.getId()))
                .warranty(refById(warranty == null ? null : warranty.getId()))
                .build();
    }

    private static @Nullable NamedReference refById(@Nullable UUID id) {
        return id == null ? null : NamedReference.byId(id.toString());
    }

    /** Fluent builder for {@link AfterSalesServices}. */
    public static final class Builder {

        private static final String FIELD_IMPLIED_WARRANTY = "impliedWarranty";
        private static final String FIELD_RETURN_POLICY = "returnPolicy";
        private static final String FIELD_WARRANTY = "warranty";
        private static final String ERR_UUID_SUFFIX = " id must be a valid UUID";

        private @Nullable NamedReference impliedWarranty;
        private @Nullable NamedReference returnPolicy;
        private @Nullable NamedReference warranty;

        /** Reference the seller's implied-warranty statement (by id or name). */
        public Builder impliedWarranty(@Nullable NamedReference impliedWarranty) {
            this.impliedWarranty = requireUuidIdOrNull(impliedWarranty, FIELD_IMPLIED_WARRANTY);
            return this;
        }

        /** Reference the seller's return policy (by id or name). */
        public Builder returnPolicy(@Nullable NamedReference returnPolicy) {
            this.returnPolicy = requireUuidIdOrNull(returnPolicy, FIELD_RETURN_POLICY);
            return this;
        }

        /** Reference the seller's commercial warranty (by id or name). */
        public Builder warranty(@Nullable NamedReference warranty) {
            this.warranty = requireUuidIdOrNull(warranty, FIELD_WARRANTY);
            return this;
        }

        /** Build the after-sales conditions. */
        public AfterSalesServices build() {
            return new AfterSalesServices(impliedWarranty, returnPolicy, warranty);
        }

        private static @Nullable NamedReference requireUuidIdOrNull(
                @Nullable NamedReference reference, String field) {
            if (reference != null && reference.id() != null) {
                try {
                    UUID.fromString(reference.id());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(field + ERR_UUID_SUFFIX, ex);
                }
            }
            return reference;
        }
    }
}
