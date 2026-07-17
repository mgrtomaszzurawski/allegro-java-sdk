/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.contacts;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.Contacts;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.builder.ContactRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.contacts.model.Contact;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Full-stack WireMock coverage of the contacts facade: OAuth token acquisition,
 * vendor media types, request-body contract, Raw → record mapping, and the
 * mandatory error-path table (400 field errors, 401 replay, 404, 429, 5xx).
 */
@WireMockTest
class ContactsClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String CONTACTS_PATH = "/sale/offer-contacts";
    private static final String CONTACT_ID = "3b5ba33b-8536-420e-beea-88a3f00d8789";
    private static final String CONTACT_PATH = CONTACTS_PATH + "/" + CONTACT_ID;

    private static final String TEST_NAME = "Main contact";
    private static final String TEST_EMAIL = "my@email.com";
    private static final String TEST_PHONE = "+48512323495";
    private static final String TEST_TRACE_ID = "4631702648f0524e";

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final int RETRY_TWICE = 2;
    private static final String RETRY_AFTER_SECONDS = "1";
    private static final long RETRY_AFTER_EXPECTED = 1L;
    private static final String FIELD_ERROR_CODE = "ValidationError";
    private static final String FIELD_ERROR_PATH = "name";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (ContactResponse example shape from the
    // OpenAPI spec; a sandbox capture in the contacts demo confirms/corrects it)
    private static final String CONTACT_RESPONSE = """
            {"id":"%s","name":"%s","emails":[{"address":"%s"}],"phones":[{"number":"%s"}]}
            """.formatted(CONTACT_ID, TEST_NAME, TEST_EMAIL, TEST_PHONE);
    private static final String CONTACT_LIST_RESPONSE =
            "{\"contacts\":[" + CONTACT_RESPONSE + "]}";
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"%s","message":"Name is too long",
              "userMessage":"Nazwa jest za długa","path":"%s","details":null}]}
            """.formatted(FIELD_ERROR_CODE, FIELD_ERROR_PATH);
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Contact not found",
              "userMessage":"Nie znaleziono","path":null}]}
            """;
    private static final String BUSY_RESPONSE = "{\"errors\":[]}";

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

    private static ContactRequest sampleRequest() {
        return ContactRequest.builder().name(TEST_NAME).email(TEST_EMAIL).phone(TEST_PHONE).build();
    }

    // ---- happy paths ----

    @Test
    void list_whenContactsExist_returnsMappedRecords(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CONTACTS_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CONTACT_LIST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            List<Contact> contacts = allegro.contacts().list();

            // then
            assertEquals(1, contacts.size());
            Contact contact = contacts.get(0);
            assertEquals(CONTACT_ID, contact.id());
            assertEquals(TEST_NAME, contact.name());
            assertEquals(List.of(TEST_EMAIL), contact.emails());
            assertEquals(List.of(TEST_PHONE), contact.phones());
            verify(1, getRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }

    @Test
    void get_whenContactExists_sendsVendorAcceptAndMapsRecord(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CONTACT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CONTACT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Contact contact = allegro.contacts().get(CONTACT_ID);

            // then
            assertEquals(CONTACT_ID, contact.id());
            assertEquals(TEST_NAME, contact.name());
            verify(1, getRequestedFor(urlEqualTo(CONTACT_PATH)));
        }
    }

    @Test
    void create_whenValidRequest_sendsVendorJsonBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(CONTACTS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(TEST_NAME)))
                .withRequestBody(matchingJsonPath("$.emails[0].address", equalTo(TEST_EMAIL)))
                .withRequestBody(matchingJsonPath("$.phones[0].number", equalTo(TEST_PHONE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(CONTACT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Contact created = allegro.contacts().create(sampleRequest());

            // then — the write reached the wire exactly once, response mapped
            assertEquals(CONTACT_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }

    @Test
    void update_whenValidRequest_putsVendorJsonBodyToContactPath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(CONTACT_PATH))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(TEST_NAME)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CONTACT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Contact updated = allegro.contacts().update(CONTACT_ID, sampleRequest());

            // then
            assertEquals(CONTACT_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(CONTACT_PATH)));
        }
    }

    // ---- error-path table (per facade) ----

    @Test
    void create_when400_throwsBadRequestWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(CONTACTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Contacts contacts = allegro.contacts();
            ContactRequest request = sampleRequest();

            // then — the typed field errors survive parsing
            AllegroBadRequestException failure =
                    assertThrows(AllegroBadRequestException.class, () -> contacts.create(request));
            assertEquals(1, failure.errors().size());
            assertEquals(FIELD_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(FIELD_ERROR_PATH, failure.errors().get(0).path());
            // POSTs are not retried by default
            verify(1, postRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two on re-auth
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(CONTACT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(CONTACT_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CONTACT_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            // when
            Contact contact = allegro.contacts().get(CONTACT_ID);

            // then — replayed once, the second request carried the FRESH token
            assertEquals(CONTACT_ID, contact.id());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(CONTACT_PATH)));
            verify(1, getRequestedFor(urlEqualTo(CONTACT_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CONTACT_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            Contacts contacts = allegro.contacts();

            // then
            AllegroNotFoundException failure =
                    assertThrows(AllegroNotFoundException.class, () -> contacts.get(CONTACT_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void list_when429Persists_retriesThenThrowsRateLimit(WireMockRuntimeInfo wmInfo) {
        // given — always throttled; a two-attempt policy exhausts and surfaces the 429
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CONTACTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS)
                        .withBody(BUSY_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Contacts contacts = allegro.contacts();

            // then — retried to exhaustion, Retry-After surfaced to the caller
            AllegroRateLimitException failure =
                    assertThrows(AllegroRateLimitException.class, contacts::list);
            assertEquals(RETRY_AFTER_EXPECTED, failure.retryAfterSeconds());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }

    @Test
    void list_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — a GET is idempotent, so a transient 500 is retried
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(CONTACTS_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(get(urlEqualTo(CONTACTS_PATH)).inScenario(SCENARIO_REPLAY)
                .whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(CONTACT_LIST_RESPONSE)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            // when
            List<Contact> contacts = allegro.contacts().list();

            // then
            assertEquals(1, contacts.size());
            verify(RETRY_TWICE, getRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }

    @Test
    void create_when5xx_isNotRetried(WireMockRuntimeInfo wmInfo) {
        // given — writes are not idempotent; retryPost defaults to false
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(CONTACTS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));
        RetryPolicy twoAttempts = RetryPolicy.builder().maxAttempts(RETRY_TWICE).build();

        try (AllegroClient allegro = client(wmInfo, twoAttempts)) {
            Contacts contacts = allegro.contacts();
            ContactRequest request = sampleRequest();

            // then — exactly one attempt, no retry on a POST
            assertThrows(AllegroServerException.class, () -> contacts.create(request));
            verify(1, postRequestedFor(urlEqualTo(CONTACTS_PATH)));
        }
    }
}
