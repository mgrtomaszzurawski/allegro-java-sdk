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
 * seller has configured under after-sales-service-conditions, by its id.
 *
 * <p>The same immutable value is used both ways: build one to attach conditions on
 * {@code CreateOfferRequest}, or read the attached ids back from an {@link Offer}.
 * Every field is optional. The ids are the UUIDs Allegro assigns to the seller's
 * conditions; each is validated as a UUID fail-fast when set on the builder
 * ({@link IllegalArgumentException} on a malformed id), so a bad id surfaces at
 * construction rather than deep in the request.
 *
 * @param impliedWarrantyId id of the seller's implied-warranty statement, or {@code null}
 * @param returnPolicyId    id of the seller's return policy, or {@code null}
 * @param warrantyId        id of the seller's commercial warranty, or {@code null}
 * @since 0.3.0
 */
public record AfterSalesServices(
        @Nullable String impliedWarrantyId,
        @Nullable String returnPolicyId,
        @Nullable String warrantyId) {

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder pre-populated with this value's fields. */
    public Builder toBuilder() {
        return new Builder()
                .impliedWarrantyId(impliedWarrantyId)
                .returnPolicyId(returnPolicyId)
                .warrantyId(warrantyId);
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
                .impliedWarrantyId(impliedWarranty == null ? null : asString(impliedWarranty.getId()))
                .returnPolicyId(returnPolicy == null ? null : asString(returnPolicy.getId()))
                .warrantyId(warranty == null ? null : asString(warranty.getId()))
                .build();
    }

    private static @Nullable String asString(@Nullable UUID id) {
        return id == null ? null : id.toString();
    }

    /** Fluent builder for {@link AfterSalesServices}. */
    public static final class Builder {

        private static final String FIELD_IMPLIED_WARRANTY = "impliedWarrantyId";
        private static final String FIELD_RETURN_POLICY = "returnPolicyId";
        private static final String FIELD_WARRANTY = "warrantyId";
        private static final String ERR_UUID_SUFFIX = " must be a valid UUID";

        private @Nullable String impliedWarrantyId;
        private @Nullable String returnPolicyId;
        private @Nullable String warrantyId;

        /** Reference the seller's implied-warranty statement by id (a UUID). */
        public Builder impliedWarrantyId(@Nullable String impliedWarrantyId) {
            this.impliedWarrantyId = requireUuidOrNull(impliedWarrantyId, FIELD_IMPLIED_WARRANTY);
            return this;
        }

        /** Reference the seller's return policy by id (a UUID). */
        public Builder returnPolicyId(@Nullable String returnPolicyId) {
            this.returnPolicyId = requireUuidOrNull(returnPolicyId, FIELD_RETURN_POLICY);
            return this;
        }

        /** Reference the seller's commercial warranty by id (a UUID). */
        public Builder warrantyId(@Nullable String warrantyId) {
            this.warrantyId = requireUuidOrNull(warrantyId, FIELD_WARRANTY);
            return this;
        }

        /** Build the after-sales conditions. */
        public AfterSalesServices build() {
            return new AfterSalesServices(impliedWarrantyId, returnPolicyId, warrantyId);
        }

        private static @Nullable String requireUuidOrNull(@Nullable String value, String field) {
            if (value != null) {
                try {
                    UUID.fromString(value);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(field + ERR_UUID_SUFFIX, ex);
                }
            }
            return value;
        }
    }
}
