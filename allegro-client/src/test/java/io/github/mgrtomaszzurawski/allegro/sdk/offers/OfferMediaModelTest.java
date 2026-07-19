/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentFileRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentTypeRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAttachmentRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferAttachmentRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.OfferImageUploadResponseRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AttachmentType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.ImageFormat;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferAttachment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferImage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OfferMediaModelTest {

    private static final String LOCATION = "https://a.allegroimg.com/original/abc/x.jpg";
    private static final String FILE_NAME = "manual.pdf";
    private static final String FILE_URL = "https://a.allegroimg.com/original/att/manual.pdf";
    private static final String ATTACHMENT_ID = "att-1";

    @Test
    void imageFormat_mediaTypes() {
        assertEquals("image/jpeg", ImageFormat.JPEG.mediaType());
        assertEquals("image/png", ImageFormat.PNG.mediaType());
        assertEquals("image/webp", ImageFormat.WEBP.mediaType());
    }

    @Test
    void attachmentType_from_mapsKnownValue() {
        assertEquals(AttachmentType.USER_MANUAL, AttachmentType.from(AttachmentTypeRaw.USER_MANUAL));
    }

    @Test
    void attachmentType_from_whenUnknownWireValue_degradesToUnknown() {
        assertEquals(AttachmentType.UNKNOWN,
                AttachmentType.from(AttachmentTypeRaw.UNKNOWN_DEFAULT_OPEN_API));
    }

    @Test
    void attachmentType_from_whenNull_isUnknown() {
        assertEquals(AttachmentType.UNKNOWN, AttachmentType.from(null));
    }

    @Test
    void attachmentType_toRaw_mapsKnownValue() {
        assertEquals(AttachmentTypeRaw.ENERGY_LABEL, AttachmentType.ENERGY_LABEL.toRaw());
    }

    @Test
    void attachmentType_toRaw_whenUnknown_throws() {
        assertThrows(IllegalStateException.class, AttachmentType.UNKNOWN::toRaw);
    }

    @Test
    void attachmentDeclaration_toRaw_writesTypeAndFileName() {
        // when
        OfferAttachmentRequestRaw raw = AttachmentDeclaration
                .of(AttachmentType.USER_MANUAL, FILE_NAME).toRaw();

        // then
        assertEquals(AttachmentTypeRaw.USER_MANUAL, raw.getType());
        assertEquals(FILE_NAME, raw.getFile().getName());
    }

    @Test
    void attachmentDeclaration_whenTypeNull_throws() {
        assertThrows(NullPointerException.class,
                () -> AttachmentDeclaration.of(null, FILE_NAME));
    }

    @Test
    void offerImage_from_mapsLocationAndExpiry() {
        // given
        OffsetDateTime expiry = OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OfferImageUploadResponseRaw raw = new OfferImageUploadResponseRaw()
                .location(LOCATION).expiresAt(expiry);

        // when
        OfferImage image = OfferImage.from(raw);

        // then
        assertEquals(LOCATION, image.location());
        assertEquals(expiry, image.expiresAt());
    }

    @Test
    void offerAttachment_from_mapsIdTypeAndFile() {
        // given
        OfferAttachmentRaw raw = new OfferAttachmentRaw()
                .id(ATTACHMENT_ID)
                .type(AttachmentTypeRaw.USER_MANUAL)
                ._file(new AttachmentFileRaw().name(FILE_NAME).url(FILE_URL));

        // when
        OfferAttachment attachment = OfferAttachment.from(raw);

        // then
        assertEquals(ATTACHMENT_ID, attachment.id());
        assertEquals(AttachmentType.USER_MANUAL, attachment.type());
        assertEquals(FILE_NAME, attachment.fileName());
        assertEquals(FILE_URL, attachment.fileUrl());
    }

    @Test
    void offerAttachment_from_whenNoFile_leavesFileFieldsNull() {
        // given — a freshly declared attachment before upload has no file
        OfferAttachmentRaw raw = new OfferAttachmentRaw()
                .id(ATTACHMENT_ID).type(AttachmentTypeRaw.USER_MANUAL);

        // when
        OfferAttachment attachment = OfferAttachment.from(raw);

        // then
        assertNull(attachment.fileName());
        assertNull(attachment.fileUrl());
    }
}
