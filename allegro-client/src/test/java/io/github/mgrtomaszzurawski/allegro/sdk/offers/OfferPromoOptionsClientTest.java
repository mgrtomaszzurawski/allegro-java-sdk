/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroExecutionInterceptor;
import io.github.mgrtomaszzurawski.allegro.sdk.config.policy.RetryPolicy;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.AvailablePromotionPackages;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MarketplacePromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.OfferPromoOptions;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.client.offers.PromoOptionsImpl;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.allegro.sdk.internal.runtime.transport.RetryHandler;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@WireMockTest
class OfferPromoOptionsClientTest {

    private static final String TEST_TOKEN = "promo-test-token";
    private static final String AVAILABLE_PATH = "/sale/offer-promotion-packages";
    private static final String OFFER_ID = "246810";
    private static final String FOR_OFFER_PATH = "/sale/offers/" + OFFER_ID + "/promo-options";

    private static final String BASE_ID = "BOLD";
    private static final String BASE_NAME = "Bold title";
    private static final String BASE_CYCLE = "P7D";
    private static final String EXTRA_ID = "HIGHLIGHT";
    private static final String MARKETPLACE_ID = "allegro-pl";
    private static final String ADDL_MARKETPLACE_ID = "allegro-cz";
    private static final String PENDING_BASE_ID = "BOLD_NEXT";

    private static final String AVAILABLE_BODY = "{\"basePackages\":[{\"id\":\"" + BASE_ID
            + "\",\"name\":\"" + BASE_NAME + "\",\"cycleDuration\":\"" + BASE_CYCLE + "\"}],"
            + "\"extraPackages\":[{\"id\":\"" + EXTRA_ID + "\",\"name\":\"Highlight\"}]}";
    private static final String FOR_OFFER_BODY = "{\"offerId\":\"" + OFFER_ID + "\","
            + "\"marketplaceId\":\"" + MARKETPLACE_ID + "\","
            + "\"basePackage\":{\"id\":\"" + BASE_ID + "\",\"validFrom\":\"2026-01-01T00:00:00Z\","
            + "\"validTo\":\"2026-02-01T00:00:00Z\"},"
            + "\"extraPackages\":[{\"id\":\"" + EXTRA_ID + "\"}],"
            + "\"pendingChanges\":{\"basePackage\":{\"id\":\"" + PENDING_BASE_ID + "\"}},"
            + "\"additionalMarketplaces\":[{\"marketplaceId\":\"" + ADDL_MARKETPLACE_ID + "\","
            + "\"basePackage\":{\"id\":\"" + BASE_ID + "\"},"
            + "\"extraPackages\":[{\"id\":\"" + EXTRA_ID + "\"}],"
            + "\"pendingChanges\":{\"basePackage\":{\"id\":\"" + PENDING_BASE_ID + "\"}}}]}";
    private static final String MINIMAL_FOR_OFFER_BODY = "{\"offerId\":\"" + OFFER_ID + "\","
            + "\"basePackage\":{\"id\":\"" + BASE_ID + "\"}}";
    private static final String NOT_FOUND_BODY =
            "{\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"offer not found\"}]}";

    private static PromoOptionsImpl promoOptions(WireMockRuntimeInfo wmInfo) {
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
                // no-op: 401 replay is covered in the transport suite
            }
        };
        return new PromoOptionsImpl(runtime);
    }

    @Test
    void availablePackages_whenPackagesOffered_mapsBaseAndExtra(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(AVAILABLE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(AVAILABLE_BODY)));

        // when
        AvailablePromotionPackages available = promoOptions(wmInfo).availablePackages();

        // then
        assertEquals(1, available.basePackages().size());
        assertEquals(BASE_ID, available.basePackages().get(0).id());
        assertEquals(BASE_NAME, available.basePackages().get(0).name());
        assertEquals(BASE_CYCLE, available.basePackages().get(0).cycleDuration());
        assertEquals(1, available.extraPackages().size());
        assertEquals(EXTRA_ID, available.extraPackages().get(0).id());
    }

    @Test
    void forOffer_whenOptionsApplied_mapsMarketplacePendingAndAdditionalMarketplaces(
            WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(FOR_OFFER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(FOR_OFFER_BODY)));

        // when
        OfferPromoOptions applied = promoOptions(wmInfo).forOffer(OFFER_ID);

        // then — base marketplace, packages and validity map
        assertEquals(OFFER_ID, applied.offerId());
        assertEquals(MARKETPLACE_ID, applied.marketplaceId());
        assertNotNull(applied.basePackage());
        assertEquals(BASE_ID, applied.basePackage().id());
        assertNotNull(applied.basePackage().validFrom());
        assertEquals(1, applied.extraPackages().size());
        assertEquals(EXTRA_ID, applied.extraPackages().get(0).id());
        // and the pending base-package change queued for the next cycle
        assertNotNull(applied.pendingChanges());
        assertNotNull(applied.pendingChanges().basePackage());
        assertEquals(PENDING_BASE_ID, applied.pendingChanges().basePackage().id());
        // and the per-additional-marketplace options, keyed by marketplace id
        assertEquals(1, applied.additionalMarketplaces().size());
        MarketplacePromoOptions additional = applied.additionalMarketplaces().get(ADDL_MARKETPLACE_ID);
        assertNotNull(additional);
        assertEquals(BASE_ID, additional.basePackage().id());
        assertEquals(1, additional.extraPackages().size());
        assertEquals(EXTRA_ID, additional.extraPackages().get(0).id());
        assertNotNull(additional.pendingChanges());
        assertEquals(PENDING_BASE_ID, additional.pendingChanges().basePackage().id());
    }

    @Test
    void forOffer_whenNoPendingOrAdditionalMarketplaces_mapsNullAndEmptyDefaults(
            WireMockRuntimeInfo wmInfo) {
        // given a response that omits marketplaceId, pendingChanges and additionalMarketplaces
        stubFor(get(urlEqualTo(FOR_OFFER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(MINIMAL_FOR_OFFER_BODY)));

        // when
        OfferPromoOptions applied = promoOptions(wmInfo).forOffer(OFFER_ID);

        // then the optional blocks tolerate absence: null pending, empty marketplace map
        assertEquals(BASE_ID, applied.basePackage().id());
        assertNull(applied.marketplaceId());
        assertNull(applied.pendingChanges());
        assertTrue(applied.additionalMarketplaces().isEmpty());
    }

    @Test
    void forOffer_whenOfferMissing_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(FOR_OFFER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND_BODY)));

        // then
        assertThrows(AllegroNotFoundException.class, () -> promoOptions(wmInfo).forOffer(OFFER_ID));
    }

    @Test
    void availablePackages_whenNoPackages_returnsEmptyLists(WireMockRuntimeInfo wmInfo) {
        // given — a response with no package lists at all (null-tolerant mapping)
        stubFor(get(urlEqualTo(AVAILABLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody("{}")));

        // when
        AvailablePromotionPackages available = promoOptions(wmInfo).availablePackages();

        // then — absent lists map to empty, never null
        assertTrue(available.basePackages().isEmpty());
        assertTrue(available.extraPackages().isEmpty());
    }

    @Test
    void availablePackages_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(AVAILABLE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        // then
        assertThrows(AllegroServerException.class, () -> promoOptions(wmInfo).availablePackages());
    }
}
