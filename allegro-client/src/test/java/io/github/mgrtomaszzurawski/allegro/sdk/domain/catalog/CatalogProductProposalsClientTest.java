/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductChangeProposalRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductProposalParameter;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.builder.ProductProposalRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductChangeProposal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductProposal;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProductProposalStatus;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.catalog.model.ProposalResolution;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for the {@code catalog().products()} proposal operations:
 * the create/change request bodies pin the wire shape (product name + category),
 * the responses map onto {@link ProductProposal}/{@link ProductChangeProposal}
 * (status + resolution enums), builders reject a missing name fail-fast, and the
 * not-found error path is typed.
 *
 * <p>Fixtures are {@code spec-derived}: product proposals are moderation-gated, so
 * a live write→read is not exercised here (see the E2E debt ledger).
 */
@WireMockTest
class CatalogProductProposalsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String PRODUCT_PROPOSALS_PATH = "/sale/product-proposals";
    private static final String PRODUCT_ID = "8f2b1c00-0000-4000-8000-000000000001";
    private static final String CHANGE_PROPOSAL_ID = "cp-77";
    private static final String PRODUCT_CHANGE_PROPOSALS_PATH =
            "/sale/products/" + PRODUCT_ID + "/change-proposals";
    private static final String CHANGE_PROPOSAL_PATH =
            "/sale/products/change-proposals/" + CHANGE_PROPOSAL_ID;

    private static final String PROPOSAL_ID = "prop-1";
    private static final String PRODUCT_NAME = "ACME Widget 3000";
    private static final String CHANGED_NAME = "ACME Widget 3000 (2026)";
    private static final String CATEGORY_ID = "257";
    private static final String IMAGE_URL = "https://img.example/widget.jpg";
    private static final String PARAM_ID = "11323";
    private static final String PARAM_VALUE_ID = "1";
    private static final String LANGUAGE = "pl-PL";
    private static final String NOTE = "Renamed to the 2026 edition";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","token_type":"bearer","expires_in":%d,"scope":"sale:offers:write"}
            """;

    private static final String PROPOSAL_RESPONSE = """
            {"id":"%s","name":"%s","category":{"id":"%s"},
             "images":[{"url":"%s"}],"language":"%s","publication":{"status":"PROPOSED"}}
            """.formatted(PROPOSAL_ID, PRODUCT_NAME, CATEGORY_ID, IMAGE_URL, LANGUAGE);

    private static final String CHANGE_PROPOSAL_RESPONSE = """
            {"id":"%s","note":"%s","notifyViaEmailAfterVerification":true,"language":"%s",
             "name":{"current":"%s","proposal":"%s","reason":null,"resolutionType":"UNRESOLVED"}}
            """.formatted(CHANGE_PROPOSAL_ID, NOTE, LANGUAGE, PRODUCT_NAME, CHANGED_NAME);

    private static final String NOT_FOUND = """
            {"errors":[{"code":"NotFound","message":"No such change proposal"}]}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        .build());
    }

    private static void stubToken() {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS))));
    }

    @Test
    void propose_whenAccepted_mapsProposalAndSendsNameCategoryParameter(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlPathEqualTo(PRODUCT_PROPOSALS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK).withBody(PROPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ProductProposal proposal = allegro.catalog().products().propose(
                    ProductProposalRequest.builder()
                            .name(PRODUCT_NAME).categoryId(CATEGORY_ID)
                            .addImageUrl(IMAGE_URL)
                            .addParameter(ProductProposalParameter.ofValueIds(PARAM_ID, PARAM_VALUE_ID))
                            .language(LANGUAGE)
                            .build());

            // then — response mapped
            assertEquals(PROPOSAL_ID, proposal.id());
            assertEquals(PRODUCT_NAME, proposal.name());
            assertEquals(CATEGORY_ID, proposal.categoryId());
            assertEquals(ProductProposalStatus.PROPOSED, proposal.status());

            // and the request body carried name, category id and the parameter value id
            verify(1, postRequestedFor(urlPathEqualTo(PRODUCT_PROPOSALS_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(PRODUCT_NAME)))
                    .withRequestBody(matchingJsonPath("$.category.id", equalTo(CATEGORY_ID)))
                    .withRequestBody(matchingJsonPath("$.parameters[0].valuesIds[0]", equalTo(PARAM_VALUE_ID))));
        }
    }

    @Test
    void proposeChange_whenAccepted_mapsNameProposalAndSendsName(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(post(urlPathEqualTo(PRODUCT_CHANGE_PROPOSALS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CHANGE_PROPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ProductChangeProposal proposal = allegro.catalog().products().proposeChange(PRODUCT_ID,
                    ProductChangeProposalRequest.builder()
                            .name(CHANGED_NAME).note(NOTE)
                            .notifyViaEmailAfterVerification(true)
                            .build());

            // then — the proposed name and its resolution mapped
            assertEquals(CHANGE_PROPOSAL_ID, proposal.id());
            assertEquals(NOTE, proposal.note());
            assertEquals(CHANGED_NAME, proposal.name().proposal());
            assertEquals(ProposalResolution.UNRESOLVED, proposal.name().resolution());

            // and the corrected name reached the wire
            verify(1, postRequestedFor(urlPathEqualTo(PRODUCT_CHANGE_PROPOSALS_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(CHANGED_NAME))));
        }
    }

    @Test
    void changeProposal_whenExists_readsProposalById(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(CHANGE_PROPOSAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CHANGE_PROPOSAL_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            ProductChangeProposal proposal = allegro.catalog().products().changeProposal(CHANGE_PROPOSAL_ID);

            // then
            assertEquals(CHANGE_PROPOSAL_ID, proposal.id());
            assertEquals(CHANGED_NAME, proposal.name().proposal());
            verify(1, getRequestedFor(urlPathEqualTo(CHANGE_PROPOSAL_PATH)));
        }
    }

    @Test
    void changeProposal_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken();
        stubFor(get(urlPathEqualTo(CHANGE_PROPOSAL_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND).withBody(NOT_FOUND)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when / then
            assertThrows(AllegroNotFoundException.class,
                    () -> allegro.catalog().products().changeProposal(CHANGE_PROPOSAL_ID));
        }
    }

    @Test
    void propose_whenNameBlank_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> ProductProposalRequest.builder().categoryId(CATEGORY_ID).build());
    }

    @Test
    void proposeChange_whenNameBlank_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> ProductChangeProposalRequest.builder().note(NOTE).build());
    }

    @Test
    void changeProposalRequest_whenAllFieldsSet_projectsEveryFieldToRaw() {
        ProductChangeProposalRequest request = ProductChangeProposalRequest.builder()
                .name(CHANGED_NAME)
                .note(NOTE)
                .categoryId(CATEGORY_ID)
                .addImageUrl(IMAGE_URL)
                .addParameter(ProductProposalParameter.ofValues(PARAM_ID, "Cherry MX"))
                .notifyViaEmailAfterVerification(true)
                .language(LANGUAGE)
                .build();

        assertEquals(CHANGED_NAME, request.name());
        assertEquals(NOTE, request.note());
        assertEquals(CATEGORY_ID, request.categoryId());
        assertEquals(IMAGE_URL, request.imageUrls().get(0));
        assertEquals(PARAM_ID, request.parameters().get(0).id());
        assertEquals(LANGUAGE, request.language());

        var raw = request.toRaw();
        assertEquals(CHANGED_NAME, raw.getName());
        assertEquals(CATEGORY_ID, raw.getCategory().getId());
        assertEquals(IMAGE_URL, raw.getImages().get(0).getUrl());
        assertEquals("Cherry MX", raw.getParameters().get(0).getValues().get(0));
    }
}
