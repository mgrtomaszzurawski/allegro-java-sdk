/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.builder.ReturnPolicyUpdateRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.AfterSalesAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnCostCoveredBy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyAvailability;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnPolicyOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.aftersale.model.ReturnRestrictionCause;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast validation coverage for {@link ReturnPolicyRequest},
 * {@link ReturnPolicyUpdateRequest} and their builders — every builder method and
 * every required-field / length guard is exercised.
 */
class ReturnPolicyBuilderTest {

    private static final String NAME = "Standard 14-day returns";
    private static final String WITHDRAWAL_PERIOD = "P14D";
    private static final int NAME_AT_LIMIT = 200;
    private static final int OVER_NAME_LIMIT = 201;
    private static final String FIELD_NAME = "name";
    private static final String FIELD_FULFILLMENT = "fulfillment";
    private static final String FIELD_AVAILABILITY = "availability";
    private static final String LENGTH_WORD = "exceeds";

    private static AfterSalesAddress address() {
        return new AfterSalesAddress("Allegro sp. z o.o.", "Grunwaldzka 182", "60-166", "Poznań", "PL");
    }

    private static ReturnPolicyOptions options() {
        return new ReturnPolicyOptions(true, true, false, false, true);
    }

    // ---- create request ----

    @Test
    void build_whenRequiredFieldsOnly_succeeds() {
        ReturnPolicyRequest request = ReturnPolicyRequest.builder()
                .name(NAME)
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.full())
                .build();

        assertEquals(NAME, request.name());
        assertEquals(false, request.fulfillment());
        assertEquals(ReturnRange.FULL, request.availability().range());
        assertNull(request.withdrawalPeriod());
        assertNull(request.returnCost());
        assertNull(request.address());
        assertNull(request.contact());
        assertNull(request.options());
    }

    @Test
    void build_whenAllCoreFieldsSet_preservesEveryField() {
        ReturnPolicyRequest request = ReturnPolicyRequest.builder()
                .name(NAME)
                .fulfillment(true)
                .availability(ReturnPolicyAvailability.restricted(ReturnRestrictionCause.SEALED_MEDIA))
                .withdrawalPeriod(WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.SELLER)
                .address(address())
                .contact(new ReturnPolicyContact("123 123 123", "seller@example.com"))
                .options(options())
                .build();

        assertEquals(true, request.fulfillment());
        assertEquals(ReturnRange.RESTRICTED, request.availability().range());
        assertEquals(ReturnRestrictionCause.SEALED_MEDIA, request.availability().restrictionCause());
        assertEquals(WITHDRAWAL_PERIOD, request.withdrawalPeriod());
        assertEquals(ReturnCostCoveredBy.SELLER, request.returnCost());
        assertEquals("Poznań", request.address().city());
        assertEquals("seller@example.com", request.contact().email());
        assertTrue(request.options().cashOnDeliveryNotAllowed());
    }

    @Test
    void toBuilder_whenRebuilt_preservesFields() {
        ReturnPolicyRequest original = ReturnPolicyRequest.builder()
                .name(NAME)
                .fulfillment(true)
                .availability(ReturnPolicyAvailability.full())
                .withdrawalPeriod(WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.BUYER)
                .address(address())
                .options(options())
                .build();

        ReturnPolicyRequest copy = original.toBuilder().build();

        assertEquals(original.name(), copy.name());
        assertEquals(original.fulfillment(), copy.fulfillment());
        assertEquals(original.withdrawalPeriod(), copy.withdrawalPeriod());
        assertEquals(original.returnCost(), copy.returnCost());
        assertEquals(original.address().street(), copy.address().street());
        assertEquals(original.options().collectBySellerOnly(), copy.options().collectBySellerOnly());
    }

    @Test
    void build_whenNameMissing_throws() {
        var builder = ReturnPolicyRequest.builder()
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.full());
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void build_whenNameTooLong_throws() {
        var builder = ReturnPolicyRequest.builder()
                .name("a".repeat(OVER_NAME_LIMIT))
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.full());
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void build_whenNameAtLimit_succeeds() {
        String maxName = "a".repeat(NAME_AT_LIMIT);
        ReturnPolicyRequest request = ReturnPolicyRequest.builder()
                .name(maxName)
                .fulfillment(false)
                .availability(ReturnPolicyAvailability.full())
                .build();
        assertEquals(maxName, request.name());
    }

    @Test
    void build_whenFulfillmentMissing_throws() {
        var builder = ReturnPolicyRequest.builder()
                .name(NAME)
                .availability(ReturnPolicyAvailability.full());
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_FULFILLMENT));
    }

    @Test
    void build_whenAvailabilityMissing_throws() {
        var builder = ReturnPolicyRequest.builder()
                .name(NAME)
                .fulfillment(false);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_AVAILABILITY));
    }

    // ---- update request (no fulfillment flag) ----

    @Test
    void update_build_whenRequiredFieldsOnly_succeeds() {
        ReturnPolicyUpdateRequest request = ReturnPolicyUpdateRequest.builder()
                .name(NAME)
                .availability(ReturnPolicyAvailability.disabled(ReturnRestrictionCause.CUSTOM_ITEM))
                .build();

        assertEquals(NAME, request.name());
        assertEquals(ReturnRange.DISABLED, request.availability().range());
        assertNull(request.withdrawalPeriod());
    }

    @Test
    void update_toBuilder_whenRebuilt_preservesFields() {
        ReturnPolicyUpdateRequest original = ReturnPolicyUpdateRequest.builder()
                .name(NAME)
                .availability(ReturnPolicyAvailability.full())
                .withdrawalPeriod(WITHDRAWAL_PERIOD)
                .returnCost(ReturnCostCoveredBy.SELLER)
                .address(address())
                .contact(new ReturnPolicyContact(null, "seller@example.com"))
                .options(options())
                .build();

        ReturnPolicyUpdateRequest copy = original.toBuilder().build();

        assertEquals(original.name(), copy.name());
        assertEquals(original.withdrawalPeriod(), copy.withdrawalPeriod());
        assertEquals(original.returnCost(), copy.returnCost());
        assertEquals(original.contact().email(), copy.contact().email());
    }

    @Test
    void update_build_whenNameMissing_throws() {
        var builder = ReturnPolicyUpdateRequest.builder()
                .availability(ReturnPolicyAvailability.full());
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
    }

    @Test
    void update_build_whenNameTooLong_throws() {
        var builder = ReturnPolicyUpdateRequest.builder()
                .name("a".repeat(OVER_NAME_LIMIT))
                .availability(ReturnPolicyAvailability.full());
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_NAME));
        assertTrue(failure.getMessage().contains(LENGTH_WORD));
    }

    @Test
    void update_build_whenAvailabilityMissing_throws() {
        var builder = ReturnPolicyUpdateRequest.builder().name(NAME);
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(FIELD_AVAILABILITY));
    }

    // ---- availability value type ----

    @Test
    void availability_factories_produceExpectedRangeAndCause() {
        assertEquals(ReturnRange.FULL, ReturnPolicyAvailability.full().range());
        assertNull(ReturnPolicyAvailability.full().restrictionCause());
        assertEquals(ReturnRestrictionCause.PRESS,
                ReturnPolicyAvailability.restricted(ReturnRestrictionCause.PRESS).restrictionCause());
        assertEquals(ReturnRange.DISABLED,
                ReturnPolicyAvailability.disabled(ReturnRestrictionCause.ALCOHOL).range());
    }
}
