/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferPricingRules;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferRuleAssignment;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferRulePriceRange;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroBadRequestException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroRateLimitException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroServerException;
import io.github.mgrtomaszzurawski.allegro.sdk.support.TestHttpConstants;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * WireMock contract for {@link PricingAutomation}: request shape (path, auth,
 * vendor media type, body), response mapping (including the oneOf configuration
 * and {@link Money}), and the mandatory error-path table (400 field errors,
 * 401 replay, 404, 429 retry, 5xx with POST-not-retried).
 */
@WireMockTest
class PricingAutomationClientTest {

    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_TOKEN = "token-one";
    private static final String TEST_TOKEN_2 = "token-two";
    private static final long EXPIRY_SECONDS = 3600L;

    private static final String RULES_PATH = "/sale/price-automation/rules";
    private static final String TEST_RULE_ID = "641c73feaef0a8281a3d11f8";
    private static final String RULE_PATH = RULES_PATH + "/" + TEST_RULE_ID;
    private static final String TEST_RULE_NAME = "Follow Allegro minus 5%";
    private static final String DEFAULT_RULE_NAME = "Lowest price on Allegro";
    private static final String TEST_PERCENTAGE = "5";
    private static final String TEST_AMOUNT = "10.99";
    private static final String TEST_CURRENCY = "PLN";
    private static final Instant TEST_UPDATED_AT = Instant.parse("2026-07-17T10:15:30Z");

    private static final String TEST_TRACE_ID = "4631702648f0524e";
    private static final String TEST_ERROR_CODE = "ValidationError";
    private static final String TEST_ERROR_PATH = "type";
    private static final long TEST_RETRY_AFTER = 1L;

    private static final String SCENARIO_REPLAY = "replay-401";
    private static final String STATE_REAUTHED = "reauthed";
    private static final String SCENARIO_RETRY = "retry-5xx";
    private static final String STATE_RECOVERED = "recovered";

    private static final String TOKEN_RESPONSE = """
            {"access_token":"%s","expires_in":%d}
            """;
    // spec-derived: not yet wire-verified (confirmed by the pricing demo's
    // write->read pass against the sandbox before this bucket's final PR).
    private static final String PERCENTAGE_RULE_RESPONSE = """
            {"id":"%s","type":"FOLLOW_BY_ALLEGRO_MIN_PRICE","name":"%s","default":false,
             "updatedAt":"2026-07-17T10:15:30Z",
             "configuration":{"changeByPercentage":{"operation":"SUBTRACT","value":"5"}}}
            """;
    // spec-derived: not yet wire-verified
    private static final String AMOUNT_RULE_RESPONSE = """
            {"id":"%s","type":"FOLLOW_BY_MARKET_MIN_PRICE","name":"%s","default":false,
             "updatedAt":"2026-07-17T10:15:30Z",
             "configuration":{"changeByAmount":{"operation":"SUBTRACT",
               "values":[{"amount":"10.99","currency":"PLN"}]}}}
            """;
    // spec-derived: not yet wire-verified (a built-in default rule carries no configuration)
    private static final String DEFAULT_RULE_RESPONSE = """
            {"id":"%s","type":"FOLLOW_BY_TOP_OFFER_PRICE","name":"%s","default":true,
             "updatedAt":"2026-07-17T10:15:30Z"}
            """;
    // forward-compat: a rule strategy this SDK release does not model (the generated
    // type enum maps it to its UNKNOWN_DEFAULT_OPEN_API sentinel) must degrade to
    // PricingRuleType.UNKNOWN, not fail the read.
    private static final String UNKNOWN_TYPE_RULE_RESPONSE = """
            {"id":"%s","type":"PRICE_MATCH_FUTURE","name":"%s","default":false,
             "updatedAt":"2026-07-17T10:15:30Z",
             "configuration":{"changeByPercentage":{"operation":"SUBTRACT","value":"5"}}}
            """;
    // spec-derived: not yet wire-verified (errors[] contract shape)
    private static final String VALIDATION_ERROR_RESPONSE = """
            {"errors":[{"code":"%s","message":"Invalid rule type",
              "userMessage":"Invalid type","path":"%s","details":null,"metadata":null}]}
            """;
    private static final String NOT_FOUND_RESPONSE = """
            {"errors":[{"code":"NotFound","message":"Rule not found",
              "userMessage":"Not found","path":null}]}
            """;

