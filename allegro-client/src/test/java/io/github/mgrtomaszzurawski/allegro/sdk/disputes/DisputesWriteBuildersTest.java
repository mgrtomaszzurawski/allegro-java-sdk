/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.disputes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.ClaimStatusChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ClaimStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueMessageType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Round-trip and fail-fast coverage of the disputes write builders:
 * {@link IssueMessageRequest}, {@link ClaimStatusChange} and
 * {@link IssueAttachmentDeclaration}. One failure test per required-field rule,
 * plus {@code toBuilder} preservation.
 */
class DisputesWriteBuildersTest {

    private static final String MESSAGE_TEXT = "Thank you, a replacement ships today.";
    private static final String ATTACHMENT_ID = "att-1";
    private static final String SECOND_ATTACHMENT_ID = "att-2";
    private static final String CHANGE_MESSAGE = "We accept a partial refund.";
    private static final String REFUND_AMOUNT = "12.50";
    private static final String CURRENCY_PLN = "PLN";
    private static final String FILE_NAME = "evidence.jpg";
    private static final int FILE_SIZE = 2048;
    private static final int ZERO_SIZE = 0;
    private static final int OVER_LIMIT_LENGTH = IssueMessageRequest.TEXT_MAX_LENGTH + 1;

    // ---- IssueMessageRequest ----

    @Test
    void issueMessageRequest_whenTextOnly_defaultsTypeToRegular() {
        // when
        IssueMessageRequest request = IssueMessageRequest.builder().text(MESSAGE_TEXT).build();

        // then
        assertEquals(MESSAGE_TEXT, request.text());
        assertEquals(IssueMessageType.REGULAR, request.type());
        assertTrue(request.attachmentIds().isEmpty());
    }

    @Test
    void issueMessageRequest_whenAttachmentOnly_buildsWithoutText() {
        // when — a message may carry only attachments
        IssueMessageRequest request = IssueMessageRequest.builder()
                .attachment(ATTACHMENT_ID)
                .attachment(SECOND_ATTACHMENT_ID)
                .build();

        // then
        assertNull(request.text());
        assertEquals(List.of(ATTACHMENT_ID, SECOND_ATTACHMENT_ID), request.attachmentIds());
    }

    @Test
    void issueMessageRequest_whenTypeOverridden_keepsIt() {
        // when
        IssueMessageRequest request = IssueMessageRequest.builder()
                .text(MESSAGE_TEXT)
                .type(IssueMessageType.END_REQUEST)
                .build();

        // then
        assertEquals(IssueMessageType.END_REQUEST, request.type());
    }

    @Test
    void issueMessageRequest_whenNeitherTextNorAttachment_throws() {
        // then
        assertThrows(IllegalStateException.class, () -> IssueMessageRequest.builder().build());
    }

    @Test
    void issueMessageRequest_whenBlankTextAndNoAttachment_throws() {
        // then — blank text does not count as present
        assertThrows(IllegalStateException.class,
                () -> IssueMessageRequest.builder().text("   ").build());
    }

    @Test
    void issueMessageRequest_whenTextTooLong_throws() {
        // given
        String tooLong = "x".repeat(OVER_LIMIT_LENGTH);

        // then
        assertThrows(IllegalStateException.class,
                () -> IssueMessageRequest.builder().text(tooLong).build());
    }

    @Test
    void issueMessageRequest_whenNullType_throws() {
        // then
        assertThrows(NullPointerException.class,
                () -> IssueMessageRequest.builder().text(MESSAGE_TEXT).type(null));
    }

    @Test
    void issueMessageRequest_whenNullAttachment_throws() {
        // then
        assertThrows(NullPointerException.class,
                () -> IssueMessageRequest.builder().attachment(null));
    }

