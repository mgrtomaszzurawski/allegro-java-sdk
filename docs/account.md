# Account & meta (`client.user()`, `client.marketplaces()`, `client.bidding()`, `client.charity()`, `client.affiliate()`)

Bucket D groups the account-level and platform-metadata endpoints: the
authenticated user and their sales reports and ratings, additional e-mail
addresses, marketplaces, auction bidding, charity search and affiliate
conversions.

Most of these need a **user-context** token (authorization-code or device
grant). The exceptions are `marketplaces()` (public — works with an app-only
client-credentials token) and `bidding()` (needs a **buyer** token).

## Current user & reports

```java
CurrentUser me = client.user().me();               // GET /me
me.login();                                         // public login name
me.baseMarketplaceId();                             // e.g. "allegro-pl", or null
CurrentUser.Company company = me.company();         // VAT data, or null (personal account)
if (company != null) {
    company.name();                                 // registered company name
    company.taxId();                                // tax id (NIP)
}

SalesQuality quality = client.user().salesQuality();          // GET /sale/quality
SmartClassification smart = client.user().smartClassification();          // GET /sale/smart
SmartClassification smartPl = client.user().smartClassification("allegro-pl");
```

`CurrentUser` carries the account's identity plus, for a business account, its
`company` (VAT registration — name and tax id) and the `baseMarketplaceId` it is
primarily registered on; both are `null` for a personal account that declares
none. `SalesQuality` exposes one `Day` per reported day (overall score, grade and
the component `Metric`s). `SmartClassification` tells you whether the account
qualifies for Smart!, when that last changed, and the per-`Condition` breakdown.

## Ratings

```java
UserRatings ratings = client.user().ratings();

// Lazy, paginated — pages fetched on demand as you consume the stream.
ratings.stream(RatingFilter.builder().recommended(false).build())
        .limit(50)
        .forEach(rating -> System.out.println(rating.buyer().login() + ": " + rating.comment()));

UserRating one = ratings.get(ratingId);                       // GET /sale/user-ratings/{id}
Answer answer = ratings.answer(ratingId,                      // PUT …/{id}/answer
        RatingAnswer.builder().message("Thank you!").build());
Removal removal = ratings.requestRemoval(ratingId,            // PUT …/{id}/removal
        RatingRemoval.builder().message("Buyer confirmed resolution").build());

UserRatingSummary summary = ratings.summaryOf(userId);        // GET /users/{userId}/ratings-summary (public)
```

The rating list has no total count, so the stream stops when a page returns
fewer than the page size. `RatingFilter` filters by `recommended` and a
last-changed date range; `RatingFilter.all()` matches everything.

## Additional e-mail addresses

```java
AdditionalEmails emails = client.user().additionalEmails();
List<AdditionalEmail> all = emails.list();          // GET    /account/additional-emails
AdditionalEmail one = emails.get(emailId);          // GET    …/{emailId}
AdditionalEmail added = emails.add("extra@example.com"); // POST …           (profile:write)
emails.delete(added.id());                          // DELETE …/{emailId}    (profile:write)
```

## Marketplaces

Public platform metadata — no user token required (see the top of this guide).

```java
for (Marketplace marketplace : client.marketplaces().list()) {   // GET /marketplaces
    System.out.println(marketplace.id() + " base=" + marketplace.baseCurrency()
            + " ships to " + marketplace.shippingCountries());
}
```

`Marketplace` flattens the API's nested language/currency/country objects to the
code strings you actually use: `id()`, `offerCreationLanguages()`,
`offerDisplayLanguages()`, `baseCurrency()` (nullable), `additionalCurrencies()`,
`shippingCountries()`. Collections are never `null`.

## Bidding (buyer side)

Needs a **buyer** user-context token (`bids` scope). Amounts use the SDK-wide
`Money` type.

```java
Bidding bidding = client.bidding();
MyBid current = bidding.myBid(offerId);                        // GET /bidding/offers/{offerId}/bid
MyBid placed = bidding.placeBid(offerId, Money.of("120.00", "PLN")); // PUT …/bid
```

`myBid` throws `AllegroNotFoundException` when the auction does not exist **or**
the user has not bid in it (the API returns 404 for both). `placeBid` maps a
422 ("bidding not allowed") to `AllegroBadRequestException`.

## Charity & affiliate (beta)

Both use Allegro's beta media type, handled internally.

```java
List<FundraisingCampaign> campaigns = client.charity().searchCampaigns(   // GET /charity/fundraising-campaigns
        CharitySearch.builder().phrase("children").limit(20).build());

client.affiliate().streamCpsConversions(                                  // GET /affiliate/conversions/cps
        ConversionFilter.builder().status(ConversionStatus.CONFIRMED).build())
        .forEach(conversion -> System.out.println(conversion.id() + " " + conversion.commission()));
```

`charity().searchCampaigns` requires a search `phrase` and a bounded `limit`
(1–100, default 100). `affiliate().streamCpsConversions` is a lazy stream over
CPS conversions filtered by order/modification dates and status; it needs the
`affiliate:read` scope. A conversion whose `status` a future Allegro release
introduces is read back as `ConversionStatus.UNKNOWN` rather than failing the
stream; passing `UNKNOWN` as a `status` filter simply omits the parameter.
