/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.internal.client.disputes;

import io.github.mgrtomaszzurawski.allegro.client.model.AttachmentDeclarationRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.ClaimStatusChangeRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.MessageRequestRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueAttachmentIdRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueChatMessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueChatResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueListResponseRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueMessageRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PostPurchaseIssueRaw;
import io.github.mgrtomaszzurawski.allegro.client.model.PriceRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.Disputes;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.ClaimStatusChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueAttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpSupport;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Located;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.Query;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint wrapper behind the {@link Disputes} facade — the read side of the
 * {@code /sale/issues} (post-purchase issues) resource.
 *
 * <p>Every call requests the beta vendor media type ({@code acceptBeta()}). The list and
 * chat responses carry {@code offset}/{@code limit} but no {@code totalCount}, so pagination
 * terminates on a page shorter than the requested size; both endpoints cap {@code limit} at
 * {@value #PAGE_SIZE}.
 *
 * @since 0.2.0
 */
public final class DisputesImpl implements Disputes {

    /** Server cap on {@code limit} for both the issues list and the chat. */
    private static final int PAGE_SIZE = 100;

    private static final String QUERY_STATUS = "status";
    private static final String QUERY_CHECKOUT_FORM_ID = "checkoutForm.id";
    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_LIMIT = "limit";

    private static final String OP_STREAM_ISSUES = "stream post-purchase issues";
    private static final String OP_GET_ISSUE = "get post-purchase issue";
    private static final String OP_STREAM_CHAT = "stream issue chat";
    private static final String OP_ADD_MESSAGE = "add issue message";
    private static final String OP_CHANGE_STATUS = "change claim status";
    private static final String OP_DECLARE_ATTACHMENT = "declare issue attachment";
    private static final String OP_UPLOAD_ATTACHMENT = "upload issue attachment";
    private static final String OP_DOWNLOAD_ATTACHMENT = "download issue attachment";

    private static final String ACCEPT_ANY = "*/*";
    private static final String ERR_NO_UPLOAD_LOCATION =
            "attachment declaration returned no upload location";

    private final HttpSupport http;

    public DisputesImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    @Override
    public Stream<Issue> streamIssues(IssueFilter filter) {
        return PagedSpliterator.stream(pageIndex -> fetchIssuesPage(filter, pageIndex));
    }

    private PagedSpliterator.Page<Issue> fetchIssuesPage(IssueFilter filter, int pageIndex) {
        Query query = Query.create();
        for (IssueStatus status : filter.statuses()) {
            // UNKNOWN is a response-only fallback, never a valid server filter value.
            if (status != IssueStatus.UNKNOWN) {
                query.add(QUERY_STATUS, status.name());
            }
        }
        query.add(QUERY_CHECKOUT_FORM_ID, filter.checkoutFormId())
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        PostPurchaseIssueListResponseRaw response = http.request(OP_STREAM_ISSUES)
                .get(ApiPaths.ISSUES)
                .acceptBeta()
                .query(query)
                .fetch(PostPurchaseIssueListResponseRaw.class);
        List<PostPurchaseIssueRaw> issues = response.getIssues();
        List<Issue> items = issues == null
                ? List.of()
                : issues.stream().map(Issue::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public Issue get(String issueId) {
        return Issue.from(http.request(OP_GET_ISSUE)
                .get(ApiPaths.subPath(ApiPaths.ISSUES, issueId))
                .acceptBeta()
                .fetch(PostPurchaseIssueRaw.class));
    }

    @Override
    public Stream<IssueChatEntry> streamChat(String issueId) {
        return PagedSpliterator.stream(pageIndex -> fetchChatPage(issueId, pageIndex));
    }

    private PagedSpliterator.Page<IssueChatEntry> fetchChatPage(String issueId, int pageIndex) {
        Query query = Query.create()
                .add(QUERY_OFFSET, pageIndex * PAGE_SIZE)
                .add(QUERY_LIMIT, PAGE_SIZE);
        PostPurchaseIssueChatResponseRaw response = http.request(OP_STREAM_CHAT)
                .get(ApiPaths.subPath(ApiPaths.ISSUES, issueId, ApiPaths.CHAT_SEGMENT))
                .acceptBeta()
                .query(query)
                .fetch(PostPurchaseIssueChatResponseRaw.class);
        List<PostPurchaseIssueChatMessageRaw> chat = response.getChat();
        List<IssueChatEntry> items = chat == null
                ? List.of()
                : chat.stream().map(IssueChatEntry::from).toList();
        return new PagedSpliterator.Page<>(items, items.size() == PAGE_SIZE);
    }

    @Override
    public IssueChatEntry addMessage(String issueId, IssueMessageRequest request) {
        MessageRequestRaw body = new MessageRequestRaw()
                .type(MessageRequestRaw.TypeEnum.fromValue(request.type().name()));
        if (request.text() != null) {
            body.text(request.text());
        }
        for (String attachmentId : request.attachmentIds()) {
            body.addAttachmentsItem(new PostPurchaseIssueAttachmentIdRaw().id(attachmentId));
        }
        // Partial serialization: drop the empty attachments array when there is none,
        // so it is absent rather than sent as [].
        PostPurchaseIssueMessageRaw created = http.request(OP_ADD_MESSAGE)
                .post(ApiPaths.subPath(ApiPaths.ISSUES, issueId, ApiPaths.MESSAGE_SEGMENT))
                .acceptBeta()
                .betaJsonBodyPartial(body)
                .fetch(PostPurchaseIssueMessageRaw.class);
        return IssueChatEntry.from(created);
    }

    @Override
    public void changeStatus(String issueId, ClaimStatusChange change) {
        ClaimStatusChangeRequestRaw body = new ClaimStatusChangeRequestRaw()
                .status(ClaimStatusChangeRequestRaw.StatusEnum.fromValue(change.status().name()))
                .message(change.message());
        Money refund = change.partialRefund();
        if (refund != null) {
            body.partialRefund(new PriceRaw().amount(refund.amount()).currency(refund.currency()));
        }
        // Partial serialization: omit partialRefund when the status carries no refund.
        http.request(OP_CHANGE_STATUS)
                .post(ApiPaths.subPath(ApiPaths.ISSUES, issueId, ApiPaths.STATUS_SEGMENT))
                .acceptBeta()
                .betaJsonBodyPartial(body)
                .send();
    }

    @Override
    public IssueAttachmentRef uploadAttachment(IssueAttachmentDeclaration declaration,
            byte[] content, String contentType) {
        AttachmentDeclarationRaw declarationBody = new AttachmentDeclarationRaw()
                .fileName(declaration.filename())
                .size(declaration.size());
        // Declare the attachment; Allegro returns a one-time upload URL in Location.
        Located<PostPurchaseIssueAttachmentIdRaw> declared = http.request(OP_DECLARE_ATTACHMENT)
                .post(ApiPaths.ISSUES_ATTACHMENTS)
                .acceptBeta()
                .betaJsonBody(declarationBody)
                .fetchLocation(PostPurchaseIssueAttachmentIdRaw.class);
        String uploadUrl = declared.location();
        if (uploadUrl == null) {
            throw new IllegalStateException(ERR_NO_UPLOAD_LOCATION);
        }
        // PUT the bytes to that absolute (possibly cross-host) upload URL.
        PostPurchaseIssueAttachmentIdRaw uploaded = http.request(OP_UPLOAD_ATTACHMENT)
                .putAbsolute(uploadUrl)
                .acceptBeta()
                .binaryBody(content, contentType)
                .fetch(PostPurchaseIssueAttachmentIdRaw.class);
        return IssueAttachmentRef.from(uploaded);
    }

    @Override
    public byte[] downloadAttachment(String attachmentId) {
        return http.request(OP_DOWNLOAD_ATTACHMENT)
                .get(ApiPaths.subPath(ApiPaths.ISSUES_ATTACHMENTS, attachmentId))
                .accept(ACCEPT_ANY)
                .fetchBytes();
    }
}