    @Test
    void issueMessageRequest_toBuilder_preservesEveryField() {
        // given
        IssueMessageRequest original = IssueMessageRequest.builder()
                .text(MESSAGE_TEXT)
                .type(IssueMessageType.RETURN_NOT_REQUIRED)
                .attachment(ATTACHMENT_ID)
                .build();

        // when
        IssueMessageRequest copy = original.toBuilder().build();

        // then
        assertEquals(original.text(), copy.text());
        assertEquals(original.type(), copy.type());
        assertEquals(original.attachmentIds(), copy.attachmentIds());
    }

    // ---- ClaimStatusChange ----

    @Test
    void claimStatusChange_whenStatusAndMessage_buildsWithoutRefund() {
        // when
        ClaimStatusChange change = ClaimStatusChange.builder()
                .status(ClaimStatus.ACCEPTED_REFUND)
                .message(CHANGE_MESSAGE)
                .build();

        // then
        assertEquals(ClaimStatus.ACCEPTED_REFUND, change.status());
        assertEquals(CHANGE_MESSAGE, change.message());
        assertNull(change.partialRefund());
    }

    @Test
    void claimStatusChange_whenPartialRefund_keepsAmount() {
        // when
        ClaimStatusChange change = ClaimStatusChange.builder()
                .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
                .message(CHANGE_MESSAGE)
                .partialRefund(Money.of(REFUND_AMOUNT, CURRENCY_PLN))
                .build();

        // then
        assertEquals(REFUND_AMOUNT, change.partialRefund().amount());
        assertEquals(CURRENCY_PLN, change.partialRefund().currency());
    }

    @Test
    void claimStatusChange_whenNoStatus_throws() {
        // then
        assertThrows(NullPointerException.class,
                () -> ClaimStatusChange.builder().message(CHANGE_MESSAGE).build());
    }

    @Test
    void claimStatusChange_whenBlankMessage_throws() {
        // then
        assertThrows(IllegalStateException.class,
                () -> ClaimStatusChange.builder().status(ClaimStatus.REJECTED_OTHER).message(" ").build());
    }

    @Test
    void claimStatusChange_toBuilder_preservesEveryField() {
        // given
        ClaimStatusChange original = ClaimStatusChange.builder()
                .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
                .message(CHANGE_MESSAGE)
                .partialRefund(Money.of(REFUND_AMOUNT, CURRENCY_PLN))
                .build();

        // when
        ClaimStatusChange copy = original.toBuilder().build();

        // then
        assertEquals(original.status(), copy.status());
        assertEquals(original.message(), copy.message());
        assertEquals(original.partialRefund(), copy.partialRefund());
    }

    // ---- IssueAttachmentDeclaration ----

    @Test
    void issueAttachmentDeclaration_whenValid_keepsFilenameAndSize() {
        // when
        IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
                .filename(FILE_NAME)
                .size(FILE_SIZE)
                .build();

        // then
        assertEquals(FILE_NAME, declaration.filename());
        assertEquals(FILE_SIZE, declaration.size());
    }

    @Test
    void issueAttachmentDeclaration_whenNoFilename_throws() {
        // then
        assertThrows(IllegalStateException.class,
                () -> IssueAttachmentDeclaration.builder().size(FILE_SIZE).build());
    }

    @Test
    void issueAttachmentDeclaration_whenNoSize_throws() {
        // then
        assertThrows(IllegalStateException.class,
                () -> IssueAttachmentDeclaration.builder().filename(FILE_NAME).build());
    }

    @Test
    void issueAttachmentDeclaration_whenSizeBelowMinimum_throws() {
        // then — size must be at least one byte
        assertThrows(IllegalStateException.class,
                () -> IssueAttachmentDeclaration.builder().filename(FILE_NAME).size(ZERO_SIZE).build());
    }

    @Test
    void issueAttachmentDeclaration_toBuilder_preservesEveryField() {
        // given
        IssueAttachmentDeclaration original = IssueAttachmentDeclaration.builder()
                .filename(FILE_NAME)
                .size(FILE_SIZE)
                .build();

        // when
        IssueAttachmentDeclaration copy = original.toBuilder().build();

        // then
        assertEquals(original.filename(), copy.filename());
        assertEquals(original.size(), copy.size());
    }
}
