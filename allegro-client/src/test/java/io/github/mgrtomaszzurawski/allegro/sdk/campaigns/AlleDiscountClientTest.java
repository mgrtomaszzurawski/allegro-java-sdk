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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.AlleDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.EligibleOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmitOfferRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.builder.SubmittedOffersFilter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaign;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCampaignType;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountCommandStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountEligibleOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountOfferStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmitResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountSubmittedOffer;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.AlleDiscountWithdrawResult;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.campaigns.model.SchedulePolicyType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroAsyncTimeoutException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroFieldError;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock proof of the AlleDiscount sub-facade: the campaign list, the eligible /
 * submitted offer streams (mapping + lazy pagination + filters), the submit and
 * withdraw command state machines (poll to terminal, failure, timeout), and the
 * mandatory error-path table (TESTING.md §1).
 */
@WireMockTest
class AlleDiscountClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;
    private static final int PAGE_SIZE = 100;
    private static final int HTTP_ACCEPTED = 202;

    private static final String CAMPAIGN_ID = "winter-sale";
    private static final String TEST_OFFER_ID = "12345678";
    private static final String TEST_PARTICIPATION_ID = "part-1";
    private static final String TEST_COMMAND_ID = "cmd-1";

    private static final String CAMPAIGNS_PATH = "/sale/alle-discount/campaigns";
    private static final String ELIGIBLE_PATH = "/sale/alle-discount/" + CAMPAIGN_ID + "/eligible-offers";
    private static final String SUBMITTED_PATH = "/sale/alle-discount/" + CAMPAIGN_ID + "/submitted-offers";
    private static final String SUBMIT_PATH = "/sale/alle-discount/submit-offer-commands";
    private static final String WITHDRAW_PATH = "/sale/alle-discount/withdraw-offer-commands";
    private static final String SUBMIT_POLL_PATH = SUBMIT_PATH + "/" + TEST_COMMAND_ID;
    private static final String WITHDRAW_POLL_PATH = WITHDRAW_PATH + "/" + TEST_COMMAND_ID;

    private static final String CAMPAIGNS_FIXTURE = "campaigns/alle-discount-campaigns.json";
    private static final String ELIGIBLE_FIXTURE = "campaigns/alle-discount-eligible.json";
    private static final String SUBMITTED_FIXTURE = "campaigns/alle-discount-submitted.json";

    private static final String TEST_CAMPAIGN_NAME = "Winter Sale";
    private static final String TEST_CURRENCY_PLN = "PLN";
    private static final String TEST_BASE_AMOUNT = "100.00";
    private static final String TEST_REQUIRED_MERCHANT_AMOUNT = "90.00";
    private static final String TEST_MIN_DISCOUNT = "10";
    private static final String TEST_PROPOSED_AMOUNT = "84.99";
    private static final String TEST_SUBMITTED_PROPOSED_AMOUNT = "85.00";
    private static final String TEST_FAIL_CODE = "PRICE_TOO_HIGH";
    private static final String TEST_FAIL_MESSAGE = "Proposed price above the required merchant price.";
    private static final String TEST_CODE_ONLY = "UNKNOWN_ERROR";
    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String TEST_BAD_REQUEST_CODE = "VALIDATION_ERROR";
    private static final String TEST_BAD_REQUEST_PATH = "input.proposedPrice";

    private static final String OFFER_ID_PARAM = "offerId";
    private static final String MEETS_CONDITIONS_PARAM = "meetsConditions";
    private static final String OFFSET_PARAM = "offset";
    private static final String JSON_INPUT_OFFER_ID = "$.input.offer.id";
    private static final String JSON_INPUT_CAMPAIGN_ID = "$.input.campaign.id";
    private static final String JSON_INPUT_PROPOSED_AMOUNT = "$.input.proposedPrice.amount";
    private static final String JSON_INPUT_PARTICIPATION_ID = "$.input.participationId";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String SCENARIO_RECOVER = "recover-5xx";
    private static final String SCENARIO_POLL = "poll-command";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String STATE_RECOVERED = "recovered";
    private static final String STATE_TERMINAL = "terminal";
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final String RETRY_AFTER_VALUE = "1";
    private static final long RETRY_AFTER_SECONDS = 1L;

    private static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
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
            {"id":"%s"}
            """.formatted(TEST_COMMAND_ID);
    private static final String SUBMIT_PREVIEW_TEMPLATE = """
            {"id":"%s","output":{"status":"%s",
             "newOfferParticipation":{"participationId":"%s"},"errors":%s}}
            """;
    private static final String WITHDRAW_PREVIEW = """
            {"id":"%s","output":{"status":"SUCCESSFUL",
             "withdrawnOfferParticipation":{"participationId":"%s"},"errors":[]}}
            """.formatted(TEST_COMMAND_ID, TEST_PARTICIPATION_ID);

    private static String submitPreview(String status, String errorsJson) {
        return SUBMIT_PREVIEW_TEMPLATE.formatted(TEST_COMMAND_ID, status, TEST_PARTICIPATION_ID, errorsJson);
    }

    private static String withdrawPreview(String status, String errorsJson) {
        return "{\"id\":\"" + TEST_COMMAND_ID + "\",\"output\":{\"status\":\"" + status
                + "\",\"withdrawnOfferParticipation\":{\"participationId\":\"" + TEST_PARTICIPATION_ID
                + "\"},\"errors\":" + errorsJson + "}}";
    }

    private static String failureErrors() {
        return "[{\"errors\":[{\"code\":\"" + TEST_FAIL_CODE + "\",\"message\":\"" + TEST_FAIL_MESSAGE + "\"}]}]";
    }

    /** A FAILED command error carrying a code but no message (the {@code List.copyOf} NPE case). */
    private static String codeOnlyErrors() {
        return "[{\"errors\":[{\"code\":\"" + TEST_CODE_ONLY + "\"}]}]";
    }

    private static String fullPageOfEligible(int count) {
        StringBuilder json = new StringBuilder("{\"eligibleOffers\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":\"o").append(index)
                    .append("\",\"alleDiscount\":{\"campaignConditions\":{\"meetsConditions\":true}}}");
        }
        return json.append("],\"count\":").append(count).append("}").toString();
    }

    private static String fullPageOfSubmitted(int count) {
        StringBuilder json = new StringBuilder("{\"submittedOffers\":[");
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"participationId\":\"p").append(index)
                    .append("\",\"offer\":{\"id\":\"o").append(index)
                    .append("\"},\"campaign\":{\"id\":\"").append(CAMPAIGN_ID)
                    .append("\"},\"process\":{\"status\":\"ACTIVE\"}}");
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
    void campaigns_whenAuthenticated_mapsCampaignList(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountCampaign> campaigns = allegro.campaigns().alleDiscount().campaigns();

            // then
            assertEquals(1, campaigns.size());
            AlleDiscountCampaign campaign = campaigns.get(0);
            assertEquals(CAMPAIGN_ID, campaign.id());
            assertEquals(TEST_CAMPAIGN_NAME, campaign.name());
            assertEquals(AlleDiscountCampaignType.DISCOUNT, campaign.type());
            assertEquals(SchedulePolicyType.WITHIN, campaign.publication().type());
            verify(1, getRequestedFor(urlEqualTo(CAMPAIGNS_PATH)));
        }
    }

    @Test
    void streamEligibleOffers_whenConsumed_mapsOfferWithConditionsAndPrices(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ELIGIBLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(ELIGIBLE_FIXTURE)));
        EligibleOffersFilter filter = EligibleOffersFilter.builder(CAMPAIGN_ID).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountEligibleOffer> offers =
                    allegro.campaigns().alleDiscount().streamEligibleOffers(filter).toList();

            // then
            assertEquals(1, offers.size());
            AlleDiscountEligibleOffer offer = offers.get(0);
            assertEquals(TEST_OFFER_ID, offer.offerId());
            assertEquals(Money.of(TEST_BASE_AMOUNT, TEST_CURRENCY_PLN), offer.basePrice());
            assertEquals(Money.of(TEST_REQUIRED_MERCHANT_AMOUNT, TEST_CURRENCY_PLN), offer.requiredMerchantPrice());
            assertEquals(TEST_MIN_DISCOUNT, offer.minimumGuaranteedDiscountPercentage());
            assertTrue(offer.meetsConditions());
        }
    }

    @Test
    void streamEligibleOffers_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ELIGIBLE_PATH))
                .withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfEligible(PAGE_SIZE))));
        EligibleOffersFilter filter = EligibleOffersFilter.builder(CAMPAIGN_ID).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountEligibleOffer> firstOnly =
                    allegro.campaigns().alleDiscount().streamEligibleOffers(filter).limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(ELIGIBLE_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(ELIGIBLE_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void streamEligibleOffers_whenFilterGiven_sendsOfferAndMeetsConditionsQueryParams(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(ELIGIBLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfEligible(1))));
        EligibleOffersFilter filter = EligibleOffersFilter.builder(CAMPAIGN_ID)
                .offerId(TEST_OFFER_ID)
                .meetsConditions(true)
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.campaigns().alleDiscount().streamEligibleOffers(filter).toList();

            // then
            verify(getRequestedFor(urlPathEqualTo(ELIGIBLE_PATH))
                    .withQueryParam(OFFER_ID_PARAM, equalTo(TEST_OFFER_ID))
                    .withQueryParam(MEETS_CONDITIONS_PARAM, equalTo("true")));
        }
    }

    @Test
    void streamEligibleOffers_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub
        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(IllegalArgumentException.class, () -> alleDiscount.streamEligibleOffers(null));
            verify(0, getRequestedFor(urlPathEqualTo(ELIGIBLE_PATH)));
        }
    }

    @Test
    void streamSubmittedOffers_whenConsumed_mapsParticipation(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUBMITTED_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(SUBMITTED_FIXTURE)));
        SubmittedOffersFilter filter = SubmittedOffersFilter.builder(CAMPAIGN_ID).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountSubmittedOffer> offers =
                    allegro.campaigns().alleDiscount().streamSubmittedOffers(filter).toList();

            // then
            assertEquals(1, offers.size());
            AlleDiscountSubmittedOffer offer = offers.get(0);
            assertEquals(TEST_PARTICIPATION_ID, offer.participationId());
            assertEquals(TEST_OFFER_ID, offer.offerId());
            assertEquals(AlleDiscountOfferStatus.ACTIVE, offer.status());
            assertEquals(Money.of(TEST_SUBMITTED_PROPOSED_AMOUNT, TEST_CURRENCY_PLN), offer.proposedPrice());
        }
    }

    @Test
    void streamSubmittedOffers_whenConsumingFirstElement_doesNotFetchSecondPage(WireMockRuntimeInfo wmInfo) {
        // given — a full first page implies there may be more
        stubToken(TEST_TOKEN);
        stubFor(get(urlPathEqualTo(SUBMITTED_PATH))
                .withQueryParam(OFFSET_PARAM, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(fullPageOfSubmitted(PAGE_SIZE))));
        SubmittedOffersFilter filter = SubmittedOffersFilter.builder(CAMPAIGN_ID).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountSubmittedOffer> firstOnly =
                    allegro.campaigns().alleDiscount().streamSubmittedOffers(filter).limit(1).toList();

            // then — page one fetched, page two (offset=100) never requested
            assertEquals(1, firstOnly.size());
            verify(1, getRequestedFor(urlPathEqualTo(SUBMITTED_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo("0")));
            verify(0, getRequestedFor(urlPathEqualTo(SUBMITTED_PATH))
                    .withQueryParam(OFFSET_PARAM, equalTo(String.valueOf(PAGE_SIZE))));
        }
    }

    @Test
    void submitOffer_whenSuccessful_postsBodyThenPollsToTerminal(WireMockRuntimeInfo wmInfo) {
        // given — POST accepted, the command is SUCCESSFUL on first poll
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_SUCCESSFUL, ERRORS_EMPTY))));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID)
                .offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AlleDiscountSubmitResult result = allegro.campaigns().alleDiscount().submitOffer(request);

            // then — the input body was sent and the command polled once to terminal
            assertEquals(AlleDiscountCommandStatus.SUCCESSFUL, result.status());
            assertEquals(TEST_PARTICIPATION_ID, result.participationId());
            verify(1, postRequestedFor(urlEqualTo(SUBMIT_PATH))
                    .withRequestBody(matchingJsonPath(JSON_INPUT_OFFER_ID, equalTo(TEST_OFFER_ID)))
                    .withRequestBody(matchingJsonPath(JSON_INPUT_CAMPAIGN_ID, equalTo(CAMPAIGN_ID)))
                    .withRequestBody(matchingJsonPath(JSON_INPUT_PROPOSED_AMOUNT, equalTo(TEST_PROPOSED_AMOUNT))));
            verify(1, getRequestedFor(urlEqualTo(SUBMIT_POLL_PATH)));
        }
    }

    @Test
    void submitOffer_whenInProgressThenSuccessful_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — first poll IN_PROGRESS, second poll SUCCESSFUL
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_IN_PROGRESS, ERRORS_EMPTY)))
                .willSetStateTo(STATE_TERMINAL));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .inScenario(SCENARIO_POLL).whenScenarioStateIs(STATE_TERMINAL)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_SUCCESSFUL, ERRORS_EMPTY))));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN)).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AlleDiscountSubmitResult result = allegro.campaigns().alleDiscount().submitOffer(request);

            // then — polled twice, until the command left IN_PROGRESS
            assertEquals(AlleDiscountCommandStatus.SUCCESSFUL, result.status());
            verify(2, getRequestedFor(urlEqualTo(SUBMIT_POLL_PATH)));
        }
    }

    @Test
    void submitOffer_whenFailed_reportsFailureWithErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_FAILED, failureErrors()))));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN)).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AlleDiscountSubmitResult result = allegro.campaigns().alleDiscount().submitOffer(request);

            // then — FAILED is terminal; the nested error code and message map through
            assertEquals(AlleDiscountCommandStatus.FAILED, result.status());
            assertEquals(1, result.errors().size());
            assertEquals(TEST_FAIL_CODE, result.errors().get(0).code());
            assertEquals(TEST_FAIL_MESSAGE, result.errors().get(0).message());
        }
    }

    @Test
    void submitOffer_whenFailedWithCodeOnlyError_mapsViolationWithoutNpe(WireMockRuntimeInfo wmInfo) {
        // given — a FAILED command whose error carries a code but no message
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_FAILED, codeOnlyErrors()))));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN)).build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when — a null error message must not crash the mapping
            AlleDiscountSubmitResult result = allegro.campaigns().alleDiscount().submitOffer(request);

            // then — the coded violation surfaces with a null message
            assertEquals(TEST_CODE_ONLY, result.errors().get(0).code());
            assertNull(result.errors().get(0).message());
        }
    }

    @Test
    void submitOffer_whenNeverTerminalWithinTimeout_throwsAsyncTimeout(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(SUBMIT_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(submitPreview(STATUS_IN_PROGRESS, ERRORS_EMPTY))));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN)).build();

        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(AllegroAsyncTimeoutException.class,
                    () -> alleDiscount.submitOffer(request, Duration.ZERO));
        }
    }

    @Test
    void submitOffer_when5xx_throwsServerErrorAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given — persistent 500 on the POST; writes are not retried by default
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        SubmitOfferRequest request = SubmitOfferRequest.builder()
                .campaignId(CAMPAIGN_ID).offerId(TEST_OFFER_ID)
                .proposedPrice(Money.of(TEST_PROPOSED_AMOUNT, TEST_CURRENCY_PLN)).build();

        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(AllegroServerException.class, () -> alleDiscount.submitOffer(request));
            verify(1, postRequestedFor(urlEqualTo(SUBMIT_PATH)));
        }
    }

    @Test
    void submitOffer_whenNull_throwsIllegalArgument(WireMockRuntimeInfo wmInfo) {
        // given — no stub
        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(IllegalArgumentException.class, () -> alleDiscount.submitOffer(null));
            verify(0, postRequestedFor(urlEqualTo(SUBMIT_PATH)));
        }
    }

    @Test
    void withdrawOffer_whenSuccessful_postsParticipationThenPollsToTerminal(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(WITHDRAW_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(WITHDRAW_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(WITHDRAW_PREVIEW)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AlleDiscountWithdrawResult result =
                    allegro.campaigns().alleDiscount().withdrawOffer(TEST_PARTICIPATION_ID);

            // then — the participation id travelled in the body and the command polled to terminal
            assertEquals(AlleDiscountCommandStatus.SUCCESSFUL, result.status());
            assertEquals(TEST_PARTICIPATION_ID, result.participationId());
            verify(1, postRequestedFor(urlEqualTo(WITHDRAW_PATH))
                    .withRequestBody(matchingJsonPath(JSON_INPUT_PARTICIPATION_ID, equalTo(TEST_PARTICIPATION_ID))));
            verify(1, getRequestedFor(urlEqualTo(WITHDRAW_POLL_PATH)));
        }
    }

    @Test
    void withdrawOffer_whenFailed_reportsFailureWithErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(WITHDRAW_PATH))
                .willReturn(aResponse().withStatus(HTTP_ACCEPTED).withBody(ACCEPTED_RESPONSE)));
        stubFor(get(urlEqualTo(WITHDRAW_POLL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(withdrawPreview(STATUS_FAILED, failureErrors()))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            AlleDiscountWithdrawResult result =
                    allegro.campaigns().alleDiscount().withdrawOffer(TEST_PARTICIPATION_ID);

            // then — FAILED is terminal; the coded error maps through
            assertEquals(AlleDiscountCommandStatus.FAILED, result.status());
            assertEquals(TEST_FAIL_CODE, result.errors().get(0).code());
        }
    }

    @Test
    void withdrawOffer_whenParticipationBlank_throwsIllegalArgumentBeforeAnyRequest(WireMockRuntimeInfo wmInfo) {
        // given — no stub: the guard must reject before any call
        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(IllegalArgumentException.class, () -> alleDiscount.withdrawOffer("  "));
            verify(0, postRequestedFor(urlEqualTo(WITHDRAW_PATH)));
        }
    }

    @Test
    void campaigns_when400WithFieldErrors_throwsBadRequestWithParsedErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, alleDiscount::campaigns);
            AllegroFieldError fieldError = failure.errors().get(0);
            assertEquals(TEST_BAD_REQUEST_CODE, fieldError.code());
            assertEquals(TEST_BAD_REQUEST_PATH, fieldError.path());
        }
    }

    @Test
    void campaigns_when401Once_reauthenticatesAndReplays(WireMockRuntimeInfo wmInfo) {
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
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountCampaign> campaigns = allegro.campaigns().alleDiscount().campaigns();

            // then — replayed once with the fresh token
            assertEquals(1, campaigns.size());
            verify(2, getRequestedFor(urlEqualTo(CAMPAIGNS_PATH)));
        }
    }

    @Test
    void campaigns_when404_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            assertThrows(AllegroNotFoundException.class, alleDiscount::campaigns);
            verify(1, getRequestedFor(urlEqualTo(CAMPAIGNS_PATH)));
        }
    }

    @Test
    void campaigns_when429Persists_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_VALUE)));
        RetryPolicy retryTwice = RetryPolicy.builder().maxAttempts(RETRY_MAX_ATTEMPTS).build();

        try (AllegroClient allegro = client(wmInfo, retryTwice)) {
            AlleDiscount alleDiscount = allegro.campaigns().alleDiscount();

            // then
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, alleDiscount::campaigns);
            assertEquals(RETRY_AFTER_SECONDS, failure.retryAfterSeconds());
            verify(RETRY_MAX_ATTEMPTS, getRequestedFor(urlEqualTo(CAMPAIGNS_PATH)));
        }
    }

    @Test
    void campaigns_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — GETs are retried by default
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(CAMPAIGNS_PATH))
                .inScenario(SCENARIO_RECOVER).whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBodyFile(CAMPAIGNS_FIXTURE)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<AlleDiscountCampaign> campaigns = allegro.campaigns().alleDiscount().campaigns();

            // then
            assertEquals(1, campaigns.size());
            verify(2, getRequestedFor(urlEqualTo(CAMPAIGNS_PATH)));
        }
    }
}
