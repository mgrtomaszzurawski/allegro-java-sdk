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
import org.junit.jupiter.api.function.Executable;

/**
 * Round-trip and fail-fast coverage of the disputes write builders:
 * {@link IssueMessageRequest}, {@link ClaimStatusChange} and
 * {@link IssueAttachmentDeclaration}. Each failure test asserts the specific message
 * (the builders have several overlapping {@link IllegalStateException} paths, so a
 * type-only assertion could pass for the wrong validation firing), plus
 * {@code toBuilder} preservation.
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
    private static final int OVER_LIMIT_SIZE = IssueAttachmentDeclaration.MAX_SIZE_BYTES + 1;

    // Distinctive fragments of each fail-fast message, so the right rule is proven.
    private static final String ERR_MSG_EMPTY = "at least one attachment";
    private static final String ERR_MSG_TEXT_TOO_LONG = "text must be at most";
    private static final String ERR_MSG_MESSAGE_REQUIRED = "message is required";
    private static final String ERR_MSG_FILENAME = "filename is required";
    private static final String ERR_MSG_SIZE_REQUIRED = "size is required";
    private static final String ERR_MSG_SIZE_SMALL = "size must be at least";
    private static final String ERR_MSG_SIZE_LARGE = "size must be at most";

    private static void assertMessageContains(Executable action, String fragment) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, action);
        assertTrue(failure.getMessage().contains(fragment),
                () -> "expected message to contain '" + fragment + "' but was: " + failure.getMessage());
    }

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
    void issueMessageRequest_whenNeitherTextNorAttachment_throwsEmpty() {
        // then
        assertMessageContains(() -> IssueMessageRequest.builder().build(), ERR_MSG_EMPTY);
    }

    @Test
    void issueMessageRequest_whenBlankTextAndNoAttachment_throwsEmpty() {
        // then — blank text does not count as present
        assertMessageContains(() -> IssueMessageRequest.builder().text("   ").build(), ERR_MSG_EMPTY);
    }

    @Test
    void issueMessageRequest_whenTextTooLong_throwsTooLong() {
        // given
        String tooLong = "x".repeat(OVER_LIMIT_LENGTH);

        // then — the length rule (not the empty rule) fires
        assertMessageContains(() -> IssueMessageRequest.builder().text(tooLong).build(),
                ERR_MSG_TEXT_TOO_LONG);
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
    void claimStatusChange_whenBlankMessage_throwsMessageRequired() {
        // then
        assertMessageContains(
                () -> ClaimStatusChange.builder().status(ClaimStatus.REJECTED_OTHER).message(" ").build(),
                ERR_MSG_MESSAGE_REQUIRED);
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
    void issueAttachmentDeclaration_whenNoFilename_throwsFilenameRequired() {
        // then
        assertMessageContains(() -> IssueAttachmentDeclaration.builder().size(FILE_SIZE).build(),
                ERR_MSG_FILENAME);
    }

    @Test
    void issueAttachmentDeclaration_whenNoSize_throwsSizeRequired() {
        // then
        assertMessageContains(() -> IssueAttachmentDeclaration.builder().filename(FILE_NAME).build(),
                ERR_MSG_SIZE_REQUIRED);
    }

    @Test
    void issueAttachmentDeclaration_whenSizeBelowMinimum_throwsTooSmall() {
        // then — size must be at least one byte (not the required rule)
        assertMessageContains(
                () -> IssueAttachmentDeclaration.builder().filename(FILE_NAME).size(ZERO_SIZE).build(),
                ERR_MSG_SIZE_SMALL);
    }

    @Test
    void issueAttachmentDeclaration_whenSizeAboveMaximum_throwsTooLarge() {
        // then — size must not exceed the documented cap
        assertMessageContains(
                () -> IssueAttachmentDeclaration.builder().filename(FILE_NAME).size(OVER_LIMIT_SIZE).build(),
                ERR_MSG_SIZE_LARGE);
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
