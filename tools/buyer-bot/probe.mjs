/*
 * Sandbox buyer-side automation PROBE (experiment).
 *
 * Question it answers: can a headless browser reach and use the Allegro SANDBOX
 * web UI as the buyer, or does anti-bot (DataDome-class) block it? The verdict
 * decides whether buyer-side flows (device-flow consent click, buy-now,
 * disputes) are automatable or need one-time manual seeding by the operator.
 *
 * Credentials come from the environment only (never hardcoded, never printed):
 *   ALLEGRO_SANDBOX_BUYER_LOGIN / ALLEGRO_SANDBOX_BUYER_PASSWORD
 */
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';

const SANDBOX_BASE = 'https://allegro.pl.allegrosandbox.pl';
const LOGIN_URL = `${SANDBOX_BASE}/logowanie`;
const OUT_DIR = new URL('./probe-output/', import.meta.url).pathname;
const USER_AGENT =
  'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) ' +
  'Chrome/149.0.0.0 Safari/537.36';

// Markers that mean an anti-bot wall rather than the real page.
const ANTIBOT_MARKERS = [
  'captcha-delivery.com',
  'datadome',
  'geo.captcha',
  'Access Denied',
  'access to this page has been denied',
  'zweryfikuj, że nie jesteś robotem',
  'unusual traffic',
  'nietypowy ruch',
];

function log(step, detail) {
  console.log(`[probe] ${step}${detail ? ': ' + detail : ''}`);
}

