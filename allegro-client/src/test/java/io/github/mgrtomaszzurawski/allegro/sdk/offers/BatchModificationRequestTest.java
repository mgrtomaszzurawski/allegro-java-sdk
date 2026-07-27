/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchModificationRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.HandlingTime;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.OfferDuration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PaymentsModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.InvoiceType;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast validation and accessors of the {@link BatchModificationRequest}
 * builder. One failure test per required field (TESTING.md §1), plus the
 * duration/unlimited mutual exclusion; the wire mapping is covered separately by
 * {@code OfferModificationMapperTest}.
 */
class BatchModificationRequestTest {

    private static final String OFFER_ID = "111";

    @Test
    void forOffers_whenNullOffers_throws() {
        // given/when/then — a null offer list is rejected
        assertThrows(NullPointerException.class, () -> BatchModificationRequest.forOffers(null));
    }

    @Test
    void forOffers_whenEmptyOffers_throws() {
        // given/when/then — an empty offer list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchModificationRequest.forOffers(List.of()));
    }

    @Test
    void forOffers_whenBlankOfferId_throws() {
        // given/when/then — a blank offer id is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchModificationRequest.forOffers(List.of(" ")));
    }

    @Test
    void forOffers_whenTooManyOffers_throws() {
        // given — one more than the per-command maximum
        List<String> tooMany = IntStream.rangeClosed(0, BatchModificationRequest.MAX_OFFERS)
                .mapToObj(Integer::toString).toList();

        // when/then — the over-limit list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchModificationRequest.forOffers(tooMany));
    }

