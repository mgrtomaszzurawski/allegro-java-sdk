/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A batch offer-settings change — the request passed to
 * {@code offers().batch().modify(...)}. It applies <em>exactly one</em> supported
 * field change to every target offer (up to {@value #MAX_OFFERS}) in one command:
 * the listing duration (a fixed {@link OfferDuration} or unlimited), the
 * {@link HandlingTime dispatch time}, or an id-reference assignment — the shipping
 * rate table, wholesale price list, size table, additional-services group, or the
 * GPSR responsible producer/person. Exactly one is required — Allegro rejects a
 * command whose modification carries more than one element (live-verified:
 * {@code VALIDATION_ERROR}, <em>"modification should contain exactly 1 element"</em>),
 * so to change two aspects submit two commands.
 *
 * <p>The id-reference setters <em>assign</em> a reference across the target offers.
 * <em>Un</em>assigning (clearing) a reference needs the wire's explicit-null, which
 * this command's partial body omits, so a dedicated unassign path will follow separately.
 *
 * @since 0.5.0
 */
public final class BatchModificationRequest {

    /** Allegro accepts up to 1000 offers in one criterion. */
    public static final int MAX_OFFERS = 1000;

    private static final String ERR_OFFERS_EMPTY = "at least one offer id is required";
    private static final String ERR_OFFERS_TOO_MANY = "at most " + MAX_OFFERS + " offers per command";
    private static final String ERR_OFFER_ID = "offer id must not be null or blank";
    private static final String ERR_DURATION = "listing duration must not be null";
    private static final String ERR_HANDLING_TIME = "handling time must not be null";
    private static final String ERR_REFERENCE_ID = "reference id must not be null or blank";
    private static final String ERR_SINGLE_CHANGE =
            "a modification changes exactly one field — Allegro rejects a command with more than one";
    private static final String ERR_NO_CHANGE = "a modification must change exactly one field";

    private final List<String> offerIds;
    private final @Nullable OfferDuration listingDuration;
    private final boolean unlimitedListing;
    private final @Nullable HandlingTime handlingTime;
    private final @Nullable String shippingRatesId;
    private final @Nullable String wholesalePriceListId;
    private final @Nullable String sizeTableId;
    private final @Nullable String additionalServicesGroupId;
    private final @Nullable String responsibleProducerId;
    private final @Nullable String responsiblePersonId;

    private BatchModificationRequest(Builder builder) {
        this.offerIds = List.copyOf(builder.offerIds);
        this.listingDuration = builder.listingDuration;
        this.unlimitedListing = builder.unlimitedListing;
        this.handlingTime = builder.handlingTime;
        this.shippingRatesId = builder.shippingRatesId;
        this.wholesalePriceListId = builder.wholesalePriceListId;
        this.sizeTableId = builder.sizeTableId;
        this.additionalServicesGroupId = builder.additionalServicesGroupId;
        this.responsibleProducerId = builder.responsibleProducerId;
        this.responsiblePersonId = builder.responsiblePersonId;
    }

    /** Start building a modification for {@code offerIds}. */
    public static Builder forOffers(List<String> offerIds) {
        return new Builder(offerIds);
    }

    /** The offers this modification targets. */
    public List<String> offerIds() {
        return offerIds;
    }

    /** The fixed listing duration to set, or {@code null} (unlimited, or duration unchanged). */
    public @Nullable OfferDuration listingDuration() {
        return listingDuration;
    }

    /** Whether the listing should be made unlimited. */
    public boolean unlimitedListing() {
        return unlimitedListing;
    }

    /** The dispatch time to set, or {@code null} if the handling time is unchanged. */
    public @Nullable HandlingTime handlingTime() {
        return handlingTime;
    }

    /** The shipping-rate-table id to assign, or {@code null}. */
    public @Nullable String shippingRatesId() {
        return shippingRatesId;
    }

    /** The wholesale-price-list id to assign, or {@code null}. */
    public @Nullable String wholesalePriceListId() {
        return wholesalePriceListId;
    }

    /** The size-table id to assign, or {@code null}. */
    public @Nullable String sizeTableId() {
        return sizeTableId;
    }

    /** The additional-services-group id to assign, or {@code null}. */
    public @Nullable String additionalServicesGroupId() {
        return additionalServicesGroupId;
    }

    /** The GPSR responsible-producer id to assign, or {@code null}. */
    public @Nullable String responsibleProducerId() {
        return responsibleProducerId;
    }

    /** The GPSR responsible-person id to assign, or {@code null}. */
    public @Nullable String responsiblePersonId() {
        return responsiblePersonId;
    }

    /** Fluent builder; validates fail-fast on {@link #build()}. */
    public static final class Builder {

        private final List<String> offerIds;
        private @Nullable OfferDuration listingDuration;
        private boolean unlimitedListing;
        private @Nullable HandlingTime handlingTime;
        private @Nullable String shippingRatesId;
        private @Nullable String wholesalePriceListId;
        private @Nullable String sizeTableId;
        private @Nullable String additionalServicesGroupId;
        private @Nullable String responsibleProducerId;
        private @Nullable String responsiblePersonId;

        private Builder(List<String> offerIds) {
            this.offerIds = validatedOfferIds(offerIds);
        }

        /** Set a fixed listing duration (the request's single change). */
        public Builder listingDuration(OfferDuration duration) {
            requireNoChangeYet();
            this.listingDuration = Objects.requireNonNull(duration, ERR_DURATION);
            return this;
        }

        /** Make the listing unlimited (the request's single change). */
        public Builder unlimitedListing() {
            requireNoChangeYet();
            this.unlimitedListing = true;
            return this;
        }

        /** Set the dispatch (handling) time (the request's single change). */
        public Builder handlingTime(HandlingTime dispatchTime) {
            requireNoChangeYet();
            this.handlingTime = Objects.requireNonNull(dispatchTime, ERR_HANDLING_TIME);
            return this;
        }

        /** Assign a shipping-rate table by id (the request's single change). */
        public Builder shippingRates(String ratesId) {
            requireNoChangeYet();
            this.shippingRatesId = validatedId(ratesId);
            return this;
        }

        /** Assign a wholesale price list by id (the request's single change). */
        public Builder wholesalePriceList(String priceListId) {
            requireNoChangeYet();
            this.wholesalePriceListId = validatedId(priceListId);
            return this;
        }

        /** Assign a size table by id (the request's single change). */
        public Builder sizeTable(String tableId) {
            requireNoChangeYet();
            this.sizeTableId = validatedId(tableId);
            return this;
        }

        /** Assign an additional-services group by id (the request's single change). */
        public Builder additionalServicesGroup(String groupId) {
            requireNoChangeYet();
            this.additionalServicesGroupId = validatedId(groupId);
            return this;
        }

        /** Assign the GPSR responsible producer by id (the request's single change). */
        public Builder responsibleProducer(String producerId) {
            requireNoChangeYet();
            this.responsibleProducerId = validatedId(producerId);
            return this;
        }

        /** Assign the GPSR responsible person by id (the request's single change). */
        public Builder responsiblePerson(String personId) {
            requireNoChangeYet();
            this.responsiblePersonId = validatedId(personId);
            return this;
        }

        /** Build, requiring exactly one field change. */
        public BatchModificationRequest build() {
            if (!hasChange()) {
                throw new IllegalStateException(ERR_NO_CHANGE);
            }
            return new BatchModificationRequest(this);
        }

        /** Guard the single-change rule: a second change is rejected fail-fast. */
        private void requireNoChangeYet() {
            if (hasChange()) {
                throw new IllegalStateException(ERR_SINGLE_CHANGE);
            }
        }

        private boolean hasChange() {
            return listingDuration != null || unlimitedListing || handlingTime != null
                    || shippingRatesId != null || wholesalePriceListId != null || sizeTableId != null
                    || additionalServicesGroupId != null || responsibleProducerId != null
                    || responsiblePersonId != null;
        }

        private static String validatedId(String referenceId) {
            if (referenceId == null || referenceId.isBlank()) {
                throw new IllegalArgumentException(ERR_REFERENCE_ID);
            }
            return referenceId;
        }

        private static List<String> validatedOfferIds(List<String> offerIds) {
            Objects.requireNonNull(offerIds, ERR_OFFERS_EMPTY);
            if (offerIds.isEmpty()) {
                throw new IllegalArgumentException(ERR_OFFERS_EMPTY);
            }
            if (offerIds.size() > MAX_OFFERS) {
                throw new IllegalArgumentException(ERR_OFFERS_TOO_MANY);
            }
            for (String offerId : offerIds) {
                if (offerId == null || offerId.isBlank()) {
                    throw new IllegalArgumentException(ERR_OFFER_ID);
                }
            }
            return offerIds;
        }
    }
}
