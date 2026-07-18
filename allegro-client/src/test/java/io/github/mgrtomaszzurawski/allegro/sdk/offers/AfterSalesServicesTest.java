/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AfterSalesServicesTest {

    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RETURN_POLICY_ID = "22222222-2222-2222-2222-222222222222";
    private static final String WARRANTY_ID = "33333333-3333-3333-3333-333333333333";

    @Test
    void build_whenAllIdsSet_exposesEachValue() {
        // when
        AfterSalesServices services = AfterSalesServices.builder()
                .impliedWarrantyId(IMPLIED_WARRANTY_ID)
                .returnPolicyId(RETURN_POLICY_ID)
                .warrantyId(WARRANTY_ID)
                .build();

        // then
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarrantyId());
        assertEquals(RETURN_POLICY_ID, services.returnPolicyId());
        assertEquals(WARRANTY_ID, services.warrantyId());
    }

    @Test
    void build_whenNoIdsSet_leavesEveryFieldNull() {
        // when
        AfterSalesServices services = AfterSalesServices.builder().build();

        // then
        assertNull(services.impliedWarrantyId());
        assertNull(services.returnPolicyId());
        assertNull(services.warrantyId());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        AfterSalesServices original = AfterSalesServices.builder()
                .impliedWarrantyId(IMPLIED_WARRANTY_ID)
                .returnPolicyId(RETURN_POLICY_ID)
                .warrantyId(WARRANTY_ID)
                .build();

        // when
        AfterSalesServices copy = original.toBuilder().build();

        // then
        assertEquals(original.impliedWarrantyId(), copy.impliedWarrantyId());
        assertEquals(original.returnPolicyId(), copy.returnPolicyId());
        assertEquals(original.warrantyId(), copy.warrantyId());
    }

    @Test
    void from_whenResponsePresent_mapsEveryUuidAsString() {
        // given — a generated after-sales response block carrying UUID ids
        AfterSalesServicesRaw raw = new AfterSalesServicesRaw()
                .impliedWarranty(new ImpliedWarrantyRaw().id(UUID.fromString(IMPLIED_WARRANTY_ID)))
                .returnPolicy(new ReturnPolicyRaw().id(UUID.fromString(RETURN_POLICY_ID)))
                .warranty(new WarrantyRaw().id(UUID.fromString(WARRANTY_ID)));

        // when
        AfterSalesServices services = AfterSalesServices.from(raw);

        // then
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarrantyId());
        assertEquals(RETURN_POLICY_ID, services.returnPolicyId());
        assertEquals(WARRANTY_ID, services.warrantyId());
    }

    @Test
    void from_whenNull_returnsNull() {
        assertNull(AfterSalesServices.from(null));
    }

    @Test
    void from_whenPolicyAbsent_leavesThatIdNull() {
        // given — only an implied warranty is attached
        AfterSalesServicesRaw raw = new AfterSalesServicesRaw()
                .impliedWarranty(new ImpliedWarrantyRaw().id(UUID.fromString(IMPLIED_WARRANTY_ID)));

        // when
        AfterSalesServices services = AfterSalesServices.from(raw);

        // then
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarrantyId());
        assertNull(services.returnPolicyId());
        assertNull(services.warrantyId());
    }
}
