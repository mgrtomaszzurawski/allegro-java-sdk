/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.MessageFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.NewMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.ReplyRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast coverage of the message-center request builders:
 * {@link NewMessageRequest}, {@link ReplyRequest}, {@link AttachmentDeclaration}
 * and {@link MessageFilter}. Each required field has a dedicated failure test
 * and each server-side limit a boundary test.
 */
class MessagingBuildersTest {

    private static final String RECIPIENT = "buyer-login";
    private static final String ORDER_ID = "order-1";
    private static final String TEXT = "Thanks for your order.";
    private static final String ATTACHMENT_A = "att-a";
    private static final String ATTACHMENT_B = "att-b";
    private static final String FILE_NAME = "invoice.pdf";
    private static final long FILE_SIZE = 2048L;

    private static final String ERR_RECIPIENT_REQUIRED = "recipientLogin is required";
    private static final String ERR_ORDER_REQUIRED = "orderId is required";
    private static final String ERR_TEXT_REQUIRED = "text is required";
    private static final String ERR_FILENAME_REQUIRED = "filename is required";
    private static final String ERR_SIZE_REQUIRED = "size is required";
    private static final String ERR_SIZE_NEGATIVE = "size must not be negative";
    private static final String AT_MOST_MARKER = "at most";

    private static final OffsetDateTime AFTER =
            OffsetDateTime.of(2026, 7, 10, 8, 30, 15, 0, ZoneOffset.UTC);
    private static final OffsetDateTime BEFORE =
            OffsetDateTime.of(2026, 7, 18, 8, 30, 15, 0, ZoneOffset.UTC);

    // ---- NewMessageRequest ----

    @Test
    void newMessageRequest_whenRequiredFieldsOnly_buildsWithNoAttachments() {
        // when
        NewMessageRequest request = NewMessageRequest.builder()
                .recipientLogin(RECIPIENT).orderId(ORDER_ID).text(TEXT).build();

        // then
        assertEquals(RECIPIENT, request.recipientLogin());
        assertEquals(ORDER_ID, request.orderId());
        assertEquals(TEXT, request.text());
        assertTrue(request.attachmentIds().isEmpty());
    }

    @Test
    void newMessageRequest_whenAllFieldsSet_carriesAttachmentsInOrder() {
        // when
        NewMessageRequest request = NewMessageRequest.builder()
                .recipientLogin(RECIPIENT).orderId(ORDER_ID).text(TEXT)
                .attachment(ATTACHMENT_A).attachment(ATTACHMENT_B).build();

        // then
        assertEquals(List.of(ATTACHMENT_A, ATTACHMENT_B), request.attachmentIds());
    }

    @Test
    void newMessageRequest_toBuilder_preservesAllFields() {
        // given
        NewMessageRequest original = NewMessageRequest.builder()
                .recipientLogin(RECIPIENT).orderId(ORDER_ID).text(TEXT).attachment(ATTACHMENT_A).build();

        // when
        NewMessageRequest copy = original.toBuilder().build();

        // then
        assertEquals(RECIPIENT, copy.recipientLogin());
        assertEquals(ORDER_ID, copy.orderId());
        assertEquals(TEXT, copy.text());
        assertEquals(List.of(ATTACHMENT_A), copy.attachmentIds());
    }

