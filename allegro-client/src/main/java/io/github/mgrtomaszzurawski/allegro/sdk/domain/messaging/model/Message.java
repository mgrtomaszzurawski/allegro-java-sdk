/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAdditionalInformationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageAttachmentInfoRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageRaw;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A single message within a thread, as returned by the message-center facade.
 *
 * @param id server-assigned message identifier
 * @param status delivery/processing status
 * @param type origin/kind of the message
 * @param createdAt when the message was created
 * @param threadId id of the thread this message belongs to
 * @param author who wrote the message
 * @param text the message body
 * @param subject the message subject, or {@code null} when none
 * @param relatesTo the offer/order the message relates to (parts may be absent)
 * @param hasAdditionalAttachments whether attachments exist beyond those listed
 *     inline (e.g. large files fetched separately)
 * @param attachments inline attachments; never {@code null}, possibly empty
 * @param vehicleVin vehicle identification number carried by vehicle-related
 *     messages, or {@code null}
 *
 * @since 0.2.0
 */
public record Message(
        String id,
        MessageStatus status,
        MessageType type,
        OffsetDateTime createdAt,
        String threadId,
        MessageAuthor author,
        String text,
        @Nullable String subject,
        RelatedObject relatesTo,
        boolean hasAdditionalAttachments,
        List<MessageAttachment> attachments,
        @Nullable String vehicleVin) {

    public Message {
        attachments = List.copyOf(attachments);
    }

    /** Map the generated Layer-1 DTO to the public immutable record. */
    public static Message from(MessageRaw raw) {
        MessageAdditionalInformationRaw additionalInformation = raw.getAdditionalInformation();
        return new Message(
                raw.getId(),
                MessageStatus.from(raw.getStatus()),
                MessageType.from(raw.getType()),
                raw.getCreatedAt(),
                raw.getThread().getId(),
                MessageAuthor.from(raw.getAuthor()),
                raw.getText(),
                raw.getSubject(),
                RelatedObject.from(raw.getRelatesTo()),
                Boolean.TRUE.equals(raw.getHasAdditionalAttachments()),
                attachmentsOf(raw.getAttachments()),
                additionalInformation == null ? null : additionalInformation.getVin());
    }

    private static List<MessageAttachment> attachmentsOf(
            @Nullable List<MessageAttachmentInfoRaw> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream().map(MessageAttachment::from).toList();
    }
}
