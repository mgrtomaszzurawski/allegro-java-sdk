/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast validation and accessors of the {@link BatchPromoOptionsRequest}
 * builder. One failure test per required field (TESTING.md §1); the wire mapping
 * is covered separately by {@code PromoBatchMapperTest}.
 */
class BatchPromoOptionsRequestTest {

    private static final String OFFER_ID = "111";
    private static final String BASE_PACKAGE = "emphasized1d";
    private static final String EXTRA_PACKAGE = "bold30d";

    @Test
    void forOffers_whenNullOffers_throws() {
        // given/when/then — a null offer list is rejected
        assertThrows(NullPointerException.class, () -> BatchPromoOptionsRequest.forOffers(null));
    }

    @Test
    void forOffers_whenEmptyOffers_throws() {
        // given/when/then — an empty offer list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPromoOptionsRequest.forOffers(List.of()));
    }

    @Test
    void forOffers_whenBlankOfferId_throws() {
        // given/when/then — a blank offer id is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPromoOptionsRequest.forOffers(List.of(" ")));
    }

    @Test
    void forOffers_whenTooManyOffers_throws() {
        // given — one more than the per-command maximum
        List<String> tooMany = IntStream.rangeClosed(0, BatchPromoOptionsRequest.MAX_OFFERS)
                .mapToObj(Integer::toString).toList();

        // when/then — the over-limit list is rejected
        assertThrows(IllegalArgumentException.class,
                () -> BatchPromoOptionsRequest.forOffers(tooMany));
    }

    @Test
    void basePackage_whenBlank_throws() {
        // given/when/then — a blank base package id is rejected
        BatchPromoOptionsRequest.Builder builder = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.basePackage(" "));
    }

    @Test
    void addExtraPackage_whenBlank_throws() {
        // given/when/then — a blank extra package id is rejected
        BatchPromoOptionsRequest.Builder builder = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID));
        assertThrows(IllegalArgumentException.class, () -> builder.addExtraPackage(" "));
    }

    @Test
    void timing_whenNull_throws() {
        // given/when/then — a null timing is rejected
        BatchPromoOptionsRequest.Builder builder = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID));
        assertThrows(NullPointerException.class, () -> builder.timing(null));
    }

    @Test
    void build_whenNoPackageChange_throws() {
        // given — offers but neither a base nor an extra package
        BatchPromoOptionsRequest.Builder builder = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID));

        // when/then — build fails fast
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void build_whenBaseAndExtraSet_exposesBuiltData() {
        // given — a base package, an extra package and a timing
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID))
                .basePackage(BASE_PACKAGE)
                .addExtraPackage(EXTRA_PACKAGE)
                .timing(PromoModificationTiming.NOW)
                .build();

        // then — the accessors read the built intent back
        assertEquals(List.of(OFFER_ID), request.offerIds());
        assertEquals(BASE_PACKAGE, request.basePackageId());
        assertEquals(List.of(EXTRA_PACKAGE), request.extraPackageIds());
        assertEquals(PromoModificationTiming.NOW, request.timing());
    }

    @Test
    void build_whenExtraPackagesOnly_hasNoBasePackageOrTiming() {
        // given — only extra packages
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID))
                .extraPackages(List.of(EXTRA_PACKAGE))
                .build();

        // then — no base package, no timing, the extra package present
        assertNull(request.basePackageId());
        assertNull(request.timing());
        assertEquals(List.of(EXTRA_PACKAGE), request.extraPackageIds());
    }

    @Test
    void extraPackages_whenReadFromRequest_isImmutable() {
        // given — a built request
        BatchPromoOptionsRequest request = BatchPromoOptionsRequest.forOffers(List.of(OFFER_ID))
                .addExtraPackage(EXTRA_PACKAGE)
                .build();

        // then — the exposed extra packages cannot be mutated by the caller
        List<String> extras = request.extraPackageIds();
        assertThrows(UnsupportedOperationException.class, () -> extras.add("x"));
    }
}