async function main() {
  const login = process.env.ALLEGRO_SANDBOX_BUYER_LOGIN;
  const password = process.env.ALLEGRO_SANDBOX_BUYER_PASSWORD;
  if (!login || !password) {
    console.log('MISSING_CREDS: source /workspace/shared/secrets/allegro-sandbox.env first');
    process.exit(2);
  }
  mkdirSync(OUT_DIR, { recursive: true });

  const headless = process.env.HEADLESS !== 'false';
  log('mode', headless ? 'headless' : 'headed (xvfb)');
  const browser = await chromium.launch({
    headless,
    args: [
      '--disable-blink-features=AutomationControlled',
      '--no-sandbox',
      '--disable-dev-shm-usage',
    ],
  });
  const context = await browser.newContext({
    userAgent: USER_AGENT,
    locale: 'pl-PL',
    viewport: { width: 1366, height: 900 },
    timezoneId: 'Europe/Warsaw',
  });
  // Mask the most obvious headless tell.
  await context.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => false });
  });
  const page = await context.newPage();

  const verdict = { reachedLogin: false, antibot: false, loginFormPresent: false, loggedIn: false };
  try {
    log('navigate', LOGIN_URL);
    let response = await page.goto(LOGIN_URL, { waitUntil: 'domcontentloaded', timeout: 45000 });
    let status = response ? response.status() : 0;
    await page.waitForTimeout(3500); // let any JS challenge render
    // DataDome serves a JS interstitial (429/"enable JS") that computes a cookie
    // and expects a reload; a real browser clears it transparently. Give it that
    // chance: if the first hit was a challenge, wait for the cookie then reload.
    if (status === 429 || status === 403) {
      log('challenge', 'waiting for JS challenge to settle, then reloading');
      await page.waitForTimeout(9000);
      response = await page.reload({ waitUntil: 'domcontentloaded', timeout: 45000 }).catch(() => response);
      status = response ? response.status() : status;
      await page.waitForTimeout(4000);
    }
    const url = page.url();
    const title = await page.title().catch(() => '');
    const bodyText = (await page.textContent('body').catch(() => '') || '').toLowerCase();
    log('http-status', String(status));
    log('final-url', url);
    log('title', JSON.stringify(title));

    const hit = ANTIBOT_MARKERS.find((marker) => bodyText.includes(marker.toLowerCase())
      || url.toLowerCase().includes(marker.toLowerCase()));
    // Content-based detection: after the challenge settles we may land on the
    // REAL Allegro login page even if the captured document status is odd, so
    // trust the rendered content, not just the HTTP code.
    const loginPageMarkers = ['moje allegro', 'zaloguj', 'logowanie'];
    const onRealLoginPage = loginPageMarkers.some((m) => title.toLowerCase().includes(m));
    verdict.status = status;
    verdict.antibot = Boolean(hit) && !onRealLoginPage;
    verdict.throttled = status === 429 && !onRealLoginPage;
    verdict.reachedLogin = onRealLoginPage || (status >= 200 && status < 400 && !verdict.antibot);
    if (hit) log('antibot-marker', hit);
    log('on-real-login-page', String(onRealLoginPage));
    log('body-snippet', JSON.stringify(bodyText.replace(/\s+/g, ' ').slice(0, 200)));

    // Enumerate every input so we learn Allegro's actual login selectors.
    const inputs = await page.$$eval('input', (nodes) => nodes.slice(0, 15).map((n) => ({
      name: n.getAttribute('name'), id: n.id, type: n.getAttribute('type'),
      autocomplete: n.getAttribute('autocomplete'),
    })));
    log('inputs-found', JSON.stringify(inputs));

    // A RODO/cookie consent modal overlays the login form and swallows the
    // submit click — dismiss it first.
    const consentSelector = 'button[data-role="accept-consent"]';
    if (await page.locator(consentSelector).count() > 0) {
      await page.locator(consentSelector).first().click().catch(() => {});
      await page.waitForTimeout(1500);
      log('consent', 'accepted RODO/cookie modal');
    }

    await page.screenshot({ path: OUT_DIR + '01-landing.png', fullPage: false });

    // Is a real login form present? Allegro's login has evolved; try a few selectors.
    const usernameSelectors = ['#login', 'input[name="login"]', '#username',
      'input[name="username"]', 'input[type="email"]'];
    let usernameField = null;
    for (const selector of usernameSelectors) {
      if (await page.locator(selector).count() > 0) { usernameField = selector; break; }
    }
    verdict.loginFormPresent = Boolean(usernameField);
    log('login-form-present', String(verdict.loginFormPresent) + (usernameField ? ` (${usernameField})` : ''));

    if (verdict.loginFormPresent && !verdict.antibot) {
      log('attempt-login', 'filling credentials (values never printed)');
      await page.fill(usernameField, login);
      const pwdSelectors = ['#password', 'input[name="password"]', 'input[type="password"]'];
      let pwdField = null;
      for (const selector of pwdSelectors) {
        if (await page.locator(selector).count() > 0) { pwdField = selector; break; }
      }
      if (pwdField) {
        await page.fill(pwdField, password);
        // Allegro's submit is a button with a click handler, not an Enter submit.
        const buttons = await page.$$eval('button', (nodes) => nodes.slice(0, 12).map((n) => ({
          type: n.getAttribute('type'), text: (n.textContent || '').trim().slice(0, 30),
          dataRole: n.getAttribute('data-role'),
        })));
        log('buttons-found', JSON.stringify(buttons));
        const submitSelectors = ['button[data-role="login-button"]', 'button[type="submit"]',
          'button:has-text("Zaloguj")'];
        let clicked = false;
        for (const selector of submitSelectors) {
          if (await page.locator(selector).count() > 0) {
            await page.locator(selector).first().click().catch(() => {});
            log('submit', 'clicked ' + selector);
            clicked = true;
            break;
          }
        }
        if (!clicked) { await page.keyboard.press('Enter'); log('submit', 'Enter fallback'); }
        await page.waitForLoadState('networkidle', { timeout: 20000 }).catch(() => {});
        await page.waitForTimeout(4000);
        const afterUrl = page.url();
        const afterTitle = (await page.title().catch(() => '')).toLowerCase();
        const afterBody = (await page.textContent('body').catch(() => '') || '').toLowerCase();
        const afterAntibot = ANTIBOT_MARKERS.some((m) => afterBody.includes(m.toLowerCase()));
        const badCreds = afterBody.includes('nieprawidłow') || afterBody.includes('błędn');
        // Success signals: Allegro drops us onto a post-login interstitial
        // (e.g. /logowanie/aktualizacja-danych/telefon — "add phone") or the
        // authenticated homepage. The bare login form is /logowanie exactly.
        const pathOnly = afterUrl.replace(/\?.*/, '');
        const onBareLogin = pathOnly.endsWith('/logowanie');
        const postLoginInterstitial = afterUrl.includes('/aktualizacja-danych')
          || afterUrl.includes('origin_url');
        verdict.loggedIn = (postLoginInterstitial || !onBareLogin) && !afterAntibot && !badCreds;
        verdict.antibot = verdict.antibot || afterAntibot;
        verdict.badCreds = badCreds;
        log('post-login-url', afterUrl);
        log('post-login-title', JSON.stringify(afterTitle));
        await page.screenshot({ path: OUT_DIR + '02-after-login.png', fullPage: false });
      } else {
        log('attempt-login', 'no password field found (unexpected multi-step flow)');
      }
    }
  } catch (err) {
    log('error', err && err.message ? err.message : String(err));
  } finally {
    await browser.close();
  }

  console.log('\n=== PROBE VERDICT ===');
  console.log(JSON.stringify(verdict, null, 2));
  if (verdict.antibot) {
    console.log('RESULT: anti-bot blocked headless — fallback to manual operator seeding.');
  } else if (verdict.loggedIn) {
    console.log('RESULT: headless login SUCCEEDED — buyer-side automation is viable.');
  } else if (verdict.reachedLogin) {
    console.log('RESULT: reached the login page (no anti-bot wall) but did not confirm sign-in — flow needs tuning.');
  } else {
    console.log('RESULT: could not reach the login page — inconclusive (network or unexpected response).');
  }
}

main();
