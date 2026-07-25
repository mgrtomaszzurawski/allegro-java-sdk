/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.client.model.PromoOptionsModificationRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.BatchPromoOptionsRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.builder.PromoModificationTiming;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.BatchReport;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.PromoOptionsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.command.CommandPoller;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

@WireMockTest
class PromoOptionsWriteClientTest {

    private static final String TEST_TOKEN = "promo-test-token";
    private static final String FOR_ALL_PATH = "/sale/offers/promo-options";
    private static final String OFFER_ID = "13579";
    private static final String MODIFY_PATH = "/sale/offers/" + OFFER_ID + "/promo-options-modification";
    private static final int PAGE_SIZE = 100;
    private static final String OFFSET_QUERY = "offset";
    private static final String PACKAGE_ID = "pkg-base-1";
    private static final String TWO_OFFERS = "{\"promoOptions\":[{\"offerId\":\"o1\"},{\"offerId\":\"o2\"}],"
            + "\"count\":2,\"totalCount\":2}";
    private static final String MOD_TYPE_PATH = "$.modifications[0].modificationType";
    private static final String PKG_TYPE_PATH = "$.modifications[0].packageType";
    private static final String PKG_ID_PATH = "$.modifications[0].packageId";

    private static final String UUID_PATTERN = "[0-9a-fA-F-]{36}";
    private static final String CMD_PATH = "/sale/offers/promo-options-commands/" + UUID_PATTERN;
    private static final String CMD_TASKS_PATH = CMD_PATH + "/tasks";
    private static final String BASE_PACKAGE_ID = "emphasized1d";
    private static final String EXTRA_PACKAGE_ID = "bold30d";
    private static final String OFFER_ONE = "111";
    private static final String OFFER_TWO = "222";
    private static final String BASE_PKG_JSON_PATH = "$.modification.basePackage.id";
    private static final String EXTRA_PKG_JSON_PATH = "$.modification.extraPackages[0].id";
    private static final String TIMING_JSON_PATH = "$.modification.modificationTime";
    private static final String CRITERIA_TYPE_JSON_PATH = "$.offerCriteria[0].type";
    private static final String CRITERIA_OFFER_JSON_PATH = "$.offerCriteria[0].offers[0].id";
    private static final String PENDING_PROMO_REPORT =
            "{\"id\":\"cmd-1\",\"taskCount\":{\"total\":2,\"success\":0,\"failed\":0}}";
    private static final String COMPLETED_PROMO_REPORT =
            "{\"id\":\"cmd-1\",\"taskCount\":{\"total\":2,\"success\":1,\"failed\":1}}";
    private static final String PROMO_TASKS =
            "{\"tasks\":[{\"offer\":{\"id\":\"" + OFFER_ONE + "\"},\"status\":\"DONE\"},"
                    + "{\"offer\":{\"id\":\"" + OFFER_TWO + "\"},\"status\":\"ERROR\","
                    + "\"errors\":[{\"message\":\"package unavailable\"}]}]}";
    private static final String BAD_REQUEST_BODY =
            "{\"errors\":[{\"code\":\"INVALID\",\"message\":\"unknown offer\",\"path\":\"offerCriteria\"}]}";
    private static final String POLL_SCENARIO = "promo-poll";
    private static final String STATE_COMPLETED = "completed";

