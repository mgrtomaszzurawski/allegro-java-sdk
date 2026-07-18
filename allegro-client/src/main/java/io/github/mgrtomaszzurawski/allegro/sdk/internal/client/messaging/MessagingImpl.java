/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.messaging;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageAttachmentIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageOrderRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessagesListRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NewAttachmentDeclarationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NewMessageInThreadRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.NewMessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.RecipientRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThreadRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThreadReadFlagRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ThreadsListRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.Messaging;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.MessageFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.NewMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.ReplyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.Message;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageThread;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link Messaging} facade — the eleven
 * {@code /messaging} operations.
 *
 * <p>The thread and message list responses carry {@code offset}/{@code limit}
 * but no {@code totalCount}, so pagination terminates when a page comes back
 * shorter than the requested page size (a full page implies there may be more).
 * Both endpoints cap {@code limit} at {@value #PAGE_SIZE}.
 *
 * @since 0.2.0
 */
public final class MessagingImpl implements Messaging {

    /** Server cap on {@code limit} for both threads and messages. */
    private static final int PAGE_SIZE = 20;

    private static final String ACCEPT_ANY = "*/*";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";
    private static final String QUERY_BEFORE = "before";
    private static final String QUERY_AFTER = "after";

    private static final String OP_STREAM_THREADS = "stream message threads";
    private static final String OP_GET_THREAD = "get message thread";
    private static final String OP_MARK_READ = "mark thread read";
    private static final String OP_STREAM_MESSAGES = "stream thread messages";
    private static final String OP_SEND = "send message";
    private static final String OP_REPLY = "reply in thread";
    private static final String OP_GET_MESSAGE = "get message";
    private static final String OP_DELETE_MESSAGE = "delete message";
    private static final String OP_DECLARE_ATTACHMENT = "declare message attachment";
    private static final String OP_UPLOAD_ATTACHMENT = "upload message attachment";
    private static final String OP_DOWNLOAD_ATTACHMENT = "download message attachment";

    private final HttpSupport http;

    public MessagingImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<MessageThread> streamThreads() {
        return PagedSpliterator.stream(this::fetchThreadsPage);
    }

    private PagedSpliterator.Page<MessageThread> fetchThreadsPage(int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        ThreadsListRaw response = http.request(OP_STREAM_THREADS)
                .get(ApiPaths.MESSAGING_THREADS)
                .query(query)
                .fetch(ThreadsListRaw.class);
        List<ThreadRaw> threads = response.getThreads();
        List<MessageThread> items = threads == null
                ? List.of()
                : threads.stream().map(MessageThread::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public MessageThread thread(String threadId) {
        return MessageThread.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.MESSAGING_THREADS, threadId), ThreadRaw.class, OP_GET_THREAD));
    }

    @Override
    public MessageThread markRead(String threadId) {
        ThreadReadFlagRaw body = new ThreadReadFlagRaw().read(Boolean.TRUE);
        return MessageThread.from(http.putJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.MESSAGING_THREADS, threadId, ApiPaths.READ_SEGMENT),
                body, ThreadRaw.class, OP_MARK_READ));
    }

    @Override
    public Stream<Message> streamMessages(String threadId) {
        return streamMessages(threadId, MessageFilter.none());
    }

    @Override
    public Stream<Message> streamMessages(String threadId, MessageFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchMessagesPage(threadId, filter, pageIndex));
    }

    private PagedSpliterator.Page<Message> fetchMessagesPage(String threadId, MessageFilter filter,
            int pageIndex) {
        Query query = Query.create()
                .add(QUERY_BEFORE, filter.before())
                .add(QUERY_AFTER, filter.after())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        MessagesListRaw response = http.request(OP_STREAM_MESSAGES)
                .get(ApiPaths.subPath(ApiPaths.MESSAGING_THREADS, threadId, ApiPaths.MESSAGES_SEGMENT))
                .query(query)
                .fetch(MessagesListRaw.class);
        List<MessageRaw> messages = response.getMessages();
        List<Message> items = messages == null
                ? List.of()
                : messages.stream().map(Message::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public Message send(NewMessageRequest request) {
        NewMessageRaw body = new NewMessageRaw()
                .recipient(new RecipientRaw().login(request.recipientLogin()))
                .order(new MessageOrderRaw().id(request.orderId()))
                .text(request.text());
        for (String attachmentId : request.attachmentIds()) {
            body.addAttachmentsItem(new MessageAttachmentIdRaw().id(attachmentId));
        }
        return Message.from(http.postJsonAuthenticated(
                ApiPaths.MESSAGING_MESSAGES, body, MessageRaw.class, OP_SEND));
    }

    @Override
    public Message reply(String threadId, ReplyRequest request) {
        NewMessageInThreadRaw body = new NewMessageInThreadRaw().text(request.text());
        for (String attachmentId : request.attachmentIds()) {
            body.addAttachmentsItem(new MessageAttachmentIdRaw().id(attachmentId));
        }
        return Message.from(http.postJsonAuthenticated(
                ApiPaths.subPath(ApiPaths.MESSAGING_THREADS, threadId, ApiPaths.MESSAGES_SEGMENT),
                body, MessageRaw.class, OP_REPLY));
    }

    @Override
    public Message message(String messageId) {
        return Message.from(http.getAuthenticated(
                ApiPaths.subPath(ApiPaths.MESSAGING_MESSAGES, messageId), MessageRaw.class,
                OP_GET_MESSAGE));
    }

    @Override
    public void deleteMessage(String messageId) {
        http.deleteAuthenticated(
                ApiPaths.subPath(ApiPaths.MESSAGING_MESSAGES, messageId), OP_DELETE_MESSAGE);
    }

    @Override
    public AttachmentRef declareAttachment(AttachmentDeclaration declaration) {
        NewAttachmentDeclarationRaw body = new NewAttachmentDeclarationRaw()
                .filename(declaration.filename())
                .size(declaration.size());
        return AttachmentRef.from(http.postJsonAuthenticated(
                ApiPaths.MESSAGING_MESSAGE_ATTACHMENTS, body, MessageAttachmentIdRaw.class,
                OP_DECLARE_ATTACHMENT));
    }

    @Override
    public AttachmentRef uploadAttachment(String attachmentId, byte[] content, String contentType) {
        MessageAttachmentIdRaw uploaded = http.request(OP_UPLOAD_ATTACHMENT)
                .put(ApiPaths.subPath(ApiPaths.MESSAGING_MESSAGE_ATTACHMENTS, attachmentId))
                .binaryBody(content, contentType)
                .fetch(MessageAttachmentIdRaw.class);
        return AttachmentRef.from(uploaded);
    }

    @Override
    public byte[] downloadAttachment(String attachmentId) {
        return http.request(OP_DOWNLOAD_ATTACHMENT)
                .get(ApiPaths.subPath(ApiPaths.MESSAGING_MESSAGE_ATTACHMENTS, attachmentId))
                .accept(ACCEPT_ANY)
                .fetchBytes();
    }
}
