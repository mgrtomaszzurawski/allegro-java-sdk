/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.MyBid;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live bidding (buyer-side) E2E — verifies bucket D's {@code bidding()} facade
 * end-to-end with a <strong>real buyer token</strong> minted through the
 * automated device-flow consent click ({@link BuyerAuthentication}), the token
 * flow the REST API alone cannot bootstrap.
 *
 * <p><strong>Not part of {@code check}.</strong> Needs a real browser under Xvfb,
 * the live sandbox, and buyer credentials, so it runs only with {@code -Pe2e}. A
 * bootstrapped buyer {@code storageState} must already exist (run
 * {@code :allegro-e2e:run} once) so the session is reused instead of a fresh
 * login (which from a datacenter IP risks DataDome's block).
 *
 * <pre>
 *   set -a; . /workspace/shared/secrets/allegro-sandbox.env; set +a
 *   Xvfb :99 -screen 0 1366x900x24 &gt;/tmp/xvfb.log 2&gt;&amp;1 &amp;
 *   DISPLAY=:99 ./gradlew :allegro-e2e:test -Pe2e --tests '*BiddingE2ETest'
 * </pre>
 *
 * <h2>What is asserted</h2>
 * <ul>
 *   <li>Always: the buyer token is minted/reused and authenticates, and a
 *       {@code myBid} on a non-existent auction returns {@link
 *       AllegroNotFoundException} — proving the buyer-scoped bidding read path
 *       reaches the live API through a real token.</li>
 *   <li>Opt-in ({@code ALLEGRO_SANDBOX_AUCTION_OFFER_ID}
 *       + {@code ALLEGRO_SANDBOX_AUCTION_BID_AMOUNT}): a full
 *       {@code placeBid}&rarr;{@code myBid} write&rarr;read round-trip against a
 *       real sandbox auction. Gated behind env so the test never places a live
 *       bid by accident.</li>
 * </ul>
 */
@Tag("e2e")
class BiddingE2ETest {

    private static final String CLIENT_ID_ENV = "ALLEGRO_SANDBOX_CLIENT_ID";
    private static final String CLIENT_SECRET_ENV = "ALLEGRO_SANDBOX_CLIENT_SECRET";
    private static final String AUCTION_OFFER_ID_ENV = "ALLEGRO_SANDBOX_AUCTION_OFFER_ID";
    private static final String AUCTION_BID_AMOUNT_ENV = "ALLEGRO_SANDBOX_AUCTION_BID_AMOUNT";
    private static final String CURRENCY_PLN = "PLN";
    /** Numeric-shaped offer id that does not exist — {@code myBid} must 404. */
    private static final String NONEXISTENT_OFFER_ID = "0000000000";

    @Test
    void bidding_withMintedBuyerToken_reachesLiveApiAndRoundTrips() {
        // given — a bootstrapped buyer session (fail loudly rather than logging in
        // fresh, which risks DataDome from a datacenter IP)
        var storageStatePath = BuyerCredentials.storageStatePath();
        assertTrue(Files.exists(storageStatePath),
                "run :allegro-e2e:run once to bootstrap the buyer storageState first");
        String clientId = requiredEnv(CLIENT_ID_ENV);
        String clientSecret = requiredEnv(CLIENT_SECRET_ENV);

        // when — open the buyer browser and mint a buyer token via device-flow consent
        try (BuyerBrowser browser = BuyerBrowser.authenticated(
                        BuyerCredentials.fromEnv(), storageStatePath);
                AllegroClient buyer = BuyerAuthentication.authenticatedBuyer(
                        browser, clientId, clientSecret)) {

            // then — the buyer-scoped bidding read path reaches the live API: an
            // auction that does not exist maps to AllegroNotFoundException (the API
            // returns 404 for both "no auction" and "no bid" — see Bidding.myBid)
            assertThrows(AllegroNotFoundException.class,
                    () -> buyer.bidding().myBid(NONEXISTENT_OFFER_ID),
                    "myBid on a non-existent auction should map the 404 to AllegroNotFoundException");

            // and — an optional full write→read against a real sandbox auction
            verifyLiveAuctionRoundTrip(buyer);
        }
    }

    /**
     * Full {@code placeBid}&rarr;{@code myBid} round-trip, only when a real auction
     * offer id and a bid amount are configured; otherwise a no-op (the always-on
     * assertions above already prove the buyer-scoped path).
     */
    private static void verifyLiveAuctionRoundTrip(AllegroClient buyer) {
        String auctionOfferId = System.getenv(AUCTION_OFFER_ID_ENV);
        String bidAmount = System.getenv(AUCTION_BID_AMOUNT_ENV);
        if (auctionOfferId == null || bidAmount == null) {
            System.out.println("Live auction write→read skipped: set " + AUCTION_OFFER_ID_ENV
                    + " and " + AUCTION_BID_AMOUNT_ENV + " to exercise placeBid→myBid.");
            return;
        }

        // when — place a proxy bid, then read it back
        Money maxAmount = Money.of(bidAmount, CURRENCY_PLN);
        MyBid placed = buyer.bidding().placeBid(auctionOfferId, maxAmount);
        MyBid read = buyer.bidding().myBid(auctionOfferId);

        // then — the write is reflected in the read
        assertNotNull(placed.currentPrice(), "placeBid should return the auction's current price");
        assertEquals(placed.maxAmount(), read.maxAmount(),
                "myBid should read back the maxAmount placed by placeBid");
        System.out.println("Bidding write→read verified on auction " + auctionOfferId
                + ": highBidder=" + read.highBidder());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        assertNotNull(value, "missing env var " + name
                + " - source /workspace/shared/secrets/allegro-sandbox.env first");
        return value;
    }
}