    @Test
    void newMessageRequest_whenRecipientMissing_throwsIllegalState() {
        // given
        NewMessageRequest.Builder builder = NewMessageRequest.builder().orderId(ORDER_ID).text(TEXT);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_RECIPIENT_REQUIRED, failure.getMessage());
    }

    @Test
    void newMessageRequest_whenOrderMissing_throwsIllegalState() {
        // given
        NewMessageRequest.Builder builder =
                NewMessageRequest.builder().recipientLogin(RECIPIENT).text(TEXT);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_ORDER_REQUIRED, failure.getMessage());
    }

    @Test
    void newMessageRequest_whenTextMissing_throwsIllegalState() {
        // given
        NewMessageRequest.Builder builder =
                NewMessageRequest.builder().recipientLogin(RECIPIENT).orderId(ORDER_ID);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_TEXT_REQUIRED, failure.getMessage());
    }

    @Test
    void newMessageRequest_whenTextExceedsMax_throwsIllegalState() {
        // given
        String tooLong = "x".repeat(NewMessageRequest.TEXT_MAX_LENGTH + 1);
        NewMessageRequest.Builder builder =
                NewMessageRequest.builder().recipientLogin(RECIPIENT).orderId(ORDER_ID).text(tooLong);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(AT_MOST_MARKER));
    }

    @Test
    void newMessageRequest_whenTextAtMax_builds() {
        // given — exactly the limit is accepted (two-sided boundary)
        String atMax = "x".repeat(NewMessageRequest.TEXT_MAX_LENGTH);

        // when
        NewMessageRequest request = NewMessageRequest.builder()
                .recipientLogin(RECIPIENT).orderId(ORDER_ID).text(atMax).build();

        // then
        assertEquals(NewMessageRequest.TEXT_MAX_LENGTH, request.text().length());
    }

    @Test
    void newMessageRequest_whenAttachmentIdNull_throwsNpe() {
        // then
        assertThrows(NullPointerException.class,
                () -> NewMessageRequest.builder().attachment(null));
    }

    // ---- ReplyRequest ----

    @Test
    void replyRequest_whenTextOnly_builds() {
        // when
        ReplyRequest reply = ReplyRequest.builder().text(TEXT).build();

        // then
        assertEquals(TEXT, reply.text());
        assertTrue(reply.attachmentIds().isEmpty());
    }

    @Test
    void replyRequest_whenAttachmentsAdded_carried() {
        // when
        ReplyRequest reply = ReplyRequest.builder().text(TEXT).attachment(ATTACHMENT_A).build();

        // then
        assertEquals(List.of(ATTACHMENT_A), reply.attachmentIds());
    }

    @Test
    void replyRequest_toBuilder_preserves() {
        // given
        ReplyRequest original = ReplyRequest.builder().text(TEXT).attachment(ATTACHMENT_A).build();

        // when
        ReplyRequest copy = original.toBuilder().build();

        // then
        assertEquals(TEXT, copy.text());
        assertEquals(List.of(ATTACHMENT_A), copy.attachmentIds());
    }

    @Test
    void replyRequest_whenTextMissing_throwsIllegalState() {
        // given
        ReplyRequest.Builder builder = ReplyRequest.builder();

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_TEXT_REQUIRED, failure.getMessage());
    }

    @Test
    void replyRequest_whenTextExceedsMax_throwsIllegalState() {
        // given
        String tooLong = "x".repeat(ReplyRequest.TEXT_MAX_LENGTH + 1);
        ReplyRequest.Builder builder = ReplyRequest.builder().text(tooLong);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(AT_MOST_MARKER));
    }

    @Test
    void replyRequest_whenAttachmentIdNull_throwsNpe() {
        // then
        assertThrows(NullPointerException.class, () -> ReplyRequest.builder().attachment(null));
    }

    // ---- AttachmentDeclaration ----

    @Test
    void attachmentDeclaration_whenValid_builds() {
        // when
        AttachmentDeclaration declaration =
                AttachmentDeclaration.builder().filename(FILE_NAME).size(FILE_SIZE).build();

        // then
        assertEquals(FILE_NAME, declaration.filename());
        assertEquals(FILE_SIZE, declaration.size());
    }

    @Test
    void attachmentDeclaration_toBuilder_preserves() {
        // given
        AttachmentDeclaration original =
                AttachmentDeclaration.builder().filename(FILE_NAME).size(FILE_SIZE).build();

        // when
        AttachmentDeclaration copy = original.toBuilder().build();

        // then
        assertEquals(FILE_NAME, copy.filename());
        assertEquals(FILE_SIZE, copy.size());
    }

    @Test
    void attachmentDeclaration_whenFilenameMissing_throwsIllegalState() {
        // given
        AttachmentDeclaration.Builder builder = AttachmentDeclaration.builder().size(FILE_SIZE);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_FILENAME_REQUIRED, failure.getMessage());
    }

    @Test
    void attachmentDeclaration_whenSizeMissing_throwsIllegalState() {
        // given
        AttachmentDeclaration.Builder builder = AttachmentDeclaration.builder().filename(FILE_NAME);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_SIZE_REQUIRED, failure.getMessage());
    }

    @Test
    void attachmentDeclaration_whenSizeNegative_throwsIllegalState() {
        // given
        AttachmentDeclaration.Builder builder =
                AttachmentDeclaration.builder().filename(FILE_NAME).size(-1L);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(ERR_SIZE_NEGATIVE, failure.getMessage());
    }

    @Test
    void attachmentDeclaration_whenSizeAtMax_builds() {
        // given — exactly the limit is accepted (two-sided boundary)
        AttachmentDeclaration declaration = AttachmentDeclaration.builder()
                .filename(FILE_NAME).size(AttachmentDeclaration.MAX_SIZE_BYTES).build();

        // then
        assertEquals(AttachmentDeclaration.MAX_SIZE_BYTES, declaration.size());
    }

    @Test
    void attachmentDeclaration_whenSizeExceedsMax_throwsIllegalState() {
        // given
        AttachmentDeclaration.Builder builder = AttachmentDeclaration.builder()
                .filename(FILE_NAME).size(AttachmentDeclaration.MAX_SIZE_BYTES + 1L);

        // then
        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(failure.getMessage().contains(AT_MOST_MARKER));
    }

    // ---- MessageFilter ----

    @Test
    void messageFilter_none_hasNoBounds() {
        // when
        MessageFilter filter = MessageFilter.none();

        // then
        assertNull(filter.before());
        assertNull(filter.after());
    }

    @Test
    void messageFilter_whenBoundsSet_exposesThem() {
        // when
        MessageFilter filter = MessageFilter.builder().before(BEFORE).after(AFTER).build();

        // then
        assertEquals(BEFORE, filter.before());
        assertEquals(AFTER, filter.after());
    }

    @Test
    void messageFilter_toBuilder_preserves() {
        // given
        MessageFilter original = MessageFilter.builder().before(BEFORE).after(AFTER).build();

        // when
        MessageFilter copy = original.toBuilder().build();

        // then
        assertEquals(BEFORE, copy.before());
        assertEquals(AFTER, copy.after());
    }
}