    private static HttpRuntime httpRuntime(WireMockRuntimeInfo wmInfo) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RetryHandler retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        return new HttpRuntime() {
            @Override public String baseUrl() {
                return wmInfo.getHttpBaseUrl();
            }

            @Override public RetryHandler retryHandler() {
                return retryHandler;
            }

            @Override public AllegroExecutionInterceptor executionInterceptor() {
                return AllegroExecutionInterceptor.noop();
            }

            @Override public ObjectMapper objectMapper() {
                return mapper;
            }

            @Override public Duration readTimeout() {
                return Duration.ofSeconds(5);
            }

            @Override public String requireToken() {
                return TEST_TOKEN;
            }

            @Override public void reauthenticate() {
                // no-op
            }
        };
    }

    private static PromoOptionsImpl promo(WireMockRuntimeInfo wmInfo) {
        return new PromoOptionsImpl(httpRuntime(wmInfo));
    }

    private static PromoOptionsImpl promoBatch(WireMockRuntimeInfo wmInfo) {
        // A no-op sleeper keeps the poll loop instant; the nanoClock still advances.
        CommandPoller fastPoller = new CommandPoller(
                millis -> { }, System::nanoTime, 1L, 1L, Duration.ofSeconds(30));
        return new PromoOptionsImpl(httpRuntime(wmInfo), fastPoller);
    }

    @Test
    void forAllOffers_streamsPerOfferPackages(WireMockRuntimeInfo wmInfo) {
        // given — a page smaller than PAGE_SIZE ends the stream
        stubFor(get(urlPathEqualTo(FOR_ALL_PATH)).withQueryParam(OFFSET_QUERY, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(TWO_OFFERS)));

        // when
        List<OfferPromoOptions> all = promo(wmInfo).forAllOffers().toList();

        // then
        assertEquals(2, all.size());
        assertEquals("o1", all.get(0).offerId());
        assertEquals("o2", all.get(1).offerId());
    }

    @Test
    void forAllOffers_isLazy_doesNotFetchPageTwoUntilPageOneConsumed(WireMockRuntimeInfo wmInfo) {
        // given — a full first page (so hasMore) and a second page
        String fullPage = "{\"promoOptions\":[" + IntStream.range(0, PAGE_SIZE)
                .mapToObj(index -> "{\"offerId\":\"o" + index + "\"}")
                .collect(Collectors.joining(",")) + "]}";
        stubFor(get(urlPathEqualTo(FOR_ALL_PATH)).withQueryParam(OFFSET_QUERY, equalTo("0"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(fullPage)));

        // when — consume only the first page
        List<OfferPromoOptions> firstPage = promo(wmInfo).forAllOffers().limit(PAGE_SIZE).toList();

        // then — page two (offset=PAGE_SIZE) is never fetched
        assertEquals(PAGE_SIZE, firstPage.size());
        verify(0, getRequestedFor(urlPathEqualTo(FOR_ALL_PATH))
                .withQueryParam(OFFSET_QUERY, equalTo(String.valueOf(PAGE_SIZE))));
    }

    @Test
    void modify_postsModificationsWithTypeAndPackage(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(post(urlPathEqualTo(MODIFY_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));

        // when
        promo(wmInfo).modify(OFFER_ID,
                List.of(PromoOptionModification.change(PromoPackageType.BASE, PACKAGE_ID)));

        // then — the change goes on the wire as {modificationType, packageType, packageId}
        verify(1, postRequestedFor(urlPathEqualTo(MODIFY_PATH))
                .withRequestBody(matchingJsonPath(MOD_TYPE_PATH, equalTo("CHANGE")))
                .withRequestBody(matchingJsonPath(PKG_TYPE_PATH, equalTo("BASE")))
                .withRequestBody(matchingJsonPath(PKG_ID_PATH, equalTo(PACKAGE_ID))));
    }

    @Test
    void modify_whenNoChanges_throws(WireMockRuntimeInfo wmInfo) {
        // then — an empty change list would post an empty modifications[]; reject it fail-fast
        assertThrows(IllegalArgumentException.class, () -> promo(wmInfo).modify(OFFER_ID, List.of()));
    }

    @Test
    void modifyBatch_whenCommandCompletes_putsCommandAndReturnsReport(WireMockRuntimeInfo wmInfo) {
        // given — a command already complete when first polled
        stubFor(put(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_PROMO_REPORT)));
        stubFor(get(urlPathMatching(CMD_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PROMO_TASKS)));

        // when — set a base + extra package, timed to the end of cycle, on two offers
        BatchReport report = promoBatch(wmInfo).modifyBatch(
                BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE, OFFER_TWO))
                        .basePackage(BASE_PACKAGE_ID)
                        .addExtraPackage(EXTRA_PACKAGE_ID)
                        .timing(PromoModificationTiming.END_OF_CYCLE)
                        .build());

        // then — the PUT carries the modification and the offers as a CONTAINS_OFFERS criterion
        verify(1, putRequestedFor(urlPathMatching(CMD_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath(BASE_PKG_JSON_PATH, equalTo(BASE_PACKAGE_ID)))
                .withRequestBody(matchingJsonPath(EXTRA_PKG_JSON_PATH, equalTo(EXTRA_PACKAGE_ID)))
                .withRequestBody(matchingJsonPath(TIMING_JSON_PATH, equalTo("END_OF_CYCLE")))
                .withRequestBody(matchingJsonPath(CRITERIA_TYPE_JSON_PATH, equalTo("CONTAINS_OFFERS")))
                .withRequestBody(matchingJsonPath(CRITERIA_OFFER_JSON_PATH, equalTo(OFFER_ONE))));
        // and the terminal report carries the counts and per-offer tasks
        assertEquals(2, report.total());
        assertEquals(1, report.success());
        assertEquals(1, report.failed());
        assertEquals(2, report.tasks().size());
        assertEquals(OFFER_ONE, report.tasks().get(0).offerId());
        assertEquals("DONE", report.tasks().get(0).status());
        assertEquals("package unavailable", report.tasks().get(1).message());
    }

    @Test
    void modifyBatch_whenCommandPending_pollsUntilTerminal(WireMockRuntimeInfo wmInfo) {
        // given — the first status poll shows unfinished tasks, the second is complete
        stubFor(put(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(CMD_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PENDING_PROMO_REPORT))
                .willSetStateTo(STATE_COMPLETED));
        stubFor(get(urlPathMatching(CMD_PATH)).inScenario(POLL_SCENARIO)
                .whenScenarioStateIs(STATE_COMPLETED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_PROMO_REPORT)));
        stubFor(get(urlPathMatching(CMD_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PROMO_TASKS)));

        // when
        BatchReport report = promoBatch(wmInfo).modifyBatch(
                BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE)).basePackage(BASE_PACKAGE_ID).build());

        // then — the command was polled twice (taskCount is the completion signal, not completedAt)
        assertEquals(2, report.total());
        verify(2, getRequestedFor(urlPathMatching(CMD_PATH)));
    }

    @Test
    void modifyBatch_whenExtraPackagesUnset_omitsExtraPackages(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(put(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)));
        stubFor(get(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(COMPLETED_PROMO_REPORT)));
        stubFor(get(urlPathMatching(CMD_TASKS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PROMO_TASKS)));

        // when — a base-package-only change
        promoBatch(wmInfo).modifyBatch(
                BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE)).basePackage(BASE_PACKAGE_ID).build());

        // then — extraPackages is OMITTED (partial body preserves the offers' existing extras)
        verify(1, putRequestedFor(urlPathMatching(CMD_PATH))
                .withRequestBody(matchingJsonPath(BASE_PKG_JSON_PATH, equalTo(BASE_PACKAGE_ID))));
        verify(0, putRequestedFor(urlPathMatching(CMD_PATH))
                .withRequestBody(matchingJsonPath("$.modification.extraPackages")));
    }

    @Test
    void modifyBatch_whenSubmitRejected_throwsBadRequestAndSkipsPolling(WireMockRuntimeInfo wmInfo) {
        // given — the command submission itself is rejected
        stubFor(put(urlPathMatching(CMD_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST).withBody(BAD_REQUEST_BODY)));

        // then — the typed field error surfaces and no polling happens
        AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                () -> promoBatch(wmInfo).modifyBatch(BatchPromoOptionsRequest.forOffers(List.of(OFFER_ONE))
                        .basePackage(BASE_PACKAGE_ID).build()));
        assertEquals("offerCriteria", failure.errors().get(0).path());
        verify(0, getRequestedFor(urlPathMatching(CMD_PATH)));
    }

    @Test
    void promoOptionModification_toRaw_mapsEachKindToItsWireValue() {
        assertEquals(PromoOptionsModificationRaw.ModificationTypeEnum.CHANGE,
                PromoOptionModification.change(PromoPackageType.BASE, PACKAGE_ID).toRaw().getModificationType());
        assertEquals(PromoOptionsModificationRaw.ModificationTypeEnum.REMOVE_NOW,
                PromoOptionModification.removeNow(PromoPackageType.EXTRA, PACKAGE_ID).toRaw().getModificationType());
        assertEquals(PromoOptionsModificationRaw.ModificationTypeEnum.REMOVE_WITH_END_OF_CYCLE,
                PromoOptionModification.removeAtEndOfCycle(PromoPackageType.BASE, PACKAGE_ID)
                        .toRaw().getModificationType());
    }
}
