/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.disputes;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.Disputes;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.builder.IssueFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.ChatAuthorRole;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.Issue;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueChatEntry;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueExpectationName;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueReasonType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueRight;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.disputes.model.IssueType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Full-stack WireMock coverage of the disputes (post-purchase issues) read facade:
 * the beta vendor media type, lazy paginated streaming (issues and chat), Raw → record
 * mapping (enums, buyer, state, chat author/role, attachments), and the error-path table
 * (400 field errors, 401 replay, 404, 429, 5xx). The facade is read-only, so the
 * POST-not-retried row does not apply.
 */
@WireMockTest
class DisputesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;

    private static final String ISSUES_PATH = "/sale/issues";
    private static final String ISSUE_ID = "issue-1";
    private static final String ISSUE_PATH = ISSUES_PATH + "/" + ISSUE_ID;
    private static final String CHAT_PATH = ISSUE_PATH + "/chat";

    private static final String ORDER_ID = "order-9";
    private static final String EXPECTED_ORDER_CREATED_AT = "2026-07-10T08:00:00Z";
    private static final Integer EXPECTED_OFFER_QUANTITY = 2;
    private static final Integer EXPECTED_MESSAGES_COUNT = 3;
    private static final String EXPECTED_INITIAL_MESSAGE_ID = "msg-1";
    private static final String EXPECTED_LAST_MESSAGE_AT = "2026-07-16T12:00:00Z";
    private static final String BUYER_LOGIN = "buyer-login";
    private static final String OFFER_ID = "6789012345";
    private static final String PRODUCT_ID = "5f4e3d2c-1b0a-4988-8776-655443322110";
    private static final String REASON_DESCRIPTION = "Broke on second use";
    private static final String EXPECTED_REFUND_AMOUNT = "19.99";
    private static final String EXPECTED_REFUND_CURRENCY = "PLN";
    private static final String ISSUE_ATTACHMENT_FILENAME = "evidence.jpg";
    private static final String ISSUE_ATTACHMENT_URL = "https://x.allegro.pl/evidence.jpg";
    private static final int ONE_ELEMENT = 1;
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String OFFSET_PARAM = "offset";
    private static final String LIMIT_PARAM = "limit";
    private static final String STATUS_PARAM = "status";
    private static final String CHECKOUT_FORM_PARAM = "checkoutForm.id";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String STATE_RECOVERED = "recovered";

    private static final int RETRY_TWICE = 2;
    private static final String RETRY_AFTER_SECONDS = "1";
    private static final long RETRY_AFTER_EXPECTED = 1L;
    private static final String FIELD_ERROR_CODE = "ValidationError";
    private static final String FIELD_ERROR_PATH = "status";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (PostPurchaseIssue example shape from the beta spec;
    // a sandbox capture drops this mark once a seeded dispute exists — buyer-opened, web-only)
    private static final String ISSUE_RESPONSE = """
            {"id":"%s","type":"CLAIM","right":"COMPLAINT","referenceNumber":"REF-123",
             "subject":"Damaged item","description":"Arrived broken",
             "reason":{"type":"DEFECT_FOUND_DURING_USE","description":"%s"},
             "expectations":[{"name":"PARTIAL_REFUND","refund":{"amount":"%s","currency":"%s"}}],
             "openedDate":"2026-07-15T10:00:00Z","decisionDueDate":"2026-07-22T10:00:00Z",
             "buyer":{"id":"b1","login":"%s"},
             "checkoutForm":{"id":"%s","createdAt":"2026-07-10T08:00:00Z"},
             "offer":{"id":"%s","quantity":2},"product":{"id":"%s"},
             "attachments":[{"fileName":"%s","url":"%s"}],
             "chat":{"messagesCount":3,
               "initialMessage":{"id":"msg-1","text":"Item arrived damaged",
                 "createdAt":"2026-07-15T10:05:00Z",
                 "author":{"login":"buyer-login","role":"BUYER"},"attachments":[]},
               "lastMessage":{"createdAt":"2026-07-16T12:00:00Z"}},
             "currentState":{"status":"CLAIM_SUBMITTED","statusDueDate":"2026-07-20T10:00:00Z",
               "returnRequired":true,"chatActive":true}}
            """.formatted(ISSUE_ID, REASON_DESCRIPTION, EXPECTED_REFUND_AMOUNT, EXPECTED_REFUND_CURRENCY,
            BUYER_LOGIN, ORDER_ID, OFFER_ID, PRODUCT_ID, ISSUE_ATTACHMENT_FILENAME, ISSUE_ATTACHMENT_URL);
    // spec-derived: not yet wire-verified
    private static final String CHAT_RESPONSE = """
            {"chat":[{"id":"chat-1","text":"Please return the item.",
              "createdAt":"2026-07-16T09:00:00Z",
              "author":{"login":"seller-login","role":"SELLER"},
              "attachments":[{"fileName":"photo.jpg","url":"https://x.allegro.pl/photo.jpg"}]}]}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"Unknown status",
              "userMessage":"Nieznany status","path":"%s","details":null}]}
            """.formatted(FIELD_ERROR_CODE, FIELD_ERROR_PATH);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Issue not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String BUSY_RESPONSE = "{\"errors\":[]}";
    // forward-compat (C3): a wire enum value this SDK version predates must degrade to UNKNOWN,
    // not fail deserialization (Layer-1 enumUnknownDefaultCase → the mappers' `default -> UNKNOWN`).
    private static final String FUTURE_WIRE_STATUS = "FUTURE_DISPUTE_STATUS";
    private static final String FUTURE_WIRE_ROLE = "FUTURE_AUTHOR_ROLE";
    private static final String FUTURE_WIRE_REASON = "FUTURE_ISSUE_REASON";
    private static final String FUTURE_WIRE_EXPECTATION = "FUTURE_ISSUE_EXPECTATION";
    private static final String UNKNOWN_STATUS_ISSUE_RESPONSE = """
            {"id":"%s","type":"CLAIM","right":"COMPLAINT",
             "currentState":{"status":"%s"}}
            """.formatted(ISSUE_ID, FUTURE_WIRE_STATUS);
    private static final String UNKNOWN_REASON_EXPECTATION_ISSUE_RESPONSE = """
            {"id":"%s","type":"CLAIM","right":"COMPLAINT",
             "reason":{"type":"%s","description":"%s"},
             "expectations":[{"name":"%s"}]}
            """.formatted(ISSUE_ID, FUTURE_WIRE_REASON, REASON_DESCRIPTION, FUTURE_WIRE_EXPECTATION);
    private static final String UNKNOWN_ROLE_CHAT_RESPONSE = """
            {"chat":[{"id":"chat-1","text":"hello",
              "author":{"login":"someone","role":"%s"}}]}
            """.formatted(FUTURE_WIRE_ROLE);

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
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static String fullPageOfIssues(int count) {
        StringBuilder json = new StringBuilder("{\"issues\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"i").append(index)
                    .append("\",\"type\":\"DISPUTE\",\"right\":\"WARRANTY\"}");
        }
        return json.append("]}").toString();
    }

    private static String fullPageOfChat(int count) {
        StringBuilder json = new StringBuilder("{\"chat\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"c").append(index)
                    .append("\",\"text\":\"hi\",\"author\":{\"login\":\"x\",\"role\":\"BUYER\"}}");
        }
        return json.append("]}").toString();
    }

    // ---- streaming (laziness + mapping + beta media type) ----

    @Test
    void streamIssues_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page; the second page must not be requested
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ISSUES_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfIssues(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<Issue> firstOnly = allegro.disputes().streamIssues(IssueFilter.none())
                    .limit(1).toList();

            // then — page two (offset=100) never requested; beta media type used
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(ISSUES_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo("0"))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER,
                            equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1)));
            verify(0, getRequestedFor(urlPathEqualTo(ISSUES_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamIssues_whenFilterGiven_sendsStatusAndCheckoutFormParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfIssues(1))));
        IssueFilter filter = IssueFilter.builder()
                .status(IssueStatus.DISPUTE_ONGOING)
                .status(IssueStatus.CLAIM_SUBMITTED)
                .checkoutFormId(ORDER_ID)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.disputes().streamIssues(filter).toList();

            // then — repeated status params + checkoutForm.id propagate
            verify(getRequestedFor(urlPathEqualTo(ISSUES_PATH))
                    .withQueryParam(STATUS_PARAM, equalTo("DISPUTE_ONGOING"))
                    .withQueryParam(STATUS_PARAM, equalTo("CLAIM_SUBMITTED"))
                    .withQueryParam(CHECKOUT_FORM_PARAM, equalTo(ORDER_ID)));
        }
    }

    @Test
    void streamChat_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHAT_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfChat(PAGE_SIZE))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<IssueChatEntry> firstOnly = allegro.disputes().streamChat(ISSUE_ID).limit(1).toList();

            // then
            assertEquals(1, firstOnly.size());
            verify(0, getRequestedFor(urlPathEqualTo(CHAT_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamChat_whenTraversed_mapsAuthorRoleAndAttachments(WireMockRuntimeInfo wmInfo) {
        // given — a single short page maps and terminates
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHAT_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(CHAT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<IssueChatEntry> chat = allegro.disputes().streamChat(ISSUE_ID).toList();

            // then
            assertEquals(1, chat.size());
            IssueChatEntry entry = chat.get(0);
            assertEquals(ChatAuthorRole.SELLER, entry.author().role());
            assertEquals(1, entry.attachments().size());
            assertEquals("photo.jpg", entry.attachments().get(0).fileName());
        }
    }

    // ---- get (mapping + beta media type) ----

    @Test
    void get_whenIssueExists_sendsBetaAcceptAndMapsAllFacets(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_BETA_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ISSUE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Issue issue = allegro.disputes().get(ISSUE_ID);

            // then — every mapped facet survives
            assertEquals(ISSUE_ID, issue.id());
            assertEquals(IssueType.CLAIM, issue.type());
            assertEquals(IssueRight.COMPLAINT, issue.right());
            assertEquals(BUYER_LOGIN, issue.buyer().login());
            assertEquals(ORDER_ID, issue.checkoutFormId());
            assertEquals(IssueStatus.CLAIM_SUBMITTED, issue.state().status());
            assertTrue(issue.state().returnRequired());
            // then — the depth facets map too: reason, expectations (with refund), refs, attachments
            assertEquals(IssueReasonType.DEFECT_FOUND_DURING_USE, issue.reason().type());
            assertEquals(REASON_DESCRIPTION, issue.reason().description());
            assertEquals(ONE_ELEMENT, issue.expectations().size());
            assertEquals(IssueExpectationName.PARTIAL_REFUND, issue.expectations().get(0).name());
            assertEquals(EXPECTED_REFUND_AMOUNT, issue.expectations().get(0).refund().amount());
            assertEquals(EXPECTED_REFUND_CURRENCY, issue.expectations().get(0).refund().currency());
            assertEquals(OFFER_ID, issue.offerId());
            assertEquals(PRODUCT_ID, issue.productId());
            assertEquals(ONE_ELEMENT, issue.attachments().size());
            assertEquals(ISSUE_ATTACHMENT_FILENAME, issue.attachments().get(0).fileName());
            assertEquals(ISSUE_ATTACHMENT_URL, issue.attachments().get(0).url());
            // then — enriched depth: order created-at, offer quantity, and the chat summary
            assertEquals(OffsetDateTime.parse(EXPECTED_ORDER_CREATED_AT), issue.checkoutFormCreatedAt());
            assertEquals(EXPECTED_OFFER_QUANTITY, issue.offerQuantity());
            assertEquals(EXPECTED_MESSAGES_COUNT, issue.chat().messagesCount());
            assertEquals(EXPECTED_INITIAL_MESSAGE_ID, issue.chat().initialMessage().id());
            assertEquals(ChatAuthorRole.BUYER, issue.chat().initialMessage().author().role());
            assertEquals(OffsetDateTime.parse(EXPECTED_LAST_MESSAGE_AT), issue.chat().lastMessageAt());
            verify(1, getRequestedFor(urlEqualTo(ISSUE_PATH)));
        }
    }

    // ---- forward compatibility (C3): unknown wire enum values degrade to UNKNOWN ----

    @Test
    void get_whenStatusWireValueUnknown_mapsStateStatusToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — the server introduces a dispute status this SDK version predates
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_STATUS_ISSUE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — deserialization must not throw on the unrecognized enum value
            Issue issue = allegro.disputes().get(ISSUE_ID);

            // then — the unknown wire value degrades to UNKNOWN end-to-end
            assertEquals(IssueStatus.UNKNOWN, issue.state().status());
        }
    }

    @Test
    void get_whenReasonAndExpectationWireValuesUnknown_mapBothToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — the server introduces a reason and an expectation this SDK version predates
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_REASON_EXPECTATION_ISSUE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when — deserialization must not throw on the unrecognized enum values
            Issue issue = allegro.disputes().get(ISSUE_ID);

            // then — both unknown wire values degrade to UNKNOWN end-to-end
            assertEquals(IssueReasonType.UNKNOWN, issue.reason().type());
            // and the free-text description survives even when the enum type is unknown
            assertEquals(REASON_DESCRIPTION, issue.reason().description());
            assertEquals(ONE_ELEMENT, issue.expectations().size());
            assertEquals(IssueExpectationName.UNKNOWN, issue.expectations().get(0).name());
            // no refund quoted in this fixture → the mapper leaves it null, not a zero Money
            assertNull(issue.expectations().get(0).refund());
        }
    }

    @Test
    void streamChat_whenAuthorRoleWireValueUnknown_mapsRoleToUnknown(WireMockRuntimeInfo wmInfo) {
        // given — a chat author role introduced after this SDK version
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(CHAT_PATH)).withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_ROLE_CHAT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<IssueChatEntry> chat = allegro.disputes().streamChat(ISSUE_ID).toList();

            // then — an unrecognized author role degrades to UNKNOWN, not a failure
            assertEquals(1, chat.size());
            assertEquals(ChatAuthorRole.UNKNOWN, chat.get(0).author().role());
        }
    }

    // ---- error-path table (per facade) ----

    @Test
    void streamIssues_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Disputes disputes = allegro.disputes();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> disputes.streamIssues(IssueFilter.none()).toList());
            assertEquals(1, failure.errors().size());
            assertEquals(FIELD_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(FIELD_ERROR_PATH, failure.errors().get(0).path());
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ISSUE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Issue issue = allegro.disputes().get(ISSUE_ID);

            // then — replayed once, second request carried the fresh token
            assertEquals(ISSUE_ID, issue.id());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(ISSUE_PATH)));
            verify(1, getRequestedFor(urlEqualTo(ISSUE_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ISSUE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Disputes disputes = allegro.disputes();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> disputes.get(ISSUE_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void streamIssues_when429Persists_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — always throttled; a two-attempt policy exhausts and surfaces the 429
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS)
                        .withBody(BUSY_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Disputes disputes = allegro.disputes();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> disputes.streamIssues(IssueFilter.none()).toList());
            assertEquals(RETRY_AFTER_EXPECTED, failure.retryAfterSeconds());
            verify(RETRY_TWICE, getRequestedFor(urlPathEqualTo(ISSUES_PATH)));
        }
    }

    @Test
    void get_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — a GET is idempotent, so a transient 500 is retried
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ISSUE_PATH)).inScenario(SCENARIO_RECOVER)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(ISSUE_PATH)).inScenario(SCENARIO_RECOVER)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(ISSUE_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            // when
            Issue issue = allegro.disputes().get(ISSUE_ID);

            // then
            assertEquals(ISSUE_ID, issue.id());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(ISSUE_PATH)));
        }
    }
}
