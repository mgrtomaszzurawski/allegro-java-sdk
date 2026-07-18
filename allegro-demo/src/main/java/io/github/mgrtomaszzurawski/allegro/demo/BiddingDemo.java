/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.demo;

import io.github.mgrtomaszzurawski.allegro.sdk.AllegroClient;
import io.github.mgrtomaszzurawski.allegro.sdk.config.AllegroEnvironment;
import io.github.mgrtomaszzurawski.allegro.sdk.config.credentials.DeviceCodeCredentials;
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.CurrentUser;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.account.model.MyBid;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroException;
import io.github.mgrtomaszzurawski.allegro.sdk.exception.AllegroNotFoundException;
import java.io.IOException;

/**
 * Bucket D bidding probe (TESTING.md §2) — the buyer half of the marketplace.
 * Runs against the SANDBOX with a stored <strong>buyer</strong> user token, so
 * invoke it with {@code -Pdemo.account=buyer}:
 *
 * <pre>
 *   ./gradlew :allegro-demo:run -Pdemo.scenario=bidding -Pdemo.account=buyer \
 *       [-Pdemo.offerId=&lt;auctionOfferId&gt; [-Pdemo.bidAmount=&lt;amount&gt;]]
 * </pre>
 *
 * <p>The buyer token is minted once by a human via {@code auth-bootstrap
 * -Pdemo.account=buyer} (the device-flow consent page is DataDome-fronted, so it
 * is not automated from a datacenter IP — see {@code KNOWN-SERVER-BEHAVIORS.md});
 * every later run reuses the rotation-safe stored token. The read probe always
 * runs; {@code placeBid} needs an explicit live auction offer id, so it never
 * places a bid by accident. Status-level output only — never tokens or bodies.
 */
public final class BiddingDemo {

    static final String SCENARIO = "bidding";

    private static final String OFFER_ID_PROPERTY = "demo.offerId";
    private static final String BID_AMOUNT_PROPERTY = "demo.bidAmount";
    private static final String CURRENCY_PLN = "PLN";
    /** Numeric-shaped offer id that does not exist — {@code myBid} must 404. */
    private static final String NONEXISTENT_OFFER_ID = "0000000000";
    private static final String STORED_TOKEN_EXPIRED =
            "(stored token expired - rerun auth-bootstrap -Pdemo.account=buyer)";

    private BiddingDemo() {
    }

    static void run(String clientId, String clientSecret, String account) throws IOException {
        SharedTokenStore tokenStore = new SharedTokenStore();
        String storedRefreshToken = tokenStore.load(account);
        if (storedRefreshToken == null) {
            System.out.println("No stored refresh token for '" + account + "' - bidding needs a "
                    + "BUYER token; run auth-bootstrap -Pdemo.account=buyer first");
            return;
        }
        DeviceCodeCredentials credentials = DeviceCodeCredentials.ofRefreshToken(
                clientId, clientSecret,
                ignored -> System.out.println(STORED_TOKEN_EXPIRED),
                storedRefreshToken);
        try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.SANDBOX)) {
            CurrentUser buyer = client.user().me();
            System.out.println("me(): login=" + buyer.login() + ", id=" + buyer.id());
            String rotated = client.refreshToken();
            if (rotated != null) {
                tokenStore.store(account, rotated);
            }
            biddingProbes(client);
        }
    }

    private static void biddingProbes(AllegroClient client) {
        // Read path always: a non-existent auction maps 404 -> AllegroNotFoundException,
        // proving the buyer-scoped bidding endpoint is reachable with this token.
        try {
            client.bidding().myBid(NONEXISTENT_OFFER_ID);
            System.out.println("myBid(nonexistent): unexpectedly returned a bid");
        } catch (AllegroNotFoundException expected) {
            System.out.println("myBid(nonexistent): 404 -> AllegroNotFoundException "
                    + "(buyer bidding read path reaches the live API)");
        }

        String offerId = System.getProperty(OFFER_ID_PROPERTY);
        if (offerId == null) {
            System.out.println("Live auction probe skipped: pass -Pdemo.offerId=<auctionOfferId> "
                    + "[-Pdemo.bidAmount=<amount>] to exercise myBid / placeBid.");
            return;
        }
        auctionRoundTrip(client, offerId);
    }

    private static void auctionRoundTrip(AllegroClient client, String offerId) {
        try {
            MyBid current = client.bidding().myBid(offerId);
            System.out.println("myBid(" + offerId + "): highBidder=" + current.highBidder()
                    + ", currentPrice=" + formatMoney(current.currentPrice()));
        } catch (AllegroNotFoundException noBid) {
            // The API returns 404 for both "no auction" and "no bid yet" (see Bidding.myBid).
            System.out.println("myBid(" + offerId + "): 404 - no auction, or no bid placed yet");
        }

        String bidAmount = System.getProperty(BID_AMOUNT_PROPERTY);
        if (bidAmount == null) {
            System.out.println("placeBid skipped: pass -Pdemo.bidAmount=<amount> to place a "
                    + "proxy bid (write->read).");
            return;
        }
        try {
            MyBid placed = client.bidding().placeBid(offerId, Money.of(bidAmount, CURRENCY_PLN));
            MyBid readBack = client.bidding().myBid(offerId);
            System.out.println("placeBid->myBid: maxAmount=" + formatMoney(placed.maxAmount())
                    + ", readBack highBidder=" + readBack.highBidder()
                    + ", roundTripMatch=" + sameMoney(placed.maxAmount(), readBack.maxAmount()));
        } catch (AllegroException rejected) {
            // A bid below the minimum, a closed auction, etc. — report the wire
            // outcome as a status line rather than aborting with a stack trace.
            System.out.println("placeBid rejected: " + rejected.getClass().getSimpleName()
                    + " (" + rejected.statusCode() + ")");
        }
    }

    /** Value equality on amount + currency, tolerant of trailing-zero normalisation
     * ("10" vs "10.00") that the raw-string {@code Money.equals} would miss. */
    private static boolean sameMoney(Money left, Money right) {
        return left.currency().equals(right.currency())
                && left.amountAsDecimal().compareTo(right.amountAsDecimal()) == 0;
    }

    private static String formatMoney(Money value) {
        return value.amount() + " " + value.currency();
    }
}
