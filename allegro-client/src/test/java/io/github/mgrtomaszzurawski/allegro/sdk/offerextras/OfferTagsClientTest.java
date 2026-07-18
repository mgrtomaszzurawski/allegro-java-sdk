/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offerextras;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.builder.TagRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offerextras.model.Tag;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the offer-tags facade, reached through
 * {@code offers().tags()} — the first bucket-F sub-facade wired onto the bucket-A
 * {@code Offers} root. Pins the tag catalogue CRUD, the per-offer assignment
 * read/write (request bodies verified), lazy stream pagination, and the mandatory
 * error-path table on {@code ofOffer} as the facade's representative endpoint.
 */
@WireMockTest
class OfferTagsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final String TAGS_PATH = "/sale/offer-tags";
    private static final String TEST_TAG_ID = "8f1c-tag-01";
    private static final String TEST_TAG_ID_2 = "8f1c-tag-02";
    private static final String TAG_PATH = TAGS_PATH + "/" + TEST_TAG_ID;
    private static final String TEST_OFFER_ID = "8235476198";
    private static final String OFFER_TAGS_PATH = "/sale/offers/" + TEST_OFFER_ID + "/tags";
    private static final String OFFSET_PARAM = "offset";
    private static final String LIMIT_PARAM = "limit";
    private static final String OFFSET_FIRST = "0";
    private static final String OFFSET_SECOND = "100";
    private static final String PAGE_SIZE = "100";
    private static final int FULL_PAGE = 100;
    private static final int SHORT_PAGE = 20;
    private static final String TEST_TAG_NAME = "Priority";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final long RETRY_AFTER_SECONDS = 1L;
    private static final int FAST_MAX_ATTEMPTS = 2;

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified. A tag list with a visible and a hidden tag.
    private static final String TAG_LIST_RESPONSE = """
            {"tags":[
              {"id":"%s","name":"%s","hidden":false},
              {"id":"%s","name":"Archive","hidden":true}
            ]}
            """.formatted(TEST_TAG_ID, TEST_TAG_NAME, TEST_TAG_ID_2);
    private static final String CREATE_RESPONSE = """
            {"id":"%s"}
            """.formatted(TEST_TAG_ID);
    // The exact bodies the writes must send.
    private static final String CREATE_REQUEST_BODY = """
            {"name":"%s","hidden":true}
            """.formatted(TEST_TAG_NAME);
    private static final String ASSIGN_REQUEST_BODY = """
            {"tags":[{"id":"%s"},{"id":"%s"}]}
            """.formatted(TEST_TAG_ID, TEST_TAG_ID_2);
    // spec-derived: not yet wire-verified (errors[] contract shape).
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ConstraintViolationException","message":"invalid",
              "userMessage":"Nieprawidłowe","path":"tags"}]}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFoundException","message":"offer not found","path":null}]}
            """;

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

    private static RetryPolicy oneRetry() {
        return RetryPolicy.builder().maxAttempts(FAST_MAX_ATTEMPTS).build();
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    /** A minimal tag page of {@code size} items. */
    private static String tagPage(int size) {
        StringBuilder body = new StringBuilder("{\"tags\":[");
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                body.append(',');
            }
            body.append("{\"id\":\"").append(index).append("\",\"name\":\"t").append(index)
                    .append("\",\"hidden\":false}");
        }
        return body.append("]}").toString();
    }

    @Test
    void streamTags_whenMultiplePages_streamsAllAndPagesLazily(WireMockRuntimeInfo wmInfo) {
        // given — a full first page (implying more) then a short second page
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_FIRST))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tagPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tagPage(SHORT_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Tag> tags = allegro.offers().tags().streamTags().toList();

            // then — both pages streamed, limit forwarded on each
            assertEquals(FULL_PAGE + SHORT_PAGE, tags.size());
            verify(1, getRequestedFor(urlPathEqualTo(TAGS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(OFFSET_FIRST))
                    .withQueryParam(LIMIT_PARAM, equalTo(PAGE_SIZE)));
            verify(1, getRequestedFor(urlPathEqualTo(TAGS_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(OFFSET_SECOND)));
        }
    }

    @Test
    void streamTags_whenConsumerStopsAtFirstPage_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_FIRST))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tagPage(FULL_PAGE))));
        stubFor(get(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_SECOND))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(tagPage(SHORT_PAGE))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when — take exactly the first page and stop
            long taken = allegro.offers().tags().streamTags().limit(FULL_PAGE).count();

            // then — laziness: the second page is never requested
            assertEquals(FULL_PAGE, taken);
            verify(1, getRequestedFor(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_FIRST)));
            verify(0, getRequestedFor(urlPathEqualTo(TAGS_PATH)).withQueryParam(OFFSET_PARAM, equalTo(OFFSET_SECOND)));
        }
    }

    @Test
    void streamTags_whenTagsReturned_mapsIdNameAndHidden(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(TAGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAG_LIST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Tag> tags = allegro.offers().tags().streamTags().toList();

            // then
            assertEquals(2, tags.size());
            assertEquals(TEST_TAG_ID, tags.get(0).id());
            assertEquals(TEST_TAG_NAME, tags.get(0).name());
            assertFalse(tags.get(0).hidden());
            assertTrue(tags.get(1).hidden());
        }
    }

    @Test
    void create_whenRequestGiven_postsBodyAndReturnsNewId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(TAGS_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(equalToJson(CREATE_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED).withBody(CREATE_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            String newId = allegro.offers().tags()
                    .create(TagRequest.builder().name(TEST_TAG_NAME).hidden(true).build());

            // then — the created id comes back and the body went out once
            assertEquals(TEST_TAG_ID, newId);
            verify(1, postRequestedFor(urlPathEqualTo(TAGS_PATH)).withRequestBody(equalToJson(CREATE_REQUEST_BODY)));
        }
    }

    @Test
    void rename_whenRequestGiven_putsBodyToTagPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlPathEqualTo(TAG_PATH))
                .withRequestBody(equalToJson(CREATE_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().tags().rename(TEST_TAG_ID,
                    TagRequest.builder().name(TEST_TAG_NAME).hidden(true).build());

            // then
            verify(1, putRequestedFor(urlPathEqualTo(TAG_PATH)).withRequestBody(equalToJson(CREATE_REQUEST_BODY)));
        }
    }

    @Test
    void delete_whenTagId_deletesTagPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlPathEqualTo(TAG_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().tags().delete(TEST_TAG_ID);

            // then
            verify(1, deleteRequestedFor(urlPathEqualTo(TAG_PATH)));
        }
    }

    @Test
    void ofOffer_whenOfferId_mapsAssignedTags(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAG_LIST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Tag> tags = allegro.offers().tags().ofOffer(TEST_OFFER_ID);

            // then
            assertEquals(2, tags.size());
            assertEquals(TEST_TAG_ID, tags.get(0).id());
            verify(1, getRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH)));
        }
    }

    @Test
    void assignToOffer_whenTagIds_postsIdsBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlPathEqualTo(OFFER_TAGS_PATH))
                .withRequestBody(equalToJson(ASSIGN_REQUEST_BODY))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.offers().tags().assignToOffer(TEST_OFFER_ID, List.of(TEST_TAG_ID, TEST_TAG_ID_2));

            // then
            verify(1, postRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH)).withRequestBody(equalToJson(ASSIGN_REQUEST_BODY)));
        }
    }

    @Test
    void tagWrites_whenArgumentsNull_throwBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        try (AllegroClient allegro = client(wmInfo)) {
            var tags = allegro.offers().tags();
            TagRequest request = TagRequest.builder().name(TEST_TAG_NAME).build();

            // then — every required argument is fail-fast before the wire
            assertThrows(NullPointerException.class, () -> tags.create(null));
            assertThrows(NullPointerException.class, () -> tags.rename(null, request));
            assertThrows(NullPointerException.class, () -> tags.rename(TEST_TAG_ID, null));
            assertThrows(NullPointerException.class, () -> tags.delete(null));
            assertThrows(NullPointerException.class, () -> tags.ofOffer(null));
            assertThrows(NullPointerException.class, () -> tags.assignToOffer(null, List.of(TEST_TAG_ID)));
            assertThrows(NullPointerException.class, () -> tags.assignToOffer(TEST_OFFER_ID, null));
            verify(0, postRequestedFor(urlPathEqualTo(TAGS_PATH)));
        }
    }

    @Test
    void ofOffer_when400WithErrors_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var tags = allegro.offers().tags();

            // then
            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> tags.ofOffer(TEST_OFFER_ID));
            List<AllegroFieldError> errors = failure.errors();
            assertEquals(1, errors.size());
            assertEquals("ConstraintViolationException", errors.get(0).code());
        }
    }

    @Test
    void ofOffer_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlPathEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAG_LIST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<Tag> tags = allegro.offers().tags().ofOffer(TEST_OFFER_ID);

            // then — replayed once, the replay carried the fresh token
            assertEquals(2, tags.size());
            verify(2, getRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH)));
            verify(1, getRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void ofOffer_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var tags = allegro.offers().tags();

            // then
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> tags.ofOffer(TEST_OFFER_ID));
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void ofOffer_when429WithRetryAfter_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — always 429; one retry then the typed failure
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(RETRY_AFTER_SECONDS))));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {
            var tags = allegro.offers().tags();

            // then
            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> tags.ofOffer(TEST_OFFER_ID));
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH)));
        }
    }

    @Test
    void ofOffer_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first 500, retry returns 200
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(get(urlPathEqualTo(OFFER_TAGS_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TAG_LIST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo, oneRetry())) {

            // when
            List<Tag> tags = allegro.offers().tags().ofOffer(TEST_OFFER_ID);

            // then
            assertEquals(2, tags.size());
            verify(FAST_MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(OFFER_TAGS_PATH)));
        }
    }
}