    private static final String UPDATED_RULE_NAME = "Follow Allegro minus 8%";
    private static final String UPDATED_PERCENTAGE = "8";
    private static final String TEST_OFFER_ID = "12345";
    private static final String OFFER_RULES_PATH =
            "/sale/price-automation/offers/" + TEST_OFFER_ID + "/rules";
    private static final String TEST_MARKETPLACE_ID = "allegro-pl";
    private static final String MIN_PRICE_AMOUNT = "5.00";
    private static final String MAX_PRICE_AMOUNT = "500.00";

    // spec-derived: not yet wire-verified (rules-list wrapper: one default rule + one merchant rule)
    private static final String RULES_LIST_RESPONSE = """
            {"rules":[
              {"id":"default-1","type":"FOLLOW_BY_TOP_OFFER_PRICE","name":"%s","default":true,
               "updatedAt":"2026-07-17T10:15:30Z"},
              {"id":"%s","type":"FOLLOW_BY_ALLEGRO_MIN_PRICE","name":"%s","default":false,
               "updatedAt":"2026-07-17T10:15:30Z",
               "configuration":{"changeByPercentage":{"operation":"SUBTRACT","value":"5"}}}
            ]}
            """;
    // spec-derived: not yet wire-verified (edit response echoes the new name/percentage)
    private static final String UPDATED_RULE_RESPONSE = """
            {"id":"%s","type":"FOLLOW_BY_ALLEGRO_MIN_PRICE","name":"%s","default":false,
             "updatedAt":"2026-07-17T10:15:30Z",
             "configuration":{"changeByPercentage":{"operation":"SUBTRACT","value":"8"}}}
            """;
    // spec-derived: not yet wire-verified (offer-rule assignment with a price-range configuration)
    private static final String OFFER_RULES_RESPONSE = """
            {"rules":[
              {"marketplace":{"id":"allegro-pl"},"rule":{"id":"%s"},
               "configuration":{"priceRange":{"type":"BASE_MARKETPLACE_CURRENCY",
                 "minPrice":{"amount":"5.00","currency":"PLN"},
                 "maxPrice":{"amount":"500.00","currency":"PLN"}}},
               "updatedAt":"2026-07-17T10:15:30Z"}],
             "updatedAt":"2026-07-17T10:15:30Z"}
            """;

    private static AllegroClient client(WireMockRuntimeInfo wmInfo) {
        return AllegroClient.create(
                new ClientCredentials(TEST_CLIENT_ID, TEST_CLIENT_SECRET),
                AllegroClientConfig.builder(AllegroEnvironment.SANDBOX)
                        .apiBaseUrl(wmInfo.getHttpBaseUrl())
                        .oauthBaseUrl(wmInfo.getHttpBaseUrl() + "/auth/oauth")
                        // Two attempts, flat backoff: retries stay fast and the
                        // verify() counts are deterministic (one retry per GET).
                        .retryPolicy(RetryPolicy.builder()
                                .maxAttempts(2)
                                .backoffStrategy(RetryPolicy.BackoffStrategy.FIXED)
                                .build())
                        .build());
    }

