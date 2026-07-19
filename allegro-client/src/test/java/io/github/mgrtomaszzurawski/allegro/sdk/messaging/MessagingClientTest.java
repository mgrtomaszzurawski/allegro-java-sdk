/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.Messaging;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.AttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.MessageFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.NewMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.builder.ReplyRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.AttachmentStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.Message;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageThread;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.messaging.model.MessageType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Full-stack WireMock coverage of the message-center facade: lazy paginated
 * streaming (threads and messages), Raw → record mapping (enums, author,
 * related object, attachments), the read/send/reply/delete writes, the binary
 * attachment up/download path, and the mandatory error-path table (400 field
 * errors, 401 replay, 404, 429, 5xx).
 */
@WireMockTest
class MessagingClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 20;

    private static final String THREADS_PATH = "/messaging/threads";
    private static final String THREAD_ID = "thread-1";
    private static final String THREAD_PATH = THREADS_PATH + "/" + THREAD_ID;
    private static final String THREAD_READ_PATH = THREAD_PATH + "/read";
    private static final String THREAD_MESSAGES_PATH = THREAD_PATH + "/messages";
    private static final String MESSAGES_PATH = "/messaging/messages";
    private static final String MESSAGE_ID = "msg-1";
    private static final String MESSAGE_PATH = MESSAGES_PATH + "/" + MESSAGE_ID;
    private static final String ATTACHMENTS_PATH = "/messaging/message-attachments";
    private static final String ATTACHMENT_ID = "att-1";
    private static final String ATTACHMENT_PATH = ATTACHMENTS_PATH + "/" + ATTACHMENT_ID;

    private static final String RECIPIENT_LOGIN = "buyer-login";
    private static final String ORDER_ID = "a8f8b6e0-1111-2222-3333-444455556666";
    private static final String OFFER_ID = "offer-9";
    private static final String MESSAGE_TEXT = "Thanks for your order.";
    private static final String SUBJECT = "Re: your order";
    private static final String VIN = "WVWZZZ1KZAW000001";
    private static final String INTERLOCUTOR_LOGIN = "buyer-login";
    private static final String AVATAR_URL = "https://a.allegroimg.com/avatar.png";
    private static final String FILE_NAME = "invoice.pdf";
    private static final String MIME_PDF = "application/pdf";
    private static final byte[] ATTACHMENT_BYTES = "%PDF-1.4 pretend-bytes".getBytes(StandardCharsets.UTF_8);

    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String OFFSET_PARAM = "offset";
    private static final String LIMIT_PARAM = "limit";
    private static final String BEFORE_PARAM = "before";
    private static final String AFTER_PARAM = "after";
    private static final OffsetDateTime FILTER_AFTER =
            OffsetDateTime.of(2026, 7, 10, 8, 30, 15, 0, ZoneOffset.UTC);
    private static final OffsetDateTime FILTER_BEFORE =
            OffsetDateTime.of(2026, 7, 18, 8, 30, 15, 0, ZoneOffset.UTC);

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String STATE_RECOVERED = "recovered";

    private static final String JSON_PATH_RECIPIENT = "$.recipient.login";
    private static final String JSON_PATH_ORDER = "$.order.id";
    private static final String JSON_PATH_TEXT = "$.text";
    private static final String JSON_PATH_READ = "$.read";
    private static final String JSON_PATH_FILENAME = "$.filename";
    private static final String JSON_PATH_SIZE = "$.size";
    private static final String JSON_PATH_ATTACHMENT_ID = "$.attachments[0].id";

    private static final int RETRY_TWICE = 2;
    private static final String RETRY_AFTER_SECONDS = "1";
    private static final long RETRY_AFTER_EXPECTED = 1L;
    private static final String FIELD_ERROR_CODE = "ValidationError";
    private static final String FIELD_ERROR_PATH = "text";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (Thread example shape from the OpenAPI
    // spec; a sandbox capture in the messaging demo confirms/corrects it)
    private static final String THREAD_RESPONSE = """
            {"id":"%s","read":true,"lastMessageDateTime":"2026-07-17T10:15:30Z",
             "interlocutor":{"login":"%s","avatarUrl":"%s"}}
            """.formatted(THREAD_ID, INTERLOCUTOR_LOGIN, AVATAR_URL);
    // spec-derived: not yet wire-verified (Message example shape)
    private static final String MESSAGE_RESPONSE = """
            {"id":"%s","status":"DELIVERED","type":"MESSAGE_CENTER",
             "createdAt":"2026-07-17T10:15:30Z","thread":{"id":"%s"},
             "author":{"login":"seller-login","isInterlocutor":false},
             "text":"%s","subject":"%s",
             "relatesTo":{"offer":{"id":"%s"},"order":{"id":"%s"}},
             "hasAdditionalAttachments":false,
             "attachments":[{"fileName":"%s","mimeType":"%s",
               "url":"https://x.allegro.pl/a.pdf","status":"SAFE"}],
             "additionalInformation":{"vin":"%s"}}
            """.formatted(MESSAGE_ID, THREAD_ID, MESSAGE_TEXT, SUBJECT, OFFER_ID, ORDER_ID,
            FILE_NAME, MIME_PDF, VIN);
    private static final String ATTACHMENT_ID_RESPONSE = "{\"id\":\"" + ATTACHMENT_ID + "\"}";
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"Text is too long",
              "userMessage":"Wiadomość jest za długa","path":"%s","details":null}]}
            """.formatted(FIELD_ERROR_CODE, FIELD_ERROR_PATH);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Message not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String BUSY_RESPONSE = "{\"errors\":[]}";
    // forward-compat (C3): wire enum values this SDK version predates must degrade to UNKNOWN,
    // not fail deserialization (Layer-1 enumUnknownDefaultCase → the mappers' `default -> UNKNOWN`).
    private static final String FUTURE_WIRE_TYPE = "FUTURE_MESSAGE_TYPE";
    private static final String FUTURE_WIRE_STATUS = "FUTURE_MESSAGE_STATUS";
    private static final String FUTURE_WIRE_ATTACHMENT_STATUS = "FUTURE_ATTACHMENT_STATUS";
    private static final String UNKNOWN_ENUMS_MESSAGE_RESPONSE = """
            {"id":"%s","status":"%s","type":"%s",
             "createdAt":"2026-07-17T10:15:30Z","thread":{"id":"%s"},
             "author":{"login":"seller-login","isInterlocutor":false},
             "text":"%s","relatesTo":{},"hasAdditionalAttachments":false,
             "attachments":[{"fileName":"%s","mimeType":"%s",
               "url":"https://x.allegro.pl/a.pdf","status":"%s"}]}
            """.formatted(MESSAGE_ID, FUTURE_WIRE_STATUS, FUTURE_WIRE_TYPE, THREAD_ID,
            MESSAGE_TEXT, FILE_NAME, MIME_PDF, FUTURE_WIRE_ATTACHMENT_STATUS);

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return client(wmInfo, RetryPolicy.defaults());
    }

    private static AllegroClient client(WireMockRuntimeInfo wmInfo, RetryPolicy retryPolicy) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(retryPolicy)
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static String fullPageOfThreads(int count) {
        StringBuilder json = new StringBuilder("{\"threads\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"t").append(index).append("\",\"read\":true}");
        }
        return json.append("],\"offset\":0,\"limit\":").append(PAGE_SIZE).append('}').toString();
    }

    private static String fullPageOfMessages(int count) {
        StringBuilder json = new StringBuilder("{\"messages\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"m").append(index)
                    .append("\",\"status\":\"DELIVERED\",\"type\":\"MESSAGE_CENTER\",")
                    .append("\"createdAt\":\"2026-07-17T10:15:30Z\",\"thread\":{\"id\":\"")
                    .append(THREAD_ID).append("\"},")
                    .append("\"author\":{\"login\":\"seller\",\"isInterlocutor\":false},")
                    .append("\"text\":\"hi\",\"relatesTo\":{},")
                    .append("\"hasAdditionalAttachments\":false,\"attachments\":[]}");
        }
        return json.append("],\"offset\":0,\"limit\":").append(PAGE_SIZE).append('}').toString();
    }

    // ---- streaming (laziness + mapping) ----

    @Test
    void streamThreads_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page; the second page must not be requested
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREADS_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfThreads(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — consume only the first thread
            List<MessageThread> firstOnly = allegro.messaging().streamThreads().limit(1).toList();

            // then — page one fetched, page two (offset=20) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(THREADS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(THREADS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamThreads_whenTraversed_terminatesOnShortPageAndMapsInterlocutor(
            WireMockRuntimeInfo wmInfo) {
        // given — page one full (20), page two short (1) => stops after page two
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREADS_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"threads\":[" + THREAD_RESPONSE + "],\"offset\":0,\"limit\":20}")));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — a single short page terminates the walk
            List<MessageThread> threads = allegro.messaging().streamThreads().toList();

            // then — mapped, and page two (offset=20) never requested
            assertEquals(1, threads.size());
            MessageThread thread = threads.get(0);
            assertEquals(THREAD_ID, thread.id());
            assertEquals(INTERLOCUTOR_LOGIN, thread.interlocutor().login());
            assertEquals(AVATAR_URL, thread.interlocutor().avatarUrl());
            verify(0, getRequestedFor(urlPathEqualTo(THREADS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamMessages_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page of messages
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREAD_MESSAGES_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfMessages(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — consume one message
            List<Message> firstOnly = allegro.messaging().streamMessages(THREAD_ID).limit(1).toList();

            // then — page two never requested
            assertEquals(1, firstOnly.size());
            verify(0, getRequestedFor(urlPathEqualTo(THREAD_MESSAGES_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamMessages_whenFilterGiven_sendsBeforeAndAfterQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREAD_MESSAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfMessages(1))));
        MessageFilter filter = MessageFilter.builder().after(FILTER_AFTER).before(FILTER_BEFORE).build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.messaging().streamMessages(THREAD_ID, filter).toList();

            // then — the window bounds propagate as query parameters
            verify(getRequestedFor(urlPathEqualTo(THREAD_MESSAGES_PATH))
                    .withQueryParam(BEFORE_PARAM, equalTo(FILTER_BEFORE.toString()))
                    .withQueryParam(AFTER_PARAM, equalTo(FILTER_AFTER.toString()))
                    .withQueryParam(LIMIT_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamMessages_whenTraversedWithFilter_filterSurvivesToPageTwo(WireMockRuntimeInfo wmInfo) {
        // given — page one full (20) at offset 0, page two short (1) at offset 20; BOTH
        // stubs require the before/after params, so a page-2 request that dropped the
        // filter would miss its stub (404) and fail the traversal
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREAD_MESSAGES_PATH))
                .withQueryParam(OFFSET_PARAM, equalTo("0"))
                .withQueryParam(BEFORE_PARAM, equalTo(FILTER_BEFORE.toString()))
                .withQueryParam(AFTER_PARAM, equalTo(FILTER_AFTER.toString()))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfMessages(PAGE_SIZE))));
        stubFor(get(urlPathEqualTo(THREAD_MESSAGES_PATH))
                .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE)))
                .withQueryParam(BEFORE_PARAM, equalTo(FILTER_BEFORE.toString()))
                .withQueryParam(AFTER_PARAM, equalTo(FILTER_AFTER.toString()))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfMessages(1))));
        MessageFilter filter = MessageFilter.builder().after(FILTER_AFTER).before(FILTER_BEFORE).build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when — traverse fully, forcing the page-2 fetch
            long total = allegro.messaging().streamMessages(THREAD_ID, filter).count();

            // then — page two was fetched WITH the same filter still applied
            assertEquals(PAGE_SIZE + 1L, total);
            verify(1, getRequestedFor(urlPathEqualTo(THREAD_MESSAGES_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE)))
                    .withQueryParam(BEFORE_PARAM, equalTo(FILTER_BEFORE.toString()))
                    .withQueryParam(AFTER_PARAM, equalTo(FILTER_AFTER.toString())));
        }
    }

    // ---- reads ----

    @Test
    void thread_whenExists_sendsVendorAcceptAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(THREAD_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(THREAD_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            MessageThread thread = allegro.messaging().thread(THREAD_ID);

            // then
            assertEquals(THREAD_ID, thread.id());
            verify(1, getRequestedFor(urlEqualTo(THREAD_PATH)));
        }
    }

    @Test
    void message_whenExists_mapsEnumsAuthorRelatedObjectAndAttachments(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MESSAGE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(MESSAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Message message = allegro.messaging().message(MESSAGE_ID);

            // then — every mapped facet survives
            assertEquals(MessageStatus.DELIVERED, message.status());
            assertEquals(MessageType.MESSAGE_CENTER, message.type());
            assertEquals(THREAD_ID, message.threadId());
            assertEquals("seller-login", message.author().login());
            assertFalse(message.author().interlocutor());
            assertEquals(OFFER_ID, message.relatesTo().offerId());
            assertEquals(ORDER_ID, message.relatesTo().orderId());
            assertEquals(1, message.attachments().size());
            assertEquals(AttachmentStatus.SAFE, message.attachments().get(0).status());
            assertEquals(FILE_NAME, message.attachments().get(0).fileName());
            assertEquals(VIN, message.vehicleVin());
        }
    }

    // ---- forward compatibility (C3): unknown wire enum values degrade to UNKNOWN ----

    @Test
    void message_whenWireEnumsUnknown_degradeTypeStatusAndAttachmentStatusToUnknown(
            WireMockRuntimeInfo wmInfo) {
        // given — the server introduces message type/status and attachment status values
        // this SDK version predates
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MESSAGE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_ENUMS_MESSAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — deserialization must not throw on the unrecognized enum values
            Message message = allegro.messaging().message(MESSAGE_ID);

            // then — every unknown wire value degrades to UNKNOWN end-to-end
            assertEquals(MessageType.UNKNOWN, message.type());
            assertEquals(MessageStatus.UNKNOWN, message.status());
            assertEquals(1, message.attachments().size());
            assertEquals(AttachmentStatus.UNKNOWN, message.attachments().get(0).status());
        }
    }

    // ---- writes ----

    @Test
    void markRead_whenCalled_putsReadTrueBodyAndMapsThread(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(THREAD_READ_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_READ, equalTo("true")))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(THREAD_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            MessageThread thread = allegro.messaging().markRead(THREAD_ID);

            // then — read flag posted, thread mapped back
            assertEquals(THREAD_ID, thread.id());
            verify(1, putRequestedFor(urlEqualTo(THREAD_READ_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PATH_READ, equalTo("true"))));
        }
    }

    @Test
    void send_whenValid_sendsRecipientOrderTextBodyAndMapsMessage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGES_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_RECIPIENT, equalTo(RECIPIENT_LOGIN)))
                .withRequestBody(matchingJsonPath(JSON_PATH_ORDER, equalTo(ORDER_ID)))
                .withRequestBody(matchingJsonPath(JSON_PATH_TEXT, equalTo(MESSAGE_TEXT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(MESSAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Message sent = allegro.messaging().send(NewMessageRequest.builder()
                    .recipientLogin(RECIPIENT_LOGIN)
                    .orderId(ORDER_ID)
                    .text(MESSAGE_TEXT)
                    .build());

            // then
            assertEquals(MESSAGE_ID, sent.id());
            verify(1, postRequestedFor(urlEqualTo(MESSAGES_PATH)));
        }
    }

    @Test
    void send_whenAttachmentAttached_includesAttachmentIdInBody(WireMockRuntimeInfo wmInfo) {
        // given — the stub only matches when the declared attachment id is in the body
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGES_PATH))
                .withRequestBody(matchingJsonPath(JSON_PATH_ATTACHMENT_ID, equalTo(ATTACHMENT_ID)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(MESSAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.messaging().send(NewMessageRequest.builder()
                    .recipientLogin(RECIPIENT_LOGIN).orderId(ORDER_ID).text(MESSAGE_TEXT)
                    .attachment(ATTACHMENT_ID).build());

            // then — the declared attachment id is serialized into the request body
            verify(1, postRequestedFor(urlEqualTo(MESSAGES_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PATH_ATTACHMENT_ID, equalTo(ATTACHMENT_ID))));
        }
    }

    @Test
    void reply_whenValid_postsTextToThreadMessagesAndMapsMessage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(THREAD_MESSAGES_PATH))
                .withRequestBody(matchingJsonPath(JSON_PATH_TEXT, equalTo(MESSAGE_TEXT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(MESSAGE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Message sent = allegro.messaging().reply(THREAD_ID,
                    ReplyRequest.builder().text(MESSAGE_TEXT).build());

            // then
            assertEquals(MESSAGE_ID, sent.id());
            verify(1, postRequestedFor(urlEqualTo(THREAD_MESSAGES_PATH)));
        }
    }

    @Test
    void deleteMessage_whenCalled_sendsDeleteToMessagePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(MESSAGE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.messaging().deleteMessage(MESSAGE_ID);

            // then — the delete reached the wire exactly once
            verify(1, deleteRequestedFor(urlEqualTo(MESSAGE_PATH)));
        }
    }

    // ---- attachments (binary) ----

    @Test
    void declareAttachment_whenValid_postsFilenameAndSizeAndMapsRef(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(ATTACHMENTS_PATH))
                .withRequestBody(matchingJsonPath(JSON_PATH_FILENAME, equalTo(FILE_NAME)))
                .withRequestBody(matchingJsonPath(JSON_PATH_SIZE,
                        equalTo(String.valueOf(ATTACHMENT_BYTES.length))))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(ATTACHMENT_ID_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AttachmentRef declaredRef = allegro.messaging().declareAttachment(
                    AttachmentDeclaration.builder()
                            .filename(FILE_NAME)
                            .size(ATTACHMENT_BYTES.length)
                            .build());

            // then
            assertEquals(ATTACHMENT_ID, declaredRef.id());
            verify(1, postRequestedFor(urlEqualTo(ATTACHMENTS_PATH)));
        }
    }

    @Test
    void uploadAttachment_whenCalled_putsRawBytesWithContentTypeAndMapsRef(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(ATTACHMENT_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(MIME_PDF))
                .withRequestBody(binaryEqualTo(ATTACHMENT_BYTES))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ATTACHMENT_ID_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AttachmentRef uploadedRef = allegro.messaging()
                    .uploadAttachment(ATTACHMENT_ID, ATTACHMENT_BYTES, MIME_PDF);

            // then — the exact bytes and content type reached the wire
            assertEquals(ATTACHMENT_ID, uploadedRef.id());
            verify(1, putRequestedFor(urlEqualTo(ATTACHMENT_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(MIME_PDF))
                    .withRequestBody(binaryEqualTo(ATTACHMENT_BYTES)));
        }
    }

    @Test
    void downloadAttachment_whenCalled_requestsAnyMediaTypeAndReturnsRawBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ATTACHMENT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ATTACHMENT_BYTES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            byte[] downloaded = allegro.messaging().downloadAttachment(ATTACHMENT_ID);

            // then — bytes survive verbatim (no JSON coercion), and Accept was */*
            assertArrayEquals(ATTACHMENT_BYTES, downloaded);
            verify(1, getRequestedFor(urlEqualTo(ATTACHMENT_PATH))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo("*/*")));
        }
    }

    // ---- error-path table (per facade) ----

    @Test
    void send_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Messaging messaging = allegro.messaging();
            NewMessageRequest request = NewMessageRequest.builder()
                    .recipientLogin(RECIPIENT_LOGIN).orderId(ORDER_ID).text(MESSAGE_TEXT).build();

            // then — the typed field errors survive parsing; a POST is not retried
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> messaging.send(request));
            assertEquals(1, failure.errors().size());
            assertEquals(FIELD_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(FIELD_ERROR_PATH, failure.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(MESSAGES_PATH)));
        }
    }

    @Test
    void thread_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(THREAD_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(THREAD_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(THREAD_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            MessageThread thread = allegro.messaging().thread(THREAD_ID);

            // then — replayed once, the second request carried the FRESH token
            assertEquals(THREAD_ID, thread.id());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(THREAD_PATH)));
            verify(1, getRequestedFor(urlEqualTo(THREAD_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void message_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(MESSAGE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Messaging messaging = allegro.messaging();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> messaging.message(MESSAGE_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void streamThreads_when429Persists_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — always throttled; a two-attempt policy exhausts and surfaces the 429
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(THREADS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS)
                        .withBody(BUSY_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Messaging messaging = allegro.messaging();

            // then — retried to exhaustion, Retry-After surfaced to the caller
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> messaging.streamThreads().toList());
            assertEquals(RETRY_AFTER_EXPECTED, failure.retryAfterSeconds());
            verify(RETRY_TWICE, getRequestedFor(urlPathEqualTo(THREADS_PATH)));
        }
    }

    @Test
    void thread_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — a GET is idempotent, so a transient 500 is retried
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(THREAD_PATH)).inScenario(SCENARIO_RECOVER)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(THREAD_PATH)).inScenario(SCENARIO_RECOVER)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(THREAD_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            // when
            MessageThread thread = allegro.messaging().thread(THREAD_ID);

            // then
            assertEquals(THREAD_ID, thread.id());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(THREAD_PATH)));
        }
    }

    @Test
    void send_when5xx_isNotRetried(WireMockRuntimeInfo wmInfo) {
        // given — writes are not idempotent; retryPost defaults to false
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Messaging messaging = allegro.messaging();
            NewMessageRequest request = NewMessageRequest.builder()
                    .recipientLogin(RECIPIENT_LOGIN).orderId(ORDER_ID).text(MESSAGE_TEXT).build();

            // then — exactly one attempt, no retry on a POST
            assertThrows(AllegroServerException.class, () -> messaging.send(request));
            verify(1, postRequestedFor(urlEqualTo(MESSAGES_PATH)));
        }
    }
}
