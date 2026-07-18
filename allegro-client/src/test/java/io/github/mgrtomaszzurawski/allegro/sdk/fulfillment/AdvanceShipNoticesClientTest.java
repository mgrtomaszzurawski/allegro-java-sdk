/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.fulfillment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.AdvanceShipNotices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.AsnRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.builder.SubmittedAsnUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AdvanceShipNotice;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.AsnStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.HandlingUnit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReasonCode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReceivedType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReceivingState;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.ReceivingStage;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.fulfillment.model.SubmitStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the Advance Ship Notices lifecycle sub-facade
 * ({@code fulfillment().advanceShipNotices()}): lazy count-based pagination, the
 * status filter (dropping the forward-compat {@code UNKNOWN} sentinel), the
 * {@code ETag}/{@code If-Match} optimistic-concurrency round-trip across reads
 * and writes, the async submit command polled to a terminal state, PDF label
 * bytes, the receiving-state tree, and the mandatory error-path table.
 *
 * <p>Response fixtures are {@code spec-derived}: One Fulfillment needs an enrolled
 * seller account, so they are pinned to the spec shapes and reconciled against a
 * live capture before the bucket's final PR (see {@code KNOWN-SERVER-BEHAVIORS.md}).
 */
@WireMockTest
class AdvanceShipNoticesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;

    private static final String ASN_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String PRODUCT_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ASN_BASE = "/fulfillment/advance-ship-notices";
    private static final String ASN_ONE = ASN_BASE + "/" + ASN_ID;
    private static final String ASN_CANCEL = ASN_ONE + "/cancel";
    private static final String ASN_LABELS = ASN_ONE + "/labels";
    private static final String ASN_SUBMITTED = ASN_ONE + "/submitted";
    private static final String ASN_RECEIVING = ASN_ONE + "/receiving-state";
    private static final String SUBMIT_CMD_PATTERN = "/fulfillment/submit-commands/[^/]+";

    private static final String ETAG_HEADER = "ETag";
    private static final String VERSION_ONE = "v-1";
    private static final String VERSION_TWO = "v-2";

    private static final String QUERY_OFFSET = "offset";
    private static final String QUERY_STATUS = "status";
    private static final String OFFSET_FIRST = "0";
    private static final String OFFSET_SECOND = "100";
    private static final String OFFSET_THIRD = "200";

    private static final String DISPLAY_NUMBER = "ASN-001";
    private static final String UNIT_TYPE = "BOX";
    private static final String LABELS_TYPE = "ONE_FULFILMENT";
    private static final int ITEM_QUANTITY = 12;
    private static final int HANDLING_AMOUNT = 2;
    private static final int DECLARED_VOLUME = 15_000;

    private static final String JSON_ITEM_PRODUCT = "$.items[0].product.id";
    private static final String JSON_ITEM_QUANTITY = "$.items[0].quantity";
    private static final String JSON_HANDLING_UNIT_TYPE = "$.handlingUnit.unitType";
    private static final String JSON_SUBMIT_ASN_ID = "$.input.advanceShipNoticeId";

    private static final String SCENARIO_POLL = "submit-poll";
    private static final String STATE_RUNNING_SEEN = "running-seen";
    private static final String SCENARIO_REPLAY = "reauth";
    private static final String STATE_REAUTHED = "reauthed";

    private static final byte[] LABEL_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34};

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;

    private static final String ASN_FULL_RESPONSE = """
            {"id":"%s","displayNumber":"%s","status":"DRAFT",
             "createdAt":"2026-07-10T12:00:00Z","updatedAt":"2026-07-11T09:00:00Z",
             "items":[{"product":{"id":"%s"},"quantity":%d}],
             "handlingUnit":{"unitType":"%s","amount":%d,"labelsType":"%s"},
             "labels":{"fileUrl":"http://labels/asn-001.pdf"},
             "submittedAt":"2026-07-12T08:00:00Z","volumeInCc":%d}
            """.formatted(ASN_ID, DISPLAY_NUMBER, PRODUCT_ID, ITEM_QUANTITY,
            UNIT_TYPE, HANDLING_AMOUNT, LABELS_TYPE, DECLARED_VOLUME);

    private static final String ASN_CREATE_RESPONSE = """
            {"id":"%s","displayNumber":"%s","status":"DRAFT",
             "createdAt":"2026-07-10T12:00:00Z","updatedAt":"2026-07-10T12:00:00Z",
             "items":[{"product":{"id":"%s"},"quantity":%d}]}
            """.formatted(ASN_ID, DISPLAY_NUMBER, PRODUCT_ID, ITEM_QUANTITY);

    private static final String RECEIVING_RESPONSE = """
            {"updatedAt":"2026-07-13T10:00:00Z","stage":"IN_PROGRESS","displayNumber":"%s",
             "content":[{"expected":%d,"product":{"id":"%s"},
               "received":[{"quantity":%d,"receivedType":"SELLABLE","reasonCode":"SELLABLE"}]}]}
            """.formatted(DISPLAY_NUMBER, ITEM_QUANTITY, PRODUCT_ID, ITEM_QUANTITY);

    private static final String SUBMIT_RUNNING = """
            {"input":{"advanceShipNoticeId":"%s"},"output":{"status":"RUNNING"}}
            """.formatted(ASN_ID);
    private static final String SUBMIT_SUCCESSFUL = """
            {"input":{"advanceShipNoticeId":"%s"},"output":{"status":"SUCCESSFUL"}}
            """.formatted(ASN_ID);
    private static final String SUBMIT_FAILED = """
            {"input":{"advanceShipNoticeId":"%s"},"output":{"status":"FAILED"}}
            """.formatted(ASN_ID);
    private static final String SUBMIT_ACCEPTED = "{}";

    private static final String ERROR_RESPONSE = """
            {"errors":[{"code":"SomeCode","message":"boom","userMessage":"boom","path":null}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .retryPolicy(RetryPolicy.builder().enabled(false).build())
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static AsnRequest sampleRequest() {
        return AsnRequest.builder()
                .addItem(PRODUCT_ID, ITEM_QUANTITY)
                .handlingUnit(new HandlingUnit(UNIT_TYPE, BigDecimal.valueOf(HANDLING_AMOUNT), LABELS_TYPE))
                .declaredVolumeInCc(BigDecimal.valueOf(DECLARED_VOLUME))
                .build();
    }

    /** A list page of {@code count} minimal-but-valid ASN summaries, advertising {@code totalCount}. */
    private static String asnPage(int count, int totalCount) {
        StringBuilder json = new StringBuilder("{\"advanceShipNotices\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"").append(ASN_ID).append("\",\"displayNumber\":\"").append(DISPLAY_NUMBER)
                    .append("\",\"status\":\"IN_TRANSIT\",\"createdAt\":\"2026-07-10T12:00:00Z\",")
                    .append("\"updatedAt\":\"2026-07-10T12:00:00Z\",\"items\":[]}");
        }
        return json.append("],\"count\":").append(count)
                .append(",\"totalCount\":").append(totalCount).append('}').toString();
    }

    private static void stubNoticesPage(String offset, String body) {
        stubFor(get(urlPathEqualTo(ASN_BASE))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withQueryParam(QUERY_OFFSET, equalTo(offset))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(body)));
    }

    // ---- streamNotices ----

    @Test
    void streamNotices_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies more, but only the first is consumed
        stubToken(TEST_TOKEN);
        stubNoticesPage(OFFSET_FIRST, asnPage(PAGE_SIZE, 500));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<AdvanceShipNotice> firstOnly =
                    allegro.fulfillment().advanceShipNotices().streamNotices().limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(0, getRequestedFor(urlPathEqualTo(ASN_BASE))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
        }
    }

    @Test
    void streamNotices_whenOffsetReachesTotalCount_stopsWithoutExtraFetch(WireMockRuntimeInfo wmInfo) {
        // given — two full pages, total 200; count-based termination must stop at 200
        stubToken(TEST_TOKEN);
        stubNoticesPage(OFFSET_FIRST, asnPage(PAGE_SIZE, 200));
        stubNoticesPage(OFFSET_SECOND, asnPage(PAGE_SIZE, 200));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            long total = allegro.fulfillment().advanceShipNotices().streamNotices().count();

            // then
            assertEquals(200L, total);
            verify(1, getRequestedFor(urlPathEqualTo(ASN_BASE))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_SECOND)));
            verify(0, getRequestedFor(urlPathEqualTo(ASN_BASE))
                    .withQueryParam(QUERY_OFFSET, equalTo(OFFSET_THIRD)));
        }
    }

    @Test
    void streamNotices_whenFilteredByStatus_sendsStatusTokenAndMaps(WireMockRuntimeInfo wmInfo) {
        // given — a DRAFT filter must send status=DRAFT on the wire
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ASN_BASE)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .withQueryParam(QUERY_STATUS, equalTo("DRAFT"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(asnPage(1, 1))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AsnFilter filter = AsnFilter.builder().addStatus(AsnStatus.DRAFT).build();
            List<AdvanceShipNotice> notices =
                    allegro.fulfillment().advanceShipNotices().streamNotices(filter).toList();

            // then — DRAFT reached the wire and the row mapped
            assertEquals(1, notices.size());
            assertEquals(AsnStatus.IN_TRANSIT, notices.get(0).status());
            verify(1, getRequestedFor(urlPathEqualTo(ASN_BASE))
                    .withQueryParam(QUERY_STATUS, equalTo("DRAFT")));
        }
    }

    @Test
    void streamNotices_whenFilterHasUnknownSentinel_dropsItFromQuery(WireMockRuntimeInfo wmInfo) {
        // given — a filter of only the forward-compat UNKNOWN sentinel (no wire token)
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ASN_BASE)).withQueryParam(QUERY_OFFSET, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(asnPage(0, 0))));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AsnFilter filter = AsnFilter.builder().addStatus(AsnStatus.UNKNOWN).build();
            allegro.fulfillment().advanceShipNotices().streamNotices(filter).toList();

            // then — the sentinel is never sent as a filter (it would 400 on the server)
            verify(1, getRequestedFor(urlPathEqualTo(ASN_BASE))
                    .withQueryParam(QUERY_STATUS, absent()));
        }
    }

    // ---- get (ETag -> version) ----

    @Test
    void get_whenNoticeReturned_mapsTreeAndCapturesVersion(WireMockRuntimeInfo wmInfo) {
        // given — the response carries an ETag the SDK must surface as version()
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(ETAG_HEADER, VERSION_ONE).withBody(ASN_FULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdvanceShipNotice notice = allegro.fulfillment().advanceShipNotices().get(ASN_ID);

            // then — the nested tree, enum and the ETag all survive mapping
            assertEquals(ASN_ID, notice.id());
            assertEquals(DISPLAY_NUMBER, notice.displayNumber());
            assertEquals(AsnStatus.DRAFT, notice.status());
            assertEquals(PRODUCT_ID, notice.items().get(0).productId());
            assertEquals(BigDecimal.valueOf(ITEM_QUANTITY), notice.items().get(0).quantity());
            assertEquals(UNIT_TYPE, notice.handlingUnit().unitType());
            assertEquals(VERSION_ONE, notice.version());
        }
    }

    @Test
    void get_whenStatusIsUnknownToServer_degradesToUnknownEnum(WireMockRuntimeInfo wmInfo) {
        // given — a status this build does not know (open value set); C3 makes the
        // Layer-1 parse degrade instead of throwing, so the domain enum reaches UNKNOWN
        stubToken(TEST_TOKEN);
        String body = ASN_FULL_RESPONSE.replace("\"status\":\"DRAFT\"", "\"status\":\"SOME_FUTURE_STATUS\"");
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(body)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdvanceShipNotice notice = allegro.fulfillment().advanceShipNotices().get(ASN_ID);

            // then — no thrown deserialization error; the unknown token maps to UNKNOWN
            assertEquals(AsnStatus.UNKNOWN, notice.status());
        }
    }

    // ---- create ----

    @Test
    void create_whenValidRequest_postsBodyAndReturnsVersion(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(ASN_BASE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withHeader(ETAG_HEADER, VERSION_ONE).withBody(ASN_CREATE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdvanceShipNotice created = allegro.fulfillment().advanceShipNotices().create(sampleRequest());

            // then — the item/shipping/handling body reached the wire and the ETag is captured
            assertEquals(ASN_ID, created.id());
            assertEquals(VERSION_ONE, created.version());
            verify(1, postRequestedFor(urlEqualTo(ASN_BASE))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                    .withRequestBody(matchingJsonPath(JSON_ITEM_PRODUCT, equalTo(PRODUCT_ID)))
                    .withRequestBody(matchingJsonPath(JSON_ITEM_QUANTITY, equalTo(String.valueOf(ITEM_QUANTITY))))
                    .withRequestBody(matchingJsonPath(JSON_HANDLING_UNIT_TYPE, equalTo(UNIT_TYPE))));
        }
    }

    // ---- update (If-Match) ----

    @Test
    void update_whenVersionGiven_sendsIfMatchAndReturnsNewVersion(WireMockRuntimeInfo wmInfo) {
        // given — the write must carry If-Match: v-1 and surface the fresh ETag v-2
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(ASN_ONE))
                .withHeader(TestHttpConstants.IF_MATCH_HEADER, equalTo(VERSION_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(ETAG_HEADER, VERSION_TWO).withBody(ASN_FULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdvanceShipNotice updated =
                    allegro.fulfillment().advanceShipNotices().update(ASN_ID, sampleRequest(), VERSION_ONE);

            // then
            assertEquals(VERSION_TWO, updated.version());
            verify(1, putRequestedFor(urlEqualTo(ASN_ONE))
                    .withHeader(TestHttpConstants.IF_MATCH_HEADER, equalTo(VERSION_ONE))
                    .withRequestBody(matchingJsonPath(JSON_ITEM_PRODUCT, equalTo(PRODUCT_ID))));
        }
    }

    @Test
    void update_whenVersionNull_throwsBeforeSending(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        try (AllegroClient allegro = client(wmInfo)) {
            AdvanceShipNotices asn = allegro.fulfillment().advanceShipNotices();
            AsnRequest request = sampleRequest();

            // then — fail-fast, no request leaves the SDK
            assertThrows(NullPointerException.class, () -> asn.update(ASN_ID, request, null));
            verify(0, putRequestedFor(urlEqualTo(ASN_ONE)));
        }
    }

    // ---- updateSubmitted (If-Match) ----

    @Test
    void updateSubmitted_whenVersionGiven_sendsIfMatchAndBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(ASN_SUBMITTED))
                .withHeader(TestHttpConstants.IF_MATCH_HEADER, equalTo(VERSION_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(ETAG_HEADER, VERSION_TWO).withBody(ASN_FULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            SubmittedAsnUpdate update = SubmittedAsnUpdate.builder()
                    .handlingUnit(new HandlingUnit(UNIT_TYPE, BigDecimal.valueOf(HANDLING_AMOUNT), LABELS_TYPE))
                    .build();
            AdvanceShipNotice amended =
                    allegro.fulfillment().advanceShipNotices().updateSubmitted(ASN_ID, update, VERSION_ONE);

            // then
            assertEquals(VERSION_TWO, amended.version());
            verify(1, putRequestedFor(urlEqualTo(ASN_SUBMITTED))
                    .withHeader(TestHttpConstants.IF_MATCH_HEADER, equalTo(VERSION_ONE))
                    .withRequestBody(matchingJsonPath(JSON_HANDLING_UNIT_TYPE, equalTo(UNIT_TYPE))));
        }
    }

    // ---- submit (async command) ----

    @Test
    void submit_whenCommandRunsThenSucceeds_pollsToSuccessful(WireMockRuntimeInfo wmInfo) {
        // given — PUT accepts the command, then the first poll is RUNNING and the second SUCCESSFUL
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathMatching(SUBMIT_CMD_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(SUBMIT_ACCEPTED)));
        stubFor(get(urlPathMatching(SUBMIT_CMD_PATTERN)).inScenario(SCENARIO_POLL)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUBMIT_RUNNING))
                .willSetStateTo(STATE_RUNNING_SEEN));
        stubFor(get(urlPathMatching(SUBMIT_CMD_PATTERN)).inScenario(SCENARIO_POLL)
                .whenScenarioStateIs(STATE_RUNNING_SEEN)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUBMIT_SUCCESSFUL)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            SubmitStatus status = allegro.fulfillment().advanceShipNotices().submit(ASN_ID);

            // then — the command polled past RUNNING to the terminal SUCCESSFUL
            assertEquals(SubmitStatus.SUCCESSFUL, status);
            verify(1, putRequestedFor(urlPathMatching(SUBMIT_CMD_PATTERN))
                    .withRequestBody(matchingJsonPath(JSON_SUBMIT_ASN_ID, equalTo(ASN_ID))));
            verify(2, getRequestedFor(urlPathMatching(SUBMIT_CMD_PATTERN)));
        }
    }

    @Test
    void submit_whenCommandFails_returnsFailedWithoutThrowing(WireMockRuntimeInfo wmInfo) {
        // given — the command reaches the terminal FAILED state
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathMatching(SUBMIT_CMD_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(SUBMIT_ACCEPTED)));
        stubFor(get(urlPathMatching(SUBMIT_CMD_PATTERN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(SUBMIT_FAILED)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            SubmitStatus status = allegro.fulfillment().advanceShipNotices().submit(ASN_ID, Duration.ofSeconds(5));

            // then — a failed command is terminal data, surfaced as FAILED (not an exception)
            assertEquals(SubmitStatus.FAILED, status);
        }
    }

    // ---- cancel / delete (void writes) ----

    @Test
    void cancel_whenCalled_putsCancelPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(ASN_CANCEL))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.fulfillment().advanceShipNotices().cancel(ASN_ID);

            // then — the authenticated PUT reached the cancel path
            verify(1, putRequestedFor(urlEqualTo(ASN_CANCEL))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN)));
        }
    }

    @Test
    void delete_whenCalled_deletesNotice(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            allegro.fulfillment().advanceShipNotices().delete(ASN_ID);

            // then
            verify(1, deleteRequestedFor(urlEqualTo(ASN_ONE)));
        }
    }

    // ---- labels (PDF bytes) ----

    @Test
    void labels_whenCalled_requestsPdfAndReturnsBytes(WireMockRuntimeInfo wmInfo) {
        // given — the label endpoint returns a PDF the SDK reads as raw bytes
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_LABELS))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo("application/pdf"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, "application/pdf")
                        .withBody(LABEL_BYTES)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            byte[] pdf = allegro.fulfillment().advanceShipNotices().labels(ASN_ID);

            // then — the exact bytes come back and the PDF Accept header was sent
            assertArrayEquals(LABEL_BYTES, pdf);
            verify(1, getRequestedFor(urlEqualTo(ASN_LABELS))
                    .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo("application/pdf")));
        }
    }

    // ---- receiving state ----

    @Test
    void receivingState_whenReturned_mapsTree(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_RECEIVING))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(RECEIVING_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ReceivingState state = allegro.fulfillment().advanceShipNotices().receivingState(ASN_ID);

            // then — the stage, entry and received breakdown (including enums) survive
            assertEquals(ReceivingStage.IN_PROGRESS, state.stage());
            assertEquals(1, state.content().size());
            assertEquals(ITEM_QUANTITY, state.content().get(0).expected());
            assertEquals(PRODUCT_ID, state.content().get(0).productId());
            assertEquals(ReceivedType.SELLABLE, state.content().get(0).received().get(0).receivedType());
            assertEquals(ReasonCode.SELLABLE, state.content().get(0).received().get(0).reasonCode());
        }
    }

    // ---- error-path table (on get) ----

    @Test
    void get_whenBadRequest_throwsBadRequest(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdvanceShipNotices asn = allegro.fulfillment().advanceShipNotices();
            // then
            assertThrows(AllegroBadRequestException.class, () -> asn.get(ASN_ID));
        }
    }

    @Test
    void get_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdvanceShipNotices asn = allegro.fulfillment().advanceShipNotices();
            // then
            assertThrows(AllegroNotFoundException.class, () -> asn.get(ASN_ID));
        }
    }

    @Test
    void get_whenRateLimited_throwsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, "5").withBody(ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdvanceShipNotices asn = allegro.fulfillment().advanceShipNotices();
            // then
            assertThrows(AllegroRateLimitException.class, () -> asn.get(ASN_ID));
        }
    }

    @Test
    void get_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(ASN_ONE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR).withBody(ERROR_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AdvanceShipNotices asn = allegro.fulfillment().advanceShipNotices();
            // then
            assertThrows(AllegroServerException.class, () -> asn.get(ASN_ID));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — first GET 401; after re-auth 200 with a fresh token
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(ASN_ONE))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(ASN_ONE))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(ETAG_HEADER, VERSION_ONE).withBody(ASN_FULL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            AdvanceShipNotice notice = allegro.fulfillment().advanceShipNotices().get(ASN_ID);

            // then — replayed once, the second request carried the fresh token
            assertEquals(ASN_ID, notice.id());
            verify(2, getRequestedFor(urlEqualTo(ASN_ONE)));
            verify(1, getRequestedFor(urlEqualTo(ASN_ONE))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }
}
