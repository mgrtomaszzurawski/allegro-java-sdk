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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoOptionModification;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.PromoPackageType;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.PromoOptionsImpl;
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

    private static PromoOptionsImpl promo(WireMockRuntimeInfo wmInfo) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new org.openapitools.jackson.nullable.JsonNullableModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RetryHandler retryHandler = new RetryHandler(HttpClient.newHttpClient(),
                RetryPolicy.builder().enabled(false).build());
        HttpRuntime runtime = new HttpRuntime() {
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
        return new PromoOptionsImpl(runtime);
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
