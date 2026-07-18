/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link BuyerBrowser#isDataDomeChallenge(String, List)} — the
 * pure predicate behind the session-validity and login checks. No browser, no
 * sandbox: it runs in {@code check} (untagged) and pins the exact rule that a
 * DataDome challenge page must NOT be mistaken for an authenticated one.
 *
 * <p>This is the regression guard for the false-positive fixed on 2026-07-18:
 * DataDome serves its challenge at the destination URL, so the old
 * "URL is not /logowanie" heuristic reported a challenge/blocked session as
 * valid. The predicate must return {@code true} for both DataDome tiers and
 * {@code false} only for a genuine, challenge-free page.
 */
class BuyerBrowserChallengeDetectionTest {

    private static final String CAPTCHA_FRAME_URL =
            "https://geo.captcha-delivery.com/captcha/?initialCid=abc";
    private static final String ORDINARY_FRAME_URL =
            "https://allegro.pl.allegrosandbox.pl/moje-allegro/moje-dane";
    private static final String CHALLENGE_PAGE_HTML =
            "<html><body><h1>Potwierdź, że jesteś człowiekiem.</h1></body></html>";
    private static final String CHALLENGE_PAGE_HTML_UPPERCASE =
            "<html><body><h1>POTWIERDŹ, ŻE JESTEŚ CZŁOWIEKIEM.</h1></body></html>";
    private static final String ACCOUNT_PAGE_HTML =
            "<html><body><h1>Moje dane</h1><form id=\"login-data\"></form></body></html>";

    @Test
    void isDataDomeChallenge_whenAnyFrameFromCaptchaHost_returnsTrue() {
        // given — the puzzle CAPTCHA renders in a captcha-delivery.com iframe,
        // while the host page has no marker text of its own
        // when / then
        assertTrue(BuyerBrowser.isDataDomeChallenge(
                ACCOUNT_PAGE_HTML, List.of(ORDINARY_FRAME_URL, CAPTCHA_FRAME_URL)));
    }

    @Test
    void isDataDomeChallenge_whenHostPageCarriesMarkerText_returnsTrue() {
        // given — the JS interstitial has no captcha iframe yet, only the marker
        // when / then
        assertTrue(BuyerBrowser.isDataDomeChallenge(
                CHALLENGE_PAGE_HTML, List.of(ORDINARY_FRAME_URL)));
    }

    @Test
    void isDataDomeChallenge_whenMarkerTextInUpperCase_returnsTrue() {
        // given — the match must be case-insensitive
        // when / then
        assertTrue(BuyerBrowser.isDataDomeChallenge(
                CHALLENGE_PAGE_HTML_UPPERCASE, List.of(ORDINARY_FRAME_URL)));
    }

    @Test
    void isDataDomeChallenge_whenGenuineAccountPage_returnsFalse() {
        // given — a real, challenge-free account page (the case the old URL-only
        // heuristic false-positived on)
        // when / then
        assertFalse(BuyerBrowser.isDataDomeChallenge(
                ACCOUNT_PAGE_HTML, List.of(ORDINARY_FRAME_URL)));
    }

    @Test
    void isDataDomeChallenge_whenNoFramesAndNoMarker_returnsFalse() {
        // given — no frames at all, no marker
        // when / then
        assertFalse(BuyerBrowser.isDataDomeChallenge(ACCOUNT_PAGE_HTML, List.of()));
    }
}
