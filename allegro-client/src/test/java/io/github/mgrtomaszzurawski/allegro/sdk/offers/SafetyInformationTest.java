/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationAttachmentsInnerRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentsSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NoSafetyInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ProductSetElementSafetyInformationRequestSafetyInformationRaw;
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
    void from_whenAttachmentIdMissing_dropsNullWithoutThrowing() {
        // given an ATTACHMENTS form whose attachment carries no id (spec-legal: id is not required)
        var raw = new ProductSetElementSafetyInformationResponseSafetyInformationRaw(
                new AttachmentsSafetyInformationRaw()
                        .type(AttachmentsSafetyInformationRaw.TypeEnum.ATTACHMENTS)
                        .attachments(List.of(new AttachmentsSafetyInformationAttachmentsInnerRaw())));

        // when projected; then the null id is dropped and the read does not throw
        // (the List.copyOf in the canonical constructor rejects null elements)
        SafetyInformation safety = SafetyInformation.from(raw);
        assertEquals(SafetyInformation.ATTACHMENTS, safety.type());
        assertTrue(safety.attachmentIds().isEmpty());
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

    @Test
    void text_whenBuilt_carriesTheTypeAndDescription() {
        // when a free-text safety declaration is built
        SafetyInformation safety = SafetyInformation.text(DESCRIPTION);

        // then the value carries the TEXT type and the description, with no attachments
        assertEquals(SafetyInformation.TEXT, safety.type());
        assertEquals(DESCRIPTION, safety.description());
        assertTrue(safety.attachmentIds().isEmpty());
    }

    @Test
    void text_whenDescriptionNull_throws() {
        // then the TEXT factory rejects a null description
        assertThrows(NullPointerException.class, () -> SafetyInformation.text(null));
    }

    @Test
    void text_whenDescriptionBlank_throws() {
        // then the TEXT factory rejects a blank description (Allegro rejects empty GPSR text)
        assertThrows(IllegalArgumentException.class, () -> SafetyInformation.text("   "));
    }

    @Test
    void attachments_whenBuilt_carriesTheTypeAndIds() {
        // when a document-backed safety declaration is built
        SafetyInformation safety = SafetyInformation.attachments(List.of(ATTACHMENT_ID));

        // then the value carries the ATTACHMENTS type and ids, with no description
        assertEquals(SafetyInformation.ATTACHMENTS, safety.type());
        assertNull(safety.description());
        assertEquals(List.of(ATTACHMENT_ID), safety.attachmentIds());
    }

    @Test
    void attachments_whenEmpty_throws() {
        // then the ATTACHMENTS factory requires at least one id
        assertThrows(IllegalArgumentException.class, () -> SafetyInformation.attachments(List.of()));
    }

    @Test
    void attachments_whenNull_throws() {
        // then the ATTACHMENTS factory rejects a null list
        assertThrows(NullPointerException.class, () -> SafetyInformation.attachments(null));
    }

    @Test
    void toRaw_whenText_buildsTheTextRequestForm() {
        // given a TEXT declaration
        var raw = SafetyInformation.text(DESCRIPTION).toRaw();

        // then the request oneOf carries the TEXT concrete form with the description
        TextSafetyInformationRaw text =
                assertInstanceOf(TextSafetyInformationRaw.class, raw.getActualInstance());
        assertEquals(TextSafetyInformationRaw.TypeEnum.TEXT, text.getType());
        assertEquals(DESCRIPTION, text.getDescription());
    }

    @Test
    void toRaw_whenAttachments_buildsTheAttachmentsRequestForm() {
        // given an ATTACHMENTS declaration
        var raw = SafetyInformation.attachments(List.of(ATTACHMENT_ID)).toRaw();

        // then the request oneOf carries the ATTACHMENTS concrete form with the id
        AttachmentsSafetyInformationRaw attachments =
                assertInstanceOf(AttachmentsSafetyInformationRaw.class, raw.getActualInstance());
        assertEquals(AttachmentsSafetyInformationRaw.TypeEnum.ATTACHMENTS, attachments.getType());
        assertEquals(ATTACHMENT_ID, attachments.getAttachments().get(0).getId());
    }

    @Test
    void toRaw_whenNone_throws() {
        // given a NONE value (only reachable from a read) — it cannot be sent on a request
        SafetyInformation none = new SafetyInformation(SafetyInformation.NONE, null, List.of());
        assertThrows(IllegalStateException.class, none::toRaw);
    }

    @Test
    void toRaw_whenUntyped_throws() {
        // given a value with no type discriminator — toRaw has no request form to emit
        SafetyInformation untyped = new SafetyInformation(null, null, List.of());
        assertThrows(IllegalStateException.class, untyped::toRaw);
    }
}
