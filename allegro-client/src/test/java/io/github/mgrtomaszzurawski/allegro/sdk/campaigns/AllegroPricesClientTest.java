/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.campaigns;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AllegroPrices;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.AllegroPricesOfferQuery;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ExcludeOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.OfferScope;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.ParticipationUpdate;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOffersRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AllegroPricesParticipation;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.ParticipationStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SubsidyCommandReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SubsidyOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the Allegro Prices sub-facade: participation read/update,
 * the offer-status query mapped from raw JSON (the {@code oneOf} price-reduction
 * workaround), lazy pagination in the POST body, the submit/exclude command
 * state machines (poll to terminal, failure, timeout), and the mandatory
 * error-path table (TESTING.md §1).
 */
@WireMockTest
class AllegroPricesClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;
    private static final int HTTP_ACCEPTED = 202;

    private static final String PARTICIPATIONS_PATH = "/sale/allegro-prices/accounts/participations";
    private static final String OFFERS_QUERIES_PATH = "/sale/allegro-prices/offers-queries";
    private static final String SUBMIT_PATH = "/sale/allegro-prices/offers/submit-offer-commands";
    private static final String EXCLUSION_PATH = "/sale/allegro-prices/offers/exclusion-commands";
    private static final String TEST_COMMAND_ID = "cmd-1";
    private static final String SUBMIT_POLL_PATH = SUBMIT_PATH + "/" + TEST_COMMAND_ID;
    private static final String EXCLUSION_POLL_PATH = EXCLUSION_PATH + "/" + TEST_COMMAND_ID;

    private static final String PARTICIPATION_FIXTURE = "campaigns/allegro-prices-participation.json";
    private static final String OFFERS_FIXTURE = "campaigns/allegro-prices-offers.json";
    private static final String MARKETPLACE_PL = "allegro-pl";
    private static final String MARKETPLACE_CZ = "allegro-cz";
    private static final String TEST_OFFER_ID = "12345678";
    private static final String TEST_CURRENCY_PLN = "PLN";
    private static final String TEST_BASE_AMOUNT = "100.00";
    private static final String TEST_FINAL_AMOUNT = "92.00";
    private static final String TEST_RECOMMENDED_PCT = "10";
    private static final String TEST_DECLARED_PCT = "8";
    private static final String TEST_MAX_CONTRIBUTION = "5";
    private static final String TEST_DISCOUNTED_AT = "2026-07-10T10:00:00Z";
    private static final String TEST_COMPLETED_AT = "2026-07-16T10:00:05Z";
    private static final String TEST_FAIL_MESSAGE = "Offer not eligible for Allegro Prices.";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String TEST_BAD_REQUEST_CODE = "VALIDATION_ERROR";
    private static final String TEST_BAD_REQUEST_PATH = "marketplaces";

    private static final String JSON_OFFSET = "$.offset";
    private static final String JSON_MARKETPLACE_ID = "$.marketplace.id";
    private static final String JSON_SCOPE = "$.offer.scope";
    private static final String JSON_MAX_CONTRIBUTION = "$.offers[0].sellerDiscountDeclaration.maxContributionPercentage";
    private static final String JSON_PARTICIPATION_STATUS = "$.marketplaces[0].status";
    private static final String WIRE_STATUS_ALLOWED = "ALLOWED";
    private static final String WIRE_SCOPE_DISCOUNTED = "DISCOUNTED";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String SCENARIO_POLL = "poll-command";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String STATE_RECOVERED = "recovered";
    private static final String STATE_TERMINAL = "terminal";
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final String RETRY_AFTER_VALUE = "1";
    private static final long RETRY_AFTER_SECONDS = 1L;

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String ERRORS_EMPTY = "[]";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"Invalid","userMessage":"Nieprawidłowe",
              "path":"%s","details":null}]}
            """.formatted(TEST_BAD_REQUEST_CODE, TEST_BAD_REQUEST_PATH);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Not found","userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String ACCEPTED_RESPONSE = """
            {"commandId":"%s","status":"IN_PROGRESS","createdAt":"2026-07-16T10:00:00Z"}
            """.formatted(TEST_COMMAND_ID);
    private static final String PREVIEW_TEMPLATE = """
            {"commandId":"%s","createdAt":"2026-07-16T10:00:00Z",
             "offers":[{"id":"%s","marketplace":{"id":"%s"},"status":"%s",
               "completedAt":"%s","errors":%s}]}
            """;

    private static String preview(String status, String errorsJson) {
        return PREVIEW_TEMPLATE.formatted(
                TEST_COMMAND_ID, TEST_OFFER_ID, MARKETPLACE_PL, status, TEST_COMPLETED_AT, errorsJson);
    }

    private static String failureErrors() {
        return "[{\"message\":\"" + TEST_FAIL_MESSAGE + "\"}]";
    }

    private static String fullPageOfOfferStatuses(int count) {
        StringBuilder json = new StringBuilder("{\"offers\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"o").append(index)
                    .append("\",\"name\":\"n\",\"marketplace\":{\"id\":\"").append(MARKETPLACE_PL)
                    .append("\"},\"discount\":{\"opportunity\":false}}");
        }
        return json.append("],\"count\":").append(count).append("}").toString();
    }

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

    @Test
    void participation_whenAuthenticated_mapsMarketplaces(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PARTICIPATION_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AllegroPricesParticipation participation = allegro.campaigns().allegroPrices().participation();

            // then
            assertEquals(2, participation.marketplaces().size());
            assertEquals(MARKETPLACE_PL, participation.marketplaces().get(0).marketplaceId());
            assertEquals(ParticipationStatus.ALLOWED, participation.marketplaces().get(0).status());
            assertEquals(ParticipationStatus.DENIED, participation.marketplaces().get(1).status());
            verify(1, getRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }

    @Test
    void updateParticipation_whenAllowAndDeny_patchesBodyAndMaps(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(patch(urlEqualTo(PARTICIPATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PARTICIPATION_FIXTURE)));
        ParticipationUpdate update = ParticipationUpdate.builder()
                .allow(MARKETPLACE_PL)
                .deny(MARKETPLACE_CZ)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AllegroPricesParticipation result = allegro.campaigns().allegroPrices().updateParticipation(update);

            // then — the allow/deny statuses travelled in the body and the response mapped
            assertEquals(2, result.marketplaces().size());
            verify(1, patchRequestedFor(urlEqualTo(PARTICIPATIONS_PATH))
                    .withRequestBody(matchingJsonPath(JSON_PARTICIPATION_STATUS, equalTo(WIRE_STATUS_ALLOWED))));
        }
    }

    @Test
    void updateParticipation_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub: the guard must reject before any call
        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(IllegalArgumentException.class, () -> allegroPrices.updateParticipation(null));
            verify(0, patchRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }

    @Test
    void streamOffersStatus_whenConsumed_mapsOfferWithOneOfReductions(WireMockRuntimeInfo wmInfo) {
        // given — the fixture carries populated oneOf price-reduction objects
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(OFFERS_QUERIES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(OFFERS_FIXTURE)));
        AllegroPricesOfferQuery query = AllegroPricesOfferQuery.builder(MARKETPLACE_PL).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AllegroPricesOfferStatus> statuses =
                    allegro.campaigns().allegroPrices().streamOffersStatus(query).toList();

            // then — the raw-JSON mapping reads through the oneOf reductions
            assertEquals(1, statuses.size());
            AllegroPricesOfferStatus status = statuses.get(0);
            assertEquals(TEST_OFFER_ID, status.offerId());
            assertEquals(Money.of(TEST_BASE_AMOUNT, TEST_CURRENCY_PLN), status.basePrice());
            assertEquals(TEST_RECOMMENDED_PCT, status.recommendedReductionPercentage());
            assertEquals(TEST_DECLARED_PCT, status.declaredReductionPercentage());
            assertEquals(TEST_DECLARED_PCT, status.actualReductionPercentage());
            assertEquals(Money.of(TEST_FINAL_AMOUNT, TEST_CURRENCY_PLN), status.finalBuyerPrice());
            assertEquals(OffsetDateTime.parse(TEST_DISCOUNTED_AT), status.discountedAt());
            assertNull(status.excludedAt());
        }
    }

    @Test
    void streamOffersStatus_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(OFFERS_QUERIES_PATH))
                .withRequestBody(matchingJsonPath(JSON_OFFSET, equalTo("0")))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfOfferStatuses(PAGE_SIZE))));
        AllegroPricesOfferQuery query = AllegroPricesOfferQuery.builder(MARKETPLACE_PL).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when — only the first element is consumed
            List<AllegroPricesOfferStatus> firstOnly =
                    allegro.campaigns().allegroPrices().streamOffersStatus(query).limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, postRequestedFor(urlEqualTo(OFFERS_QUERIES_PATH))
                    .withRequestBody(matchingJsonPath(JSON_OFFSET, equalTo("0"))));
            verify(0, postRequestedFor(urlEqualTo(OFFERS_QUERIES_PATH))
                    .withRequestBody(matchingJsonPath(JSON_OFFSET, equalTo(String.valueOf(PAGE_SIZE)))));
        }
    }

    @Test
    void streamOffersStatus_whenFilterGiven_sendsMarketplaceAndScopeInBody(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(OFFERS_QUERIES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfOfferStatuses(1))));
        AllegroPricesOfferQuery query = AllegroPricesOfferQuery.builder(MARKETPLACE_PL)
                .scope(OfferScope.DISCOUNTED)
                .addOfferId(TEST_OFFER_ID)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.campaigns().allegroPrices().streamOffersStatus(query).toList();

            // then — the marketplace maps to its enum id and the scope travels in the body
            verify(postRequestedFor(urlEqualTo(OFFERS_QUERIES_PATH))
                    .withRequestBody(matchingJsonPath(JSON_MARKETPLACE_ID, equalTo(MARKETPLACE_PL)))
                    .withRequestBody(matchingJsonPath(JSON_SCOPE, equalTo(WIRE_SCOPE_DISCOUNTED))));
        }
    }

    @Test
    void streamOffersStatus_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub
        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(IllegalArgumentException.class, () -> allegroPrices.streamOffersStatus(null));
            verify(0, postRequestedFor(urlEqualTo(OFFERS_QUERIES_PATH)));
        }
    }

    @Test
    void submitOffers_whenAllSuccess_pollsToTerminalReport(WireMockRuntimeInfo wmInfo) {
        // given — POST accepted (IN_PROGRESS), the command is SUCCESS on first poll
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_SUCCESS, ERRORS_EMPTY))));
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL, TEST_MAX_CONTRIBUTION)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SubsidyCommandReport report = allegro.campaigns().allegroPrices().submitOffers(request);

            // then — the contribution travelled and the command polled once to a terminal report
            assertEquals(TEST_COMMAND_ID, report.commandId());
            assertEquals(1, report.offers().size());
            assertEquals(SubsidyOfferStatus.SUCCESS, report.offers().get(0).status());
            verify(1, postRequestedFor(urlEqualTo(SUBMIT_PATH))
                    .withRequestBody(matchingJsonPath(JSON_MAX_CONTRIBUTION, equalTo(TEST_MAX_CONTRIBUTION))));
            verify(1, getRequestedFor(urlEqualTo(SUBMIT_POLL_PATH)));
        }
    }

    @Test
    void submitOffers_whenInProgressThenSuccess_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — first poll IN_PROGRESS, second poll SUCCESS
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_IN_PROGRESS, ERRORS_EMPTY)))
                .willSetStateTo(STATE_TERMINAL));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(STATE_TERMINAL)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_SUCCESS, ERRORS_EMPTY))));
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SubsidyCommandReport report = allegro.campaigns().allegroPrices().submitOffers(request);

            // then — polled twice, until every offer left IN_PROGRESS
            assertEquals(SubsidyOfferStatus.SUCCESS, report.offers().get(0).status());
            verify(2, getRequestedFor(urlEqualTo(SUBMIT_POLL_PATH)));
        }
    }

    @Test
    void submitOffers_whenFailed_reportsFailureWithErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_FAILED, failureErrors()))));
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SubsidyCommandReport report = allegro.campaigns().allegroPrices().submitOffers(request);

            // then — FAILED is terminal and the failure message maps through
            assertEquals(SubsidyOfferStatus.FAILED, report.offers().get(0).status());
            assertEquals(1, report.offers().get(0).errors().size());
            assertEquals(TEST_FAIL_MESSAGE, report.offers().get(0).errors().get(0));
        }
    }

    @Test
    void submitOffers_whenNeverTerminalWithinTimeout_throwsAsyncTimeout(WireMockRuntimeInfo wmInfo) {
        // given — the command stays IN_PROGRESS and the caller passes a zero budget
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_IN_PROGRESS, ERRORS_EMPTY))));
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(AllegroAsyncTimeoutException.class,
                    () -> allegroPrices.submitOffers(request, Duration.ZERO));
        }
    }

    @Test
    void submitOffers_when5xx_throwsServerErrorAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given — persistent 500 on the POST; writes are not retried by default
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        SubmitOffersRequest request = SubmitOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(AllegroServerException.class, () -> allegroPrices.submitOffers(request));
            verify(1, postRequestedFor(urlEqualTo(SUBMIT_PATH)));
        }
    }

    @Test
    void submitOffers_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub
        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(IllegalArgumentException.class, () -> allegroPrices.submitOffers(null));
            verify(0, postRequestedFor(urlEqualTo(SUBMIT_PATH)));
        }
    }

    @Test
    void excludeOffers_whenSuccess_pollsToTerminalReport(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(EXCLUSION_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(EXCLUSION_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_SUCCESS, ERRORS_EMPTY))));
        ExcludeOffersRequest request = ExcludeOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SubsidyCommandReport report = allegro.campaigns().allegroPrices().excludeOffers(request);

            // then
            assertEquals(SubsidyOfferStatus.SUCCESS, report.offers().get(0).status());
            verify(1, postRequestedFor(urlEqualTo(EXCLUSION_PATH)));
            verify(1, getRequestedFor(urlEqualTo(EXCLUSION_POLL_PATH)));
        }
    }

    @Test
    void excludeOffers_whenInProgressThenSuccess_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — first poll IN_PROGRESS, second poll SUCCESS
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(EXCLUSION_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(EXCLUSION_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_IN_PROGRESS, ERRORS_EMPTY)))
                .willSetStateTo(STATE_TERMINAL));
        stubFor(get(urlEqualTo(EXCLUSION_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(STATE_TERMINAL)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(preview(STATUS_SUCCESS, ERRORS_EMPTY))));
        ExcludeOffersRequest request = ExcludeOffersRequest.builder()
                .addOffer(TEST_OFFER_ID, MARKETPLACE_PL)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            SubsidyCommandReport report = allegro.campaigns().allegroPrices().excludeOffers(request);

            // then — polled twice, until every offer left IN_PROGRESS
            assertEquals(SubsidyOfferStatus.SUCCESS, report.offers().get(0).status());
            verify(2, getRequestedFor(urlEqualTo(EXCLUSION_POLL_PATH)));
        }
    }

    @Test
    void excludeOffers_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub
        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(IllegalArgumentException.class, () -> allegroPrices.excludeOffers(null));
            verify(0, postRequestedFor(urlEqualTo(EXCLUSION_PATH)));
        }
    }

    @Test
    void participation_when400WithFieldErrors_throwsBadRequestWithParsedErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, allegroPrices::participation);
            assertEquals(1, failure.errors().size());
            AllegroFieldError fieldError = failure.errors().get(0);
            assertEquals(TEST_BAD_REQUEST_CODE, fieldError.code());
            assertEquals(TEST_BAD_REQUEST_PATH, fieldError.path());
        }
    }

    @Test
    void participation_when401Once_reauthenticatesAndReplays(WireMockRuntimeInfo wmInfo) {
        // given — first call 401, then 200 after re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PARTICIPATION_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AllegroPricesParticipation participation = allegro.campaigns().allegroPrices().participation();

            // then — replayed once with the fresh token
            assertEquals(2, participation.marketplaces().size());
            verify(2, getRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }

    @Test
    void participation_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            assertThrows(AllegroNotFoundException.class, allegroPrices::participation);
            verify(1, getRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }

    @Test
    void participation_when429Persists_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — persistent 429 with a short Retry-After
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_VALUE)));
        RetryPolicy retryTwice = RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).build();

        try (AllegroClient allegro = client(wmInfo, retryTwice)) {
            AllegroPrices allegroPrices = allegro.campaigns().allegroPrices();

            // then
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, allegroPrices::participation);
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }

    @Test
    void participation_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — GETs are retried by default
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(PARTICIPATIONS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBodyFile(PARTICIPATION_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AllegroPricesParticipation participation = allegro.campaigns().allegroPrices().participation();

            // then
            assertEquals(2, participation.marketplaces().size());
            verify(2, getRequestedFor(urlEqualTo(PARTICIPATIONS_PATH)));
        }
    }
}