    @Test
    void listingDuration_whenNull_throws() {
        // given/when/then — a null listing duration is rejected
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID));
        assertThrows(NullPointerException.class, () -> builder.listingDuration(null));
    }

    @Test
    void handlingTime_whenNull_throws() {
        // given/when/then — a null handling time is rejected
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID));
        assertThrows(NullPointerException.class, () -> builder.handlingTime(null));
    }

    @Test
    void unlimitedListing_whenDurationAlreadySet_throws() {
        // given — a fixed listing duration is set (the single change)
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .listingDuration(OfferDuration.DAYS_7);

        // when/then — a second change is rejected (exactly one per command)
        assertThrows(IllegalStateException.class, builder::unlimitedListing);
    }

    @Test
    void listingDuration_whenUnlimitedAlreadySet_throws() {
        // given — an unlimited listing is set (the single change)
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .unlimitedListing();

        // when/then — a second change is rejected (exactly one per command)
        assertThrows(IllegalStateException.class, () -> builder.listingDuration(OfferDuration.DAYS_7));
    }

    @Test
    void handlingTime_whenDurationAlreadySet_throws() {
        // given — a fixed listing duration is set (the single change)
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .listingDuration(OfferDuration.DAYS_7);

        // when/then — adding a handling time change too is rejected (exactly one per command)
        assertThrows(IllegalStateException.class, () -> builder.handlingTime(HandlingTime.DAYS_2));
    }

    @Test
    void build_whenShippingRatesAssigned_exposesIdAsSingleChange() {
        // given — a shipping-rate table assignment (the single change)
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .shippingRates("rates-1")
                .build();

        // then — the id reads back and no other change is set
        assertEquals("rates-1", request.shippingRatesId());
        assertNull(request.sizeTableId());
        assertNull(request.handlingTime());
    }

    @Test
    void shippingRates_whenBlankId_throws() {
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.shippingRates(" "));
    }

    @Test
    void sizeTable_whenAnotherReferenceAlreadySet_throwsSingleChange() {
        // given — a size-table assignment already set
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .sizeTable("table-1");

        // when/then — a second reference assignment is rejected (exactly one per command)
        assertThrows(IllegalStateException.class, () -> builder.responsiblePerson("person-1"));
    }

    @Test
    void build_whenNoFieldChanged_throws() {
        // given — a builder with no field change
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID));

        // when/then — build fails fast
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenDurationOnly_exposesBuiltData() {
        // given — a fixed listing duration (the single change)
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .listingDuration(OfferDuration.DAYS_30)
                .build();

        // then — the accessors read the built intent back
        assertEquals(List.of(OFFER_ID), request.offerIds());
        assertEquals(OfferDuration.DAYS_30, request.listingDuration());
        assertFalse(request.unlimitedListing());
        assertNull(request.handlingTime());
    }

    @Test
    void build_whenHandlingTimeOnly_exposesBuiltData() {
        // given — a handling time (the single change)
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .handlingTime(HandlingTime.DAYS_2)
                .build();

        // then — the handling time is read back, no duration change
        assertEquals(HandlingTime.DAYS_2, request.handlingTime());
        assertNull(request.listingDuration());
        assertFalse(request.unlimitedListing());
    }

    @Test
    void build_whenUnlimitedOnly_exposesUnlimitedAndNoDuration() {
        // given — only an unlimited listing
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .unlimitedListing()
                .build();

        // then — unlimited is set, no fixed duration, no handling time
        assertTrue(request.unlimitedListing());
        assertNull(request.listingDuration());
        assertNull(request.handlingTime());
    }

    @Test
    void payments_whenDurationAlreadySet_throwsSingleChange() {
        // given — a listing duration already set (the single change)
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .listingDuration(OfferDuration.DAYS_7);

        // when/then — adding a payments change too is rejected (exactly one per command)
        PaymentsModification payments = PaymentsModification.builder().invoiceType(InvoiceType.VAT).build();
        assertThrows(IllegalStateException.class, () -> builder.payments(payments));
    }

    @Test
    void payments_whenNull_throws() {
        // given/when/then — a null payments modification is rejected
        BatchModificationRequest.Builder builder = BatchModificationRequest.forOffers(List.of(OFFER_ID));
        assertThrows(NullPointerException.class, () -> builder.payments(null));
    }

    @Test
    void paymentsModification_whenNeitherInvoiceNorVatRate_throws() {
        // given/when/then — a payments modification with no change is rejected fail-fast
        assertThrows(IllegalStateException.class, () -> PaymentsModification.builder().build());
    }

    @Test
    void paymentsModification_whenBlankVatRate_throws() {
        // given/when/then — a blank VAT rate is rejected
        PaymentsModification.Builder builder = PaymentsModification.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.vatRate(" "));
    }

    @Test
    void paymentsModification_whenNullVatRate_throws() {
        // given/when/then — a null VAT rate is rejected
        PaymentsModification.Builder builder = PaymentsModification.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.vatRate(null));
    }

    @Test
    void paymentsModification_whenNullInvoiceType_throws() {
        // given/when/then — a null invoice type is rejected fail-fast at the setter
        PaymentsModification.Builder builder = PaymentsModification.builder();
        assertThrows(NullPointerException.class, () -> builder.invoiceType(null));
    }

    @Test
    void paymentsModification_whenUnknownInvoiceType_throws() {
        // given/when/then — the inbound-only UNKNOWN sentinel is rejected fail-fast (not wire-settable)
        PaymentsModification.Builder builder = PaymentsModification.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.invoiceType(InvoiceType.UNKNOWN));
    }

    @Test
    void build_whenPaymentsOnly_exposesPaymentsAsSingleChange() {
        // given — a payments change carrying both invoice type and VAT rate (one element)
        PaymentsModification payments = PaymentsModification.builder()
                .invoiceType(InvoiceType.VAT).vatRate("23")
                .build();
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .payments(payments)
                .build();

        // then — the payments intent reads back and no other change is set
        assertEquals(InvoiceType.VAT, request.payments().invoiceType());
        assertEquals("23", request.payments().vatRate());
        assertNull(request.handlingTime());
    }

    @Test
    void offerIds_whenReadFromRequest_isImmutable() {
        // given — a built request
        BatchModificationRequest request = BatchModificationRequest.forOffers(List.of(OFFER_ID))
                .unlimitedListing()
                .build();

        // then — the exposed offer ids cannot be mutated by the caller
        List<String> offerIds = request.offerIds();
        assertThrows(UnsupportedOperationException.class, () -> offerIds.add("999"));
    }
}
