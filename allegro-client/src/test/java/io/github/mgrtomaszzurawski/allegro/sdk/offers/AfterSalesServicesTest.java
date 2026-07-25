/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.AfterSalesServicesRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ImpliedWarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ReturnPolicyRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.WarrantyRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AfterSalesServices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.NamedReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AfterSalesServicesTest {

    private static final String IMPLIED_WARRANTY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RETURN_POLICY_ID = "22222222-2222-2222-2222-222222222222";
    private static final String WARRANTY_ID = "33333333-3333-3333-3333-333333333333";
    private static final String IMPLIED_WARRANTY_NAME = "Standard implied warranty";
    private static final String MALFORMED_ID = "not-a-uuid";

    @Test
    void build_whenAllReferencesById_exposesEachValue() {
        // when
        AfterSalesServices services = AfterSalesServices.builder()
                .impliedWarranty(NamedReference.byId(IMPLIED_WARRANTY_ID))
                .returnPolicy(NamedReference.byId(RETURN_POLICY_ID))
                .warranty(NamedReference.byId(WARRANTY_ID))
                .build();

        // then
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarranty().id());
        assertEquals(RETURN_POLICY_ID, services.returnPolicy().id());
        assertEquals(WARRANTY_ID, services.warranty().id());
    }

    @Test
    void build_whenImpliedWarrantyByName_keepsTheNameFormWithoutUuidCheck() {
        // when a reference is given by name (not a UUID), it is accepted as-is
        AfterSalesServices services = AfterSalesServices.builder()
                .impliedWarranty(NamedReference.byName(IMPLIED_WARRANTY_NAME))
                .build();

        // then the name form is carried and no id is set
        assertEquals(IMPLIED_WARRANTY_NAME, services.impliedWarranty().name());
        assertNull(services.impliedWarranty().id());
    }

    @Test
    void build_whenNoReferencesSet_leavesEveryFieldNull() {
        // when
        AfterSalesServices services = AfterSalesServices.builder().build();

        // then
        assertNull(services.impliedWarranty());
        assertNull(services.returnPolicy());
        assertNull(services.warranty());
    }

    @Test
    void toBuilder_whenRebuilt_preservesEveryField() {
        // given
        AfterSalesServices original = AfterSalesServices.builder()
                .impliedWarranty(NamedReference.byId(IMPLIED_WARRANTY_ID))
                .returnPolicy(NamedReference.byId(RETURN_POLICY_ID))
                .warranty(NamedReference.byId(WARRANTY_ID))
                .build();

        // when
        AfterSalesServices copy = original.toBuilder().build();

        // then
        assertEquals(original.impliedWarranty(), copy.impliedWarranty());
        assertEquals(original.returnPolicy(), copy.returnPolicy());
        assertEquals(original.warranty(), copy.warranty());
    }

    @Test
    void from_whenResponsePresent_mapsEveryUuidAsAnIdReference() {
        // given — a generated after-sales response block carrying UUID ids
        AfterSalesServicesRaw raw = new AfterSalesServicesRaw()
                .impliedWarranty(new ImpliedWarrantyRaw().id(UUID.fromString(IMPLIED_WARRANTY_ID)))
                .returnPolicy(new ReturnPolicyRaw().id(UUID.fromString(RETURN_POLICY_ID)))
                .warranty(new WarrantyRaw().id(UUID.fromString(WARRANTY_ID)));

        // when
        AfterSalesServices services = AfterSalesServices.from(raw);

        // then each response id maps to an id reference
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarranty().id());
        assertEquals(RETURN_POLICY_ID, services.returnPolicy().id());
        assertEquals(WARRANTY_ID, services.warranty().id());
    }

    @Test
    void from_whenNull_returnsNull() {
        assertNull(AfterSalesServices.from(null));
    }

    @Test
    void builder_whenMalformedUuidById_throwsIllegalArgumentFailFast() {
        // then — a bad id is rejected at the point it is set, not deep in create()
        AfterSalesServices.Builder builder = AfterSalesServices.builder();
        NamedReference badReference = NamedReference.byId(MALFORMED_ID);
        assertThrows(IllegalArgumentException.class, () -> builder.impliedWarranty(badReference));
        assertThrows(IllegalArgumentException.class, () -> builder.returnPolicy(badReference));
        assertThrows(IllegalArgumentException.class, () -> builder.warranty(badReference));
    }

    @Test
    void from_whenPolicyAbsent_leavesThatReferenceNull() {
        // given — only an implied warranty is attached
        AfterSalesServicesRaw raw = new AfterSalesServicesRaw()
                .impliedWarranty(new ImpliedWarrantyRaw().id(UUID.fromString(IMPLIED_WARRANTY_ID)));

        // when
        AfterSalesServices services = AfterSalesServices.from(raw);

        // then
        assertEquals(IMPLIED_WARRANTY_ID, services.impliedWarranty().id());
        assertNull(services.returnPolicy());
        assertNull(services.warranty());
    }
}
