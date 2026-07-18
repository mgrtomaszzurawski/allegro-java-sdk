/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging;

import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.MessageFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.NewMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.ReplyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.Message;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageThread;
import java.util.stream.Stream;

/**
 * The Allegro message center — reached via {@code AllegroClient.messaging()}.
 *
 * <p>Wraps the {@code /messaging} resource: conversation threads between the
 * authenticated user and their interlocutors, the messages within them, and
 * message attachments. The facade works with both a seller and a buyer
 * user-context token — the same SDK serves both sides of a conversation. Every
 * operation needs the {@code messaging} OAuth scope.
 *
 * <p>Streams are lazy: {@link #streamThreads()} and {@link #streamMessages(String)}
 * fetch one page at a time and only request the next page as the consumer
 * advances, so a {@code limit}/{@code findFirst} touches the server minimally.
 *
 * @since 0.2.0
 */
public interface Messaging {

    /**
     * All conversation threads of the authenticated user, most-recent first, as
     * a lazily paginated stream.
     *
     * @return a lazy stream over the user's threads; never {@code null}
     */
    Stream<MessageThread> streamThreads();

    /**
     * A single thread by its identifier.
     *
     * @param threadId the thread identifier
     * @return the thread
     */
    MessageThread thread(String threadId);

    /**
     * Marks a thread as read for the authenticated user.
     *
     * @param threadId the thread identifier
     * @return the thread in its updated (read) state
     */
    MessageThread markRead(String threadId);

    /**
     * All messages in a thread, oldest first, as a lazily paginated stream.
     *
     * @param threadId the thread identifier
     * @return a lazy stream over the thread's messages; never {@code null}
     */
    Stream<Message> streamMessages(String threadId);

    /**
     * The messages in a thread that fall within a time window, as a lazily
     * paginated stream.
     *
     * @param threadId the thread identifier
     * @param filter the time-window filter (use {@link MessageFilter#none()} for all)
     * @return a lazy stream over the matching messages; never {@code null}
     */
    Stream<Message> streamMessages(String threadId, MessageFilter filter);

    /**
     * Opens a new thread by sending the first message to a recipient in the
     * context of an order.
     *
     * @param request the recipient, order context, text, and optional attachments
     * @return the sent message
     */
    Message send(NewMessageRequest request);

    /**
     * Adds a message to an existing thread.
     *
     * @param threadId the thread to reply in
     * @param request the message text and optional attachments
     * @return the sent message
     */
    Message reply(String threadId, ReplyRequest request);

    /**
     * A single message by its identifier.
     *
     * @param messageId the message identifier
     * @return the message
     */
    Message message(String messageId);

    /**
     * Deletes a single message.
     *
     * @param messageId the message identifier
     */
    void deleteMessage(String messageId);

    /**
     * Declares an attachment (file name and byte size) ahead of uploading its
     * bytes. The returned {@link AttachmentRef#id()} is the handle to
     * {@link #uploadAttachment(String, byte[], String) upload} the content and
     * to reference the attachment when composing a message.
     *
     * @param declaration the file name and byte size
     * @return a handle to the declared attachment
     */
    AttachmentRef declareAttachment(AttachmentDeclaration declaration);

    /**
     * Uploads the bytes of a previously declared attachment.
     *
     * @param attachmentId the id returned by {@link #declareAttachment}
     * @param content the raw file bytes
     * @param contentType the file's MIME type; the server accepts
     *     {@code image/png}, {@code image/gif}, {@code image/bmp},
     *     {@code image/tiff}, {@code image/jpeg} and {@code application/pdf}
     * @return a handle to the uploaded attachment
     */
    AttachmentRef uploadAttachment(String attachmentId, byte[] content, String contentType);

    /**
     * Downloads the raw bytes of an attachment.
     *
     * @param attachmentId the attachment identifier
     * @return the attachment bytes
     */
    byte[] downloadAttachment(String attachmentId);
}
