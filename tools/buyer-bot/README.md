# buyer-bot — sandbox buyer-side automation (experiment, PROVEN)

Standalone Node tooling (NOT part of the Gradle build or the published SDK). Automates the
Allegro **sandbox** web UI as the buyer to seed flows the REST API cannot do: the device-flow
consent click for a buyer token, and (next) buy-now / disputes / ratings.

## Status — viable (2026-07-17)

Headless is blocked by DataDome (HTTP 403 + `captcha-delivery.com`). **Headed full Chromium
under Xvfb passes** and logs in successfully. Verdict from `probe.mjs`:
`{ reachedLogin: true, antibot: false, loginFormPresent: true, loggedIn: true }`.

## The recipe that beats DataDome

1. **Full Chromium, not headless-shell** — `chromium.launch({ headless: false, args: [
   '--no-sandbox', '--disable-dev-shm-usage', '--disable-blink-features=AutomationControlled' ] })`
   under a virtual display (`Xvfb :99 -screen 0 1366x900x24`, `DISPLAY=:99`).
2. **Mask the headless tell** — `navigator.webdriver = false` via `addInitScript`; realistic
   `userAgent`, `locale: 'pl-PL'`, `timezoneId: 'Europe/Warsaw'`.
3. **Clear the DataDome JS challenge** — the first navigation returns a 403/429 "please enable
   JS" interstitial. Wait ~9 s for its cookie, then `page.reload()`. It clears (the URL gains
   `?dd_referrer=`) and the real login page renders.
4. **Dismiss the RODO/cookie modal** — `button[data-role="accept-consent"]` ("Zgadzam się"); it
   overlays the form and swallows the submit click otherwise.
5. **Log in** — fill `#login` and `#password` (Allegro uses `login`, NOT `username`), click
   `button[type="submit"]` ("Zaloguj się"). Success lands on the post-login phone-update
   interstitial ("Pomóż nam chronić Twoje konto", skippable via "MOŻE PÓŹNIEJ").

`xvfb-run` needs `xauth` (absent here) — start Xvfb manually instead (see below).

## Run

```bash
cd tools/buyer-bot
npm install playwright
npx playwright install chromium
set -a; . /workspace/shared/secrets/allegro-sandbox.env; set +a   # buyer creds via env only
Xvfb :99 -screen 0 1366x900x24 >/tmp/xvfb.log 2>&1 &
DISPLAY=:99 HEADLESS=false node probe.mjs                          # headed (works)
# HEADLESS=true node probe.mjs                                     # headless (blocked — for comparison)
```

Credentials (`ALLEGRO_SANDBOX_BUYER_LOGIN` / `_PASSWORD`) are read from the environment only —
never hardcoded, never printed. `node_modules/` and `probe-output/` are gitignored.

## Next (productization)

- Device-flow consent click → mint the buyer refresh token into
  `/workspace/shared/secrets/allegro-sandbox-tokens.properties` (unblocks buckets D/J).
- Buy-now seeding → one sandbox order for bucket B's read-side, disputes for bucket J.

Caveat: from a datacenter IP DataDome is stricter; if it hardens, the documented fallback is
one-time manual seeding by the operator.
