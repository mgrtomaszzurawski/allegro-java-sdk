/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.disputes;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.Disputes;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.ClaimStatusChange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueAttachmentDeclaration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueMessageRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ChatAuthorRole;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ClaimStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueAttachmentRef;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueMessageType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Full-stack WireMock coverage of the disputes (post-purchase issues) write facade: the beta
 * vendor <em>request-body</em> content type (not just Accept) on every write, request-body
 * shape (message text/type/attachment, claim status/message/partial-refund), the
 * declare-then-upload attachment flow that PUTs to the one-time {@code Location} URL, binary
 * download, and the write error-path table (400 field errors, 401 replay, POST not retried
 * on 5xx).
 */
@WireMockTest
class DisputesWritesTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String ISSUE_ID = "issue-1";
    private static final String MESSAGE_PATH = "/sale/issues/" + ISSUE_ID + "/message";
    private static final String STATUS_PATH = "/sale/issues/" + ISSUE_ID + "/status";
    private static final String ATTACHMENTS_PATH = "/sale/issues/attachments";
    private static final String ATTACHMENT_ID = "att-1";
    private static final String ATTACHMENT_DOWNLOAD_PATH = ATTACHMENTS_PATH + "/" + ATTACHMENT_ID;
    private static final String UPLOAD_SUBPATH = "/dispute-uploads/" + ATTACHMENT_ID;

    private static final String CREATED_MESSAGE_ID = "msg-1";
    private static final String MESSAGE_TEXT = "Thank you, a replacement ships today.";
    private static final String CHANGE_MESSAGE = "We accept a partial refund.";
    private static final String REFUND_AMOUNT = "12.50";
    private static final String CURRENCY_PLN = "PLN";
    private static final String FILE_NAME = "evidence.jpg";
    private static final String MIME_JPEG = "image/jpeg";
    private static final byte[] ATTACHMENT_BYTES = "binary-content".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DOWNLOAD_BYTES = "downloaded-file".getBytes(StandardCharsets.UTF_8);

    // Wire enum values (must equal the SDK enum constant names verbatim).
    private static final String TYPE_REGULAR = "REGULAR";
    private static final String TYPE_RETURN_NOT_REQUIRED = "RETURN_NOT_REQUIRED";
    private static final String STATUS_PARTIAL_REFUND = "ACCEPTED_PARTIAL_REFUND";
    private static final String STATUS_MINOR_DEFECT = "REJECTED_MINOR_DEFECT";
    private static final String ERROR_PATH_TEXT = "text";

    private static final String JSON_PATH_TEXT = "$.text";
    private static final String JSON_PATH_TYPE = "$.type";
    private static final String JSON_PATH_ATTACHMENT_ID = "$.attachments[0].id";
    private static final String JSON_PATH_STATUS = "$.status";
    private static final String JSON_PATH_MESSAGE = "$.message";
    private static final String JSON_PATH_REFUND_AMOUNT = "$.partialRefund.amount";
    private static final String JSON_PATH_FILENAME = "$.fileName";
    private static final String JSON_PATH_SIZE = "$.size";

    private static final String LOCATION_HEADER = "Location";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final int RETRY_TWICE = 2;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String MESSAGE_RESPONSE = """
            {"id":"%s","text":"%s","createdAt":"2026-07-16T09:00:00Z",
             "author":{"login":"seller-login","role":"SELLER"},"attachments":[]}
            """.formatted(CREATED_MESSAGE_ID, MESSAGE_TEXT);
    private static final String ATTACHMENT_ID_RESPONSE = "{\"id\":\"%s\"}".formatted(ATTACHMENT_ID);
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationError","message":"Message not allowed",
              "userMessage":"Nie mozna dodac wiadomosci","path":"%s","details":null}]}
            """.formatted(ERROR_PATH_TEXT);
    private static final String SERVER_ERROR_RESPONSE = "{\"errors\":[]}";

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

    // ---- addMessage ----

    @Test
    void addMessage_whenTextGiven_postsBetaBodyAndMapsCreatedEntry(WireMockRuntimeInfo wmInfo) {
        // given — the stub requires the BETA request-body content type
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_TEXT, equalTo(MESSAGE_TEXT)))
                .withRequestBody(matchingJsonPath(JSON_PATH_TYPE, equalTo(TYPE_REGULAR)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(MESSAGE_RESPONSE)));
        IssueMessageRequest request = IssueMessageRequest.builder().text(MESSAGE_TEXT).build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            IssueChatEntry created = allegro.disputes().addMessage(ISSUE_ID, request);

            // then
            assertEquals(CREATED_MESSAGE_ID, created.id());
            assertEquals(MESSAGE_TEXT, created.text());
            assertEquals(ChatAuthorRole.SELLER, created.author().role());
            verify(1, postRequestedFor(urlEqualTo(MESSAGE_PATH)));
        }
    }

    @Test
    void addMessage_whenAttachmentReferenced_putsAttachmentIdInBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGE_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_ATTACHMENT_ID, equalTo(ATTACHMENT_ID)))
                .withRequestBody(matchingJsonPath(JSON_PATH_TYPE, equalTo(TYPE_RETURN_NOT_REQUIRED)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(MESSAGE_RESPONSE)));
        IssueMessageRequest request = IssueMessageRequest.builder()
                .text(MESSAGE_TEXT)
                .type(IssueMessageType.RETURN_NOT_REQUIRED)
                .attachment(ATTACHMENT_ID)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.disputes().addMessage(ISSUE_ID, request);

            // then
            verify(1, postRequestedFor(urlEqualTo(MESSAGE_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PATH_ATTACHMENT_ID, equalTo(ATTACHMENT_ID))));
        }
    }

    // ---- changeStatus ----

    @Test
    void changeStatus_whenPartialRefund_postsStatusMessageAndAmount(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(STATUS_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_STATUS, equalTo(STATUS_PARTIAL_REFUND)))
                .withRequestBody(matchingJsonPath(JSON_PATH_MESSAGE, equalTo(CHANGE_MESSAGE)))
                .withRequestBody(matchingJsonPath(JSON_PATH_REFUND_AMOUNT, equalTo(REFUND_AMOUNT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        ClaimStatusChange change = ClaimStatusChange.builder()
                .status(ClaimStatus.ACCEPTED_PARTIAL_REFUND)
                .message(CHANGE_MESSAGE)
                .partialRefund(Money.of(REFUND_AMOUNT, CURRENCY_PLN))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.disputes().changeStatus(ISSUE_ID, change);

            // then
            verify(1, postRequestedFor(urlEqualTo(STATUS_PATH)));
        }
    }

    @Test
    void changeStatus_whenNoRefund_omitsPartialRefundField(WireMockRuntimeInfo wmInfo) {
        // given — the exact body must NOT carry partialRefund (strict, no extra elements)
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        ClaimStatusChange change = ClaimStatusChange.builder()
                .status(ClaimStatus.REJECTED_MINOR_DEFECT)
                .message(CHANGE_MESSAGE)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.disputes().changeStatus(ISSUE_ID, change);

            // then — body is exactly {status, message}; partialRefund absent
            verify(1, postRequestedFor(urlEqualTo(STATUS_PATH)).withRequestBody(equalToJson(
                    "{\"status\":\"" + STATUS_MINOR_DEFECT + "\",\"message\":\"" + CHANGE_MESSAGE + "\"}",
                    true, false)));
        }
    }

    // ---- uploadAttachment (declare + PUT to the Location URL) ----

    @Test
    void uploadAttachment_whenDeclared_putsBytesToLocationUrlAndMapsRef(WireMockRuntimeInfo wmInfo) {
        // given — declare returns a one-time absolute upload URL in Location
        stubToken(TEST_TOKEN);
        String uploadUrl = wmInfo.getHttpBaseUrl() + UPLOAD_SUBPATH;
        stubFor(post(urlEqualTo(ATTACHMENTS_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .withRequestBody(matchingJsonPath(JSON_PATH_FILENAME, equalTo(FILE_NAME)))
                .withRequestBody(matchingJsonPath(JSON_PATH_SIZE,
                        equalTo(String.valueOf(ATTACHMENT_BYTES.length))))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withHeader(LOCATION_HEADER, uploadUrl)
                        .withBody(ATTACHMENT_ID_RESPONSE)));
        stubFor(put(urlEqualTo(UPLOAD_SUBPATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(MIME_JPEG))
                .withRequestBody(binaryEqualTo(ATTACHMENT_BYTES))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(ATTACHMENT_ID_RESPONSE)));
        IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
                .filename(FILE_NAME)
                .size(ATTACHMENT_BYTES.length)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            IssueAttachmentRef uploadedRef = allegro.disputes()
                    .uploadAttachment(declaration, ATTACHMENT_BYTES, MIME_JPEG);

            // then — declared once, then PUT to the Location URL verbatim
            assertEquals(ATTACHMENT_ID, uploadedRef.id());
            verify(1, postRequestedFor(urlEqualTo(ATTACHMENTS_PATH)));
            verify(1, putRequestedFor(urlEqualTo(UPLOAD_SUBPATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(MIME_JPEG))
                    .withRequestBody(binaryEqualTo(ATTACHMENT_BYTES)));
        }
    }

    @Test
    void uploadAttachment_whenDeclarationHasNoLocation_throwsAndDoesNotUpload(WireMockRuntimeInfo wmInfo) {
        // given — a 201 without a Location header cannot be uploaded to
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(ATTACHMENTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(ATTACHMENT_ID_RESPONSE)));
        IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
                .filename(FILE_NAME)
                .size(ATTACHMENT_BYTES.length)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            Disputes disputes = allegro.disputes();

            // then
            assertThrows(IllegalStateException.class,
                    () -> disputes.uploadAttachment(declaration, ATTACHMENT_BYTES, MIME_JPEG));
            verify(0, putRequestedFor(urlEqualTo(UPLOAD_SUBPATH)));
        }
    }

    @Test
    void uploadAttachment_whenContentSizeMismatch_throwsBeforeDeclaring(WireMockRuntimeInfo wmInfo) {
        // given — the byte count must equal the declared size; a mismatch is a client error
        stubToken(TEST_TOKEN);
        IssueAttachmentDeclaration declaration = IssueAttachmentDeclaration.builder()
                .filename(FILE_NAME)
                .size(ATTACHMENT_BYTES.length + 1)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            Disputes disputes = allegro.disputes();

            // then — fails fast, no declare request is sent
            assertThrows(IllegalArgumentException.class,
                    () -> disputes.uploadAttachment(declaration, ATTACHMENT_BYTES, MIME_JPEG));
            verify(0, postRequestedFor(urlEqualTo(ATTACHMENTS_PATH)));
        }
    }

    // ---- downloadAttachment ----

    @Test
    void downloadAttachment_whenCalled_getsRawBytes(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ATTACHMENT_DOWNLOAD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(DOWNLOAD_BYTES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            byte[] bytes = allegro.disputes().downloadAttachment(ATTACHMENT_ID);

            // then
            assertArrayEquals(DOWNLOAD_BYTES, bytes);
            verify(1, getRequestedFor(urlEqualTo(ATTACHMENT_DOWNLOAD_PATH)));
        }
    }

    // ---- write error-path table ----

    @Test
    void addMessage_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(MESSAGE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));
        IssueMessageRequest request = IssueMessageRequest.builder().text(MESSAGE_TEXT).build();

        try (AllegroClient allegro = client(wmInfo)) {
            Disputes disputes = allegro.disputes();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> disputes.addMessage(ISSUE_ID, request));
            assertEquals(1, failure.errors().size());
            assertEquals(ERROR_PATH_TEXT, failure.errors().get(0).path());
        }
    }

    @Test
    void changeStatus_when5xx_isNotRetried(WireMockRuntimeInfo wmInfo) {
        // given — a POST must not be retried on 5xx (retryPost=false)
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withBody(SERVER_ERROR_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();
        ClaimStatusChange change = ClaimStatusChange.builder()
                .status(ClaimStatus.ACCEPTED_REFUND)
                .message(CHANGE_MESSAGE)
                .build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Disputes disputes = allegro.disputes();

            // then
            assertThrows(AllegroServerException.class, () -> disputes.changeStatus(ISSUE_ID, change));
            verify(1, postRequestedFor(urlEqualTo(STATUS_PATH)));
        }
    }

    @Test
    void addMessage_when401Once_reauthenticatesAndReplays(WireMockRuntimeInfo wmInfo) {
        // given — first token, then a fresh one on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(post(urlEqualTo(MESSAGE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(post(urlEqualTo(MESSAGE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(MESSAGE_RESPONSE)));
        IssueMessageRequest request = IssueMessageRequest.builder().text(MESSAGE_TEXT).build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            IssueChatEntry created = allegro.disputes().addMessage(ISSUE_ID, request);

            // then — replayed once with the fresh token
            assertEquals(CREATED_MESSAGE_ID, created.id());
            verify(RETRY_TWICE, postRequestedFor(urlEqualTo(MESSAGE_PATH)));
            verify(1, postRequestedFor(urlEqualTo(MESSAGE_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }
}