    private static void stubToken(String accessToken) {
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(accessToken, EXPIRY_SECONDS))));
    }

    private static PricingRuleRequest percentageRule() {
        return PricingRuleRequest.builder()
                .name(TEST_RULE_NAME)
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                        PricingRuleConfiguration.Operation.SUBTRACT, TEST_PERCENTAGE))
                .build();
    }

    @Test
    void create_whenPercentageRule_postsVendorBodyAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RULES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(TEST_RULE_NAME)))
                .withRequestBody(matchingJsonPath("$.type",
                        equalTo(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE.name())))
                .withRequestBody(matchingJsonPath("$.configuration.changeByPercentage.operation",
                        equalTo(PricingRuleConfiguration.Operation.SUBTRACT.name())))
                .withRequestBody(matchingJsonPath("$.configuration.changeByPercentage.value",
                        equalTo(TEST_PERCENTAGE)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(PERCENTAGE_RULE_RESPONSE.formatted(TEST_RULE_ID, TEST_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().create(percentageRule());

            // then
            assertEquals(TEST_RULE_ID, rule.id());
            assertEquals(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE, rule.type());
            assertEquals(TEST_RULE_NAME, rule.name());
            assertFalse(rule.isDefault());
            assertEquals(TEST_UPDATED_AT, rule.updatedAt());
            PricingRuleConfiguration.ChangeByPercentage configuration = assertInstanceOf(
                    PricingRuleConfiguration.ChangeByPercentage.class, rule.configuration());
            assertEquals(PricingRuleConfiguration.Operation.SUBTRACT, configuration.operation());
            assertEquals(TEST_PERCENTAGE, configuration.value());
            verify(1, postRequestedFor(urlEqualTo(RULES_PATH)));
        }
    }

    @Test
    void create_whenAmountRule_serializesAmountConfigurationAndMapsMoney(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RULES_PATH))
                .withRequestBody(matchingJsonPath("$.configuration.changeByAmount.operation",
                        equalTo(PricingRuleConfiguration.Operation.SUBTRACT.name())))
                .withRequestBody(matchingJsonPath(
                        "$.configuration.changeByAmount.values[0].currency", equalTo(TEST_CURRENCY)))
                .withRequestBody(matchingJsonPath(
                        "$.configuration.changeByAmount.values[0].amount", equalTo(TEST_AMOUNT)))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_CREATED)
                        .withBody(AMOUNT_RULE_RESPONSE.formatted(TEST_RULE_ID, TEST_RULE_NAME))));

        PricingRuleRequest request = PricingRuleRequest.builder()
                .name(TEST_RULE_NAME)
                .type(PricingRuleType.FOLLOW_BY_MARKET_MIN_PRICE)
                .configuration(new PricingRuleConfiguration.ChangeByAmount(
                        PricingRuleConfiguration.Operation.SUBTRACT,
                        List.of(Money.of(TEST_AMOUNT, TEST_CURRENCY))))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().create(request);

            // then — response oneOf mapped back to ChangeByAmount with exact Money
            PricingRuleConfiguration.ChangeByAmount configuration = assertInstanceOf(
                    PricingRuleConfiguration.ChangeByAmount.class, rule.configuration());
            assertEquals(1, configuration.values().size());
            assertEquals(Money.of(TEST_AMOUNT, TEST_CURRENCY), configuration.values().get(0));
            verify(1, postRequestedFor(urlEqualTo(RULES_PATH)));
        }
    }

    @Test
    void get_whenDefaultRule_mapsRuleWithoutConfiguration(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEFAULT_RULE_RESPONSE.formatted(TEST_RULE_ID, DEFAULT_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().get(TEST_RULE_ID);

            // then
            assertEquals(PricingRuleType.FOLLOW_BY_TOP_OFFER_PRICE, rule.type());
            assertTrue(rule.isDefault());
            assertNull(rule.configuration());
            verify(1, getRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void get_whenUnknownRuleType_mapsTypeToUnknownAndKeepsOtherFields(WireMockRuntimeInfo wmInfo) {
        // given — the server returns a rule strategy introduced after this release
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UNKNOWN_TYPE_RULE_RESPONSE.formatted(TEST_RULE_ID, TEST_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().get(TEST_RULE_ID);

            // then — the unknown type degrades to UNKNOWN; the rest of the rule
            // (including its configuration oneOf) still maps
            assertEquals(PricingRuleType.UNKNOWN, rule.type());
            assertEquals(TEST_RULE_ID, rule.id());
            assertEquals(TEST_RULE_NAME, rule.name());
            assertInstanceOf(PricingRuleConfiguration.ChangeByPercentage.class, rule.configuration());
            verify(1, getRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void delete_whenRuleExists_sendsDeleteToRulePath(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(delete(urlEqualTo(RULE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NO_CONTENT)));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            allegro.pricing().automation().delete(TEST_RULE_ID);

            // then — the write happened exactly once at the rule path
            verify(1, deleteRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void create_when400_throwsBadRequestWithParsedFieldErrorsAndDoesNotRetry(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RULES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_BAD_REQUEST)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(VALIDATION_ERROR_RESPONSE.formatted(TEST_ERROR_CODE, TEST_ERROR_PATH))));

        try (AllegroClient allegro = client(wmInfo)) {
            PricingAutomation automation = allegro.pricing().automation();
            PricingRuleRequest request = percentageRule();

            // then — parsed field errors survive; a POST is not retried
            AllegroBadRequestException failure = assertThrows(
                    AllegroBadRequestException.class, () -> automation.create(request));
            assertEquals(1, failure.errors().size());
            assertEquals(TEST_ERROR_CODE, failure.errors().get(0).code());
            assertEquals(TEST_ERROR_PATH, failure.errors().get(0).path());
            verify(1, postRequestedFor(urlEqualTo(RULES_PATH)));
        }
    }

    @Test
    void get_when401Once_reauthenticatesAndReplaysWithFreshToken(WireMockRuntimeInfo wmInfo) {
        // given — token endpoint hands out token-one, then token-two
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN, EXPIRY_SECONDS)))
                .willSetStateTo(STATE_REAUTHED));
        stubFor(post(urlEqualTo(TestHttpConstants.TOKEN_PATH))
                .inScenario(SCENARIO_REPLAY).whenScenarioStateIs(STATE_REAUTHED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(TOKEN_RESPONSE.formatted(TEST_TOKEN_2, EXPIRY_SECONDS))));
        stubFor(get(urlEqualTo(RULE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_UNAUTHORIZED)));
        stubFor(get(urlEqualTo(RULE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEFAULT_RULE_RESPONSE.formatted(TEST_RULE_ID, DEFAULT_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().get(TEST_RULE_ID);

            // then — replayed once, second request carried the fresh token
            assertEquals(TEST_RULE_ID, rule.id());
            verify(2, getRequestedFor(urlEqualTo(RULE_PATH)));
            verify(1, getRequestedFor(urlEqualTo(RULE_PATH))
                    .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                            equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN_2)));
        }
    }

    @Test
    void get_when404_throwsNotFoundWithTraceId(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_NOT_FOUND)
                        .withHeader(TestHttpConstants.TRACE_ID_HEADER, TEST_TRACE_ID)
                        .withBody(NOT_FOUND_RESPONSE)));

        try (AllegroClient allegro = client(wmInfo)) {
            PricingAutomation automation = allegro.pricing().automation();

            // then
            AllegroNotFoundException failure = assertThrows(
                    AllegroNotFoundException.class, () -> automation.get(TEST_RULE_ID));
            assertEquals(TestHttpConstants.HTTP_NOT_FOUND, failure.statusCode());
            assertEquals(TEST_TRACE_ID, failure.traceId());
        }
    }

    @Test
    void get_when429_retriesThenThrowsRateLimitWithRetryAfter(WireMockRuntimeInfo wmInfo) {
        // given — every attempt is throttled; the policy allows one retry
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULE_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_TOO_MANY_REQUESTS)
                        .withHeader(TestHttpConstants.RETRY_AFTER_HEADER, String.valueOf(TEST_RETRY_AFTER))));

        try (AllegroClient allegro = client(wmInfo)) {
            PricingAutomation automation = allegro.pricing().automation();

            // then — retried once (verify 2), then surfaced with Retry-After
            AllegroRateLimitException failure = assertThrows(
                    AllegroRateLimitException.class, () -> automation.get(TEST_RULE_ID));
            assertEquals(TEST_RETRY_AFTER, failure.retryAfterSeconds());
            verify(2, getRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void get_when5xxThenOk_retriesAndSucceeds(WireMockRuntimeInfo wmInfo) {
        // given — first attempt 500, retry returns 200
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULE_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR))
                .willSetStateTo(STATE_RECOVERED));
        stubFor(get(urlEqualTo(RULE_PATH)).inScenario(SCENARIO_RETRY)
                .whenScenarioStateIs(STATE_RECOVERED)
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(DEFAULT_RULE_RESPONSE.formatted(TEST_RULE_ID, DEFAULT_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().get(TEST_RULE_ID);

            // then
            assertEquals(TEST_RULE_ID, rule.id());
            verify(2, getRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void create_when5xx_throwsServerExceptionAndDoesNotRetryPost(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(post(urlEqualTo(RULES_PATH))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_SERVER_ERROR)));

        try (AllegroClient allegro = client(wmInfo)) {
            PricingAutomation automation = allegro.pricing().automation();
            PricingRuleRequest request = percentageRule();

            // then — POST is not retried by default
            assertThrows(AllegroServerException.class, () -> automation.create(request));
            verify(1, postRequestedFor(urlEqualTo(RULES_PATH)));
        }
    }

    @Test
    void rules_whenMultipleRules_mapsAllIncludingDefaultRule(WireMockRuntimeInfo wmInfo) {
        // given — the collection carries one built-in default rule and one merchant rule
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(RULES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(RULES_LIST_RESPONSE.formatted(
                                DEFAULT_RULE_NAME, TEST_RULE_ID, TEST_RULE_NAME))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            List<PricingRule> rules = allegro.pricing().automation().rules();

            // then — both mapped, including the default rule with no configuration
            assertEquals(2, rules.size());
            assertTrue(rules.get(0).isDefault());
            assertNull(rules.get(0).configuration());
            assertEquals(TEST_RULE_ID, rules.get(1).id());
            assertInstanceOf(PricingRuleConfiguration.ChangeByPercentage.class,
                    rules.get(1).configuration());
            verify(1, getRequestedFor(urlEqualTo(RULES_PATH)));
        }
    }

    @Test
    void update_whenValidEdit_putsNameAndConfigWithoutTypeAndMapsResponse(WireMockRuntimeInfo wmInfo) {
        // given — the PUT body carries name + configuration but never the immutable type
        stubToken(TEST_TOKEN);
        stubFor(put(urlEqualTo(RULE_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                        equalTo(TestHttpConstants.VND_ALLEGRO_V1))
                .withRequestBody(matchingJsonPath("$.name", equalTo(UPDATED_RULE_NAME)))
                .withRequestBody(matchingJsonPath("$.configuration.changeByPercentage.value",
                        equalTo(UPDATED_PERCENTAGE)))
                .withRequestBody(notMatching("(?s).*\"type\".*"))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(UPDATED_RULE_RESPONSE.formatted(TEST_RULE_ID, UPDATED_RULE_NAME))));

        PricingRuleEdit edit = PricingRuleEdit.builder()
                .name(UPDATED_RULE_NAME)
                .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                        PricingRuleConfiguration.Operation.SUBTRACT, UPDATED_PERCENTAGE))
                .build();

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            PricingRule rule = allegro.pricing().automation().update(TEST_RULE_ID, edit);

            // then
            assertEquals(UPDATED_RULE_NAME, rule.name());
            PricingRuleConfiguration.ChangeByPercentage configuration = assertInstanceOf(
                    PricingRuleConfiguration.ChangeByPercentage.class, rule.configuration());
            assertEquals(UPDATED_PERCENTAGE, configuration.value());
            verify(1, putRequestedFor(urlEqualTo(RULE_PATH)));
        }
    }

    @Test
    void rulesOfOffer_whenAssigned_mapsAssignmentsAndPriceRange(WireMockRuntimeInfo wmInfo) {
        // given
        stubToken(TEST_TOKEN);
        stubFor(get(urlEqualTo(OFFER_RULES_PATH))
                .withHeader(TestHttpConstants.AUTHORIZATION_HEADER,
                        equalTo(TestHttpConstants.BEARER_PREFIX + TEST_TOKEN))
                .willReturn(aResponse().withStatus(TestHttpConstants.HTTP_OK)
                        .withBody(OFFER_RULES_RESPONSE.formatted(TEST_RULE_ID))));

        try (AllegroClient allegro = client(wmInfo)) {

            // when
            OfferPricingRules offerRules =
                    allegro.pricing().automation().rulesOfOffer(TEST_OFFER_ID);

            // then — the assignment, its rule id and the price band all survive mapping
            assertEquals(TEST_UPDATED_AT, offerRules.updatedAt());
            assertEquals(1, offerRules.rules().size());
            OfferRuleAssignment assignment = offerRules.rules().get(0);
            assertEquals(TEST_MARKETPLACE_ID, assignment.marketplaceId());
            assertEquals(TEST_RULE_ID, assignment.ruleId());
            OfferRulePriceRange priceRange = assignment.priceRange();
            assertNotNull(priceRange);
            assertEquals(OfferRulePriceRange.PriceRangeCurrency.BASE_MARKETPLACE_CURRENCY,
                    priceRange.currency());
            assertEquals(Money.of(MIN_PRICE_AMOUNT, TEST_CURRENCY), priceRange.minPrice());
            assertEquals(Money.of(MAX_PRICE_AMOUNT, TEST_CURRENCY), priceRange.maxPrice());
            verify(1, getRequestedFor(urlEqualTo(OFFER_RULES_PATH)));
        }
    }
}
