/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.settings;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroClientConfig;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.ClientCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsiblePersonRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.builder.ResponsibleProducerRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyAddress;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePartyContact;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsiblePerson;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.settings.compliance.model.ResponsibleProducer;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock coverage of the product-compliance facade: request/response mapping,
 * offset/limit pagination, the vendor media type on the wire, and the error-path
 * table (404/400). Live write→read is verified by the {@code settings-compliance}
 * demo (create→read on the seller sandbox).
 */
@WireMockTest
class ComplianceClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String PERSONS_PATH = "/sale/responsible-persons";
    private static final String PRODUCERS_PATH = "/sale/responsible-producers";
    private static final String PERSON_ID = "fee43309-8761-43f9-9cfd-a43e539a0fc5";
    private static final String PRODUCER_ID = "a1b2c3d4-0000-43f9-9cfd-a43e539a0fc5";
    private static final String PERSON_PATH = PERSONS_PATH + "/" + PERSON_ID;
    private static final String PRODUCER_PATH = PRODUCERS_PATH + "/" + PRODUCER_ID;

    private static final String PARAM_OFFSET = "offset";
    private static final String PARAM_LIMIT = "limit";
    private static final String OFFSET_PAGE_0 = "0";
    private static final String OFFSET_PAGE_1 = "1000";
    private static final String LIMIT_VALUE = "1000";

    private static final String NAME = "Person responsible for batteries";
    private static final String PERSON_NAME = "Responsible person company name";
    private static final String PRODUCER_INTERNAL_NAME = "Company responsible for producing product";
    private static final String TRADE_NAME = "Trade name of responsible producer";
    private static final String COUNTRY = "PL";
    private static final String STREET = "Wiśniowa 1";
    private static final String POSTAL_CODE = "00-000";
    private static final String CITY = "Warszawa";
    private static final String EMAIL = "some@email.com";
    private static final String TRACE_ID = "4631702648f0524e";
    private static final String RETRY_AFTER_SECONDS = "1";
    private static final String BAD_REQUEST_CODE = "ValidationException";
    private static final String BAD_REQUEST_PATH = "personalData.contact";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // Live-verified 2026-07-18 (sandbox, seller TestBoxSDK) via the
    // settings-compliance demo write->read (create->read round-trip green).
    private static final String PERSON_RESPONSE = """
            {"id":"%s","name":"%s","personalData":{"name":"%s",
             "address":{"countryCode":"%s","street":"%s","postalCode":"%s","city":"%s"},
             "contact":{"email":"%s","phoneNumber":"123123123","formUrl":null}}}
            """.formatted(PERSON_ID, NAME, PERSON_NAME, COUNTRY, STREET, POSTAL_CODE, CITY, EMAIL);
    private static final String PRODUCER_RESPONSE = """
            {"id":"%s","name":"%s","producerData":{"tradeName":"%s",
             "address":{"countryCode":"%s","street":"%s","postalCode":"%s","city":"%s"},
             "contact":{"email":"%s"}}}
            """.formatted(PRODUCER_ID, PRODUCER_INTERNAL_NAME, TRADE_NAME, COUNTRY, STREET, POSTAL_CODE, CITY, EMAIL);
    private static final String BAD_REQUEST_RESPONSE = """
            {"errors":[{"code":"ValidationException","message":"contact is required",
              "userMessage":"Kontakt jest wymagany","path":"personalData.contact"}]}
            """;

    private static String personsPage(int count, int totalCount, int offset) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                items.append(',');
            }
            items.append("{\"id\":\"fee43309-8761-43f9-9cfd-a43e539a00")
                    .append("%02d".formatted(offset + index))
                    .append("\",\"name\":\"").append(NAME).append(' ').append(offset + index)
                    .append("\",\"personalData\":{\"name\":\"").append(PERSON_NAME).append("\"}}");
        }
        return "{\"responsiblePersons\":[" + items + "],\"count\":" + count
                + ",\"totalCount\":" + totalCount + "}";
    }

    private static String producersPage(int count, int totalCount) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                items.append(',');
            }
            items.append("{\"id\":\"a1b2c3d4-0000-43f9-9cfd-a43e539a00")
                    .append("%02d".formatted(index))
                    .append("\",\"name\":\"").append(PRODUCER_INTERNAL_NAME).append(' ').append(index)
                    .append("\",\"producerData\":{\"tradeName\":\"").append(TRADE_NAME).append("\"}}");
        }
        return "{\"responsibleProducers\":[" + items + "],\"count\":" + count
                + ",\"totalCount\":" + totalCount + "}";
    }

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

    private static ResponsiblePersonRequest samplePerson() {
        return ResponsiblePersonRequest.builder()
                .name(NAME)
                .personName(PERSON_NAME)
                .address(new ResponsiblePartyAddress(COUNTRY, STREET, POSTAL_CODE, CITY))
                .contact(new ResponsiblePartyContact(EMAIL, null, null))
                .build();
    }

    private static ResponsibleProducerRequest sampleProducer() {
        return ResponsibleProducerRequest.builder()
                .name(PRODUCER_INTERNAL_NAME)
                .tradeName(TRADE_NAME)
                .address(new ResponsiblePartyAddress(COUNTRY, STREET, POSTAL_CODE, CITY))
                .contact(new ResponsiblePartyContact(EMAIL, null, null))
                .build();
    }

    // ---- responsible persons ----

    @Test
    void streamResponsiblePersons_whenPage_mapsEveryField(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(PERSONS_PATH))
                .withHeader(TestHttpConstants.ACCEPT_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody("{\"responsiblePersons\":[" + PERSON_RESPONSE + "],\"count\":1,\"totalCount\":1}")));

        try (AllegroClient allegro = client(wmInfo)) {
            List<ResponsiblePerson> persons = allegro.settings().compliance()
                    .streamResponsiblePersons().toList();

            assertEquals(1, persons.size());
            ResponsiblePerson person = persons.get(0);
            assertEquals(PERSON_ID, person.id());
            assertEquals(NAME, person.name());
            assertEquals(PERSON_NAME, person.personName());
            assertNotNull(person.address());
            assertEquals(COUNTRY, person.address().countryCode());
            assertEquals(CITY, person.address().city());
            assertNotNull(person.contact());
            assertEquals(EMAIL, person.contact().email());
        }
    }

    @Test
    void streamResponsiblePersons_whenNotConsumed_defersTheFetch(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(PERSONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(personsPage(1, 1, 0))));

        try (AllegroClient allegro = client(wmInfo)) {
            var stream = allegro.settings().compliance().streamResponsiblePersons();
            verify(0, getRequestedFor(urlPathEqualTo(PERSONS_PATH)));
            stream.findFirst();
            verify(1, getRequestedFor(urlPathEqualTo(PERSONS_PATH)));
        }
    }

    @Test
    void streamResponsiblePersons_whenTotalCountReached_stopsWalk(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(PERSONS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(personsPage(2, 2, 0))));

        try (AllegroClient allegro = client(wmInfo)) {
            long total = allegro.settings().compliance().streamResponsiblePersons().count();
            assertEquals(2, total);
            verify(1, getRequestedFor(urlPathEqualTo(PERSONS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                    .withQueryParam(PARAM_LIMIT, equalTo(LIMIT_VALUE)));
            verify(0, getRequestedFor(urlPathEqualTo(PERSONS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1)));
        }
    }

    @Test
    void streamResponsiblePersons_whenTotalCountExceedsPage_walksNextPage(WireMockRuntimeInfo wmInfo) {
        // Drives the totalCount continuation branch: page 0 reports more remain, so
        // the walk fetches the next offset; page 1 completes the total and stops.
        stubToken();
        stubFor(get(urlPathEqualTo(PERSONS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(personsPage(2, 3, 0))));
        stubFor(get(urlPathEqualTo(PERSONS_PATH))
                .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(personsPage(1, 3, 2))));

        try (AllegroClient allegro = client(wmInfo)) {
            long total = allegro.settings().compliance().streamResponsiblePersons().count();
            assertEquals(3, total);
            verify(1, getRequestedFor(urlPathEqualTo(PERSONS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_1)));
        }
    }

    @Test
    void createResponsiblePerson_whenValidRequest_postsNestedBodyAndMaps(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(PERSONS_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(PERSON_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            ResponsiblePerson created = allegro.settings().compliance().createResponsiblePerson(samplePerson());

            assertEquals(PERSON_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(PERSONS_PATH))
                    .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.personalData.name", equalTo(PERSON_NAME)))
                    .withRequestBody(matchingJsonPath("$.personalData.address.countryCode", equalTo(COUNTRY)))
                    .withRequestBody(matchingJsonPath("$.personalData.contact.email", equalTo(EMAIL))));
        }
    }

    @Test
    void updateResponsiblePerson_whenValidRequest_putsBodyWithId(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(put(urlEqualTo(PERSON_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PERSON_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            ResponsiblePerson updated = allegro.settings().compliance()
                    .updateResponsiblePerson(PERSON_ID, samplePerson());

            assertEquals(PERSON_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(PERSON_PATH))
                    .withRequestBody(matchingJsonPath("$.id", equalTo(PERSON_ID)))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(NAME)))
                    .withRequestBody(matchingJsonPath("$.personalData.name", equalTo(PERSON_NAME))));
        }
    }

    @Test
    void createResponsiblePerson_whenBadRequest_throwsWithParsedFieldErrors(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(PERSONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)
                        .withBody(BAD_REQUEST_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compliance = allegro.settings().compliance();
            ResponsiblePersonRequest request = samplePerson();

            AllegroBadRequestException failure = assertThrows(AllegroBadRequestException.class,
                    () -> compliance.createResponsiblePerson(request));
            assertEquals(BAD_REQUEST_CODE, failure.errors().get(0).code());
            assertEquals(BAD_REQUEST_PATH, failure.errors().get(0).path());
            assertEquals(TRACE_ID, failure.traceId());
            verify(1, postRequestedFor(urlEqualTo(PERSONS_PATH)));
        }
    }

    @Test
    void createResponsiblePerson_whenRateLimited_throwsRateLimitAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(PERSONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compliance = allegro.settings().compliance();
            ResponsiblePersonRequest request = samplePerson();

            AllegroRateLimitException failure = assertThrows(AllegroRateLimitException.class,
                    () -> compliance.createResponsiblePerson(request));
            assertEquals(1L, failure.retryAfterSeconds());
            verify(1, postRequestedFor(urlEqualTo(PERSONS_PATH)));
        }
    }

    @Test
    void createResponsiblePerson_whenServerError_throwsServerExceptionAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(PERSONS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compliance = allegro.settings().compliance();
            ResponsiblePersonRequest request = samplePerson();

            assertThrows(AllegroServerException.class, () -> compliance.createResponsiblePerson(request));
            verify(1, postRequestedFor(urlEqualTo(PERSONS_PATH)));
        }
    }

    // ---- responsible producers ----

    @Test
    void streamResponsibleProducers_whenPage_mapsProducers(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlPathEqualTo(PRODUCERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(producersPage(2, 2))));

        try (AllegroClient allegro = client(wmInfo)) {
            List<ResponsibleProducer> producers = allegro.settings().compliance()
                    .streamResponsibleProducers().toList();
            assertEquals(2, producers.size());
            assertEquals(PRODUCER_INTERNAL_NAME + " 0", producers.get(0).name());
            assertEquals(TRADE_NAME, producers.get(0).tradeName());
            verify(1, getRequestedFor(urlPathEqualTo(PRODUCERS_PATH))
                    .withQueryParam(PARAM_OFFSET, equalTo(OFFSET_PAGE_0)));
        }
    }

    @Test
    void responsibleProducer_whenFound_mapsFullDefinition(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(PRODUCER_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PRODUCER_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            ResponsibleProducer producer = allegro.settings().compliance().responsibleProducer(PRODUCER_ID);

            assertEquals(PRODUCER_ID, producer.id());
            assertEquals(PRODUCER_INTERNAL_NAME, producer.name());
            assertEquals(TRADE_NAME, producer.tradeName());
            assertNotNull(producer.address());
            assertEquals(COUNTRY, producer.address().countryCode());
            assertNotNull(producer.contact());
            assertEquals(EMAIL, producer.contact().email());
            verify(1, getRequestedFor(urlEqualTo(PRODUCER_PATH)));
        }
    }

    @Test
    void responsibleProducer_whenNotFound_throwsNotFound(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(get(urlEqualTo(PRODUCER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TRACE_ID)));

        try (AllegroClient allegro = client(wmInfo)) {
            var compliance = allegro.settings().compliance();
            AllegroNotFoundException failure = assertThrows(AllegroNotFoundException.class,
                    () -> compliance.responsibleProducer(PRODUCER_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TRACE_ID, failure.traceId());
        }
    }

    @Test
    void createResponsibleProducer_whenValidRequest_postsNestedBodyAndMaps(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(post(urlEqualTo(PRODUCERS_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(PRODUCER_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            ResponsibleProducer created = allegro.settings().compliance()
                    .createResponsibleProducer(sampleProducer());

            assertEquals(PRODUCER_ID, created.id());
            verify(1, postRequestedFor(urlEqualTo(PRODUCERS_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(PRODUCER_INTERNAL_NAME)))
                    .withRequestBody(matchingJsonPath("$.producerData.tradeName", equalTo(TRADE_NAME)))
                    .withRequestBody(matchingJsonPath("$.producerData.address.countryCode", equalTo(COUNTRY))));
        }
    }

    @Test
    void updateResponsibleProducer_whenValidRequest_putsBodyWithId(WireMockRuntimeInfo wmInfo) {
        stubToken();
        stubFor(put(urlEqualTo(PRODUCER_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(PRODUCER_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            ResponsibleProducer updated = allegro.settings().compliance()
                    .updateResponsibleProducer(PRODUCER_ID, sampleProducer());

            assertEquals(PRODUCER_ID, updated.id());
            verify(1, putRequestedFor(urlEqualTo(PRODUCER_PATH))
                    .withRequestBody(matchingJsonPath("$.id", equalTo(PRODUCER_ID)))
                    .withRequestBody(matchingJsonPath("$.producerData.tradeName", equalTo(TRADE_NAME))));
        }
    }
}
