/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationAttachmentsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NoSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationResponseSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.TextSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.SafetyInformation;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Flattening of the GPSR safety-information {@code oneOf} onto the consumer value. */
class SafetyInformationTest {

    private static final String DESCRIPTION = "Keep away from small children.";
    private static final String ATTACHMENT_ID = "att-1";

    @Test
    void from_whenNull_returnsNull() {
        // then an absent safety-information block projects to null
        assertNull(SafetyInformation.from(null));
    }

    @Test
    void from_whenText_mapsTypeAndDescription() {
        // given the TEXT oneOf form
        var raw = new ProductSetElementSafetyInformationResponseSafetyInformationRaw(
                new TextSafetyInformationRaw()
                        .type(TextSafetyInformationRaw.TypeEnum.TEXT).description(DESCRIPTION));

        // when projected
        SafetyInformation safety = SafetyInformation.from(raw);

        // then the type and free-text description map, with no attachments
        assertEquals(SafetyInformation.TEXT, safety.type());
        assertEquals(DESCRIPTION, safety.description());
        assertTrue(safety.attachmentIds().isEmpty());
    }

    @Test
    void from_whenAttachments_mapsTypeAndAttachmentIds() {
        // given the ATTACHMENTS oneOf form
        var raw = new ProductSetElementSafetyInformationResponseSafetyInformationRaw(
                new AttachmentsSafetyInformationRaw()
                        .type(AttachmentsSafetyInformationRaw.TypeEnum.ATTACHMENTS)
                        .attachments(List.of(new AttachmentsSafetyInformationAttachmentsInnerRaw().id(ATTACHMENT_ID))));

        // when projected
        SafetyInformation safety = SafetyInformation.from(raw);

        // then the attachment ids map, with no free-text description
        assertEquals(SafetyInformation.ATTACHMENTS, safety.type());
        assertNull(safety.description());
        assertEquals(List.of(ATTACHMENT_ID), safety.attachmentIds());
    }

    @Test
    void from_whenNone_mapsTypeOnly() {
        // given the explicit NONE oneOf form
        var raw = new ProductSetElementSafetyInformationResponseSafetyInformationRaw(
                new NoSafetyInformationRaw().type(NoSafetyInformationRaw.TypeEnum.NO_SAFETY_INFORMATION));

        // when projected; then only the type is present
        SafetyInformation safety = SafetyInformation.from(raw);
        assertEquals(SafetyInformation.NONE, safety.type());
        assertNull(safety.description());
        assertTrue(safety.attachmentIds().isEmpty());
    }
}
