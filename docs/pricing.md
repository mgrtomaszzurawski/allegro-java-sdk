# Pricing

Seller pricing tools, reached via `client.pricing()`. This guide covers automatic
pricing rules, fee preview, fee quotes, turnover discounts, deposit types and
rebate promotions.

## Automatic pricing rules

An automatic pricing rule is a reusable strategy that keeps an offer's price in
step with the market — following the lowest price on Allegro, the lowest price on
the market, the current top offer, or converting from the base marketplace using
the exchange rate. A rule may optionally adjust the followed price by a fixed
amount (per currency) or by a percentage.

Reach the rules through `client.pricing().automation()`.

### Create a rule

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRule;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleConfiguration;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleType;

PricingRule rule = client.pricing().automation().create(
        PricingRuleRequest.builder()
                .name("Follow Allegro minus 5%")            // required, max 33 characters
                .type(PricingRuleType.FOLLOW_BY_ALLEGRO_MIN_PRICE)
                .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                        PricingRuleConfiguration.Operation.SUBTRACT, "5"))
                .build());

String ruleId = rule.id();
```

A fixed-amount adjustment uses `ChangeByAmount` with one `Money` per marketplace
currency:

```java
PricingRuleRequest.builder()
        .name("Market min minus 2 PLN")
        .type(PricingRuleType.FOLLOW_BY_MARKET_MIN_PRICE)
        .configuration(new PricingRuleConfiguration.ChangeByAmount(
                PricingRuleConfiguration.Operation.SUBTRACT,
                java.util.List.of(Money.of("2.00", "PLN"))))
        .build();
```

The builder validates fail-fast: a missing name or type, or a name longer than
33 characters, throws `IllegalStateException` before any request is sent.

### List, read, edit and delete

```java
import java.util.List;

List<PricingRule> rules = client.pricing().automation().rules();   // incl. built-in defaults

PricingRule fetched = client.pricing().automation().get(ruleId);

if (fetched.configuration() instanceof PricingRuleConfiguration.ChangeByPercentage percentage) {
    System.out.println(percentage.operation() + " " + percentage.value() + "%");
}

client.pricing().automation().delete(ruleId);   // built-in default rules cannot be deleted
```

A rule's `type` is fixed at creation, so an edit carries only the name and
configuration — build it with `PricingRuleEdit`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PricingRuleEdit;

PricingRule updated = client.pricing().automation().update(ruleId,
        PricingRuleEdit.builder()
                .name("Follow Allegro minus 8%")
                .configuration(new PricingRuleConfiguration.ChangeByPercentage(
                        PricingRuleConfiguration.Operation.SUBTRACT, "8"))
                .build());
```

`PricingRule.configuration()` is `null` for built-in default rules and pure
follow rules. `PricingRule.isDefault()` distinguishes the built-in defaults from
rules you created.

A rule strategy introduced by Allegro after this SDK release is surfaced as
`PricingRuleType.UNKNOWN` (rather than failing the read); it is a read-only
value and cannot be used to create or edit a rule.

### Rules assigned to an offer

Defining and editing rules lives here in `pricing()`. Applying a rule to a set of
offers in bulk is a batch-offer command on the offers facade, not this
sub-facade. To read which rules are currently assigned to one offer, per
marketplace, use `rulesOfOffer`:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferPricingRules;

OfferPricingRules assigned = client.pricing().automation().rulesOfOffer(offerId);
assigned.rules().forEach(rule ->
        System.out.println(rule.marketplaceId() + " -> " + rule.ruleId()));
```

### Scopes

Reading rules requires `sale:offers:read`; creating, editing and deleting them
require `sale:offers:write`.

## Fee preview

Preview the fees a draft offer would incur before publishing it — the one-off
sale commission and any recurring quotes — for a given category and Buy Now
price:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.FeePreview;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferFeePreviewRequest;

FeePreview preview = client.pricing().feePreview(
        OfferFeePreviewRequest.builder()
                .categoryId("257")
                .price(Money.of("99.99", "PLN"))
                .build());

preview.commissions().forEach(commission ->
        System.out.println(commission.name() + ": " + commission.feeAmount()));
preview.quotes().forEach(quote ->
        System.out.println(quote.name() + " every " + quote.cycleDuration()));
```

Pass `.offerId(...)` to preview the fees for an existing offer. The preview needs
`sale:offers:read`.

## Fee quotes

The recurring fees your offers are currently subject to (for example promoted-
listing quotes). Pass one or more offer ids; each is sent as a repeated
`offer.id` filter:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferQuote;

List<OfferQuote> quotes = client.pricing().quotes(List.of("12345", "67890"));
```

Fee quotes require the `billing:read` scope.

## Turnover discounts

Reward a buyer's cumulated turnover with a percentage discount, configured per
marketplace. Turnover discounts require a company seller account.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscount;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverDiscountRequest;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.TurnoverThreshold;

TurnoverDiscount discount = client.pricing().turnoverDiscounts().set("allegro-pl",
        TurnoverDiscountRequest.builder()
                .addThreshold(new TurnoverThreshold(Money.of("1000.00", "PLN"), "5"))
                .build());

List<TurnoverDiscount> all = client.pricing().turnoverDiscounts().list();
client.pricing().turnoverDiscounts().deactivate("allegro-pl");
```

## Deposit types

The deposit types the seller can attach to offers (for example returnable
packaging or bottle deposits):

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.DepositType;

List<DepositType> depositTypes = client.pricing().depositTypes();
```

## Rebate promotions

Rebate promotions reward buyers with a discount — a large-order discount, a
quantity-tiered wholesale price list, or a "buy several, get some cheaper"
multipack. Reach them through `client.pricing().promotions()`. Creating a
promotion requires the seller's base marketplace to be `allegro-pl`.

### List promotions

The list is filtered by promotion type (required) and paginated; the SDK exposes
it as a lazy `Stream` that fetches one page at a time as you consume it — only
what you materialise is held in memory.

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Promotion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionType;

client.pricing().promotions()
        .streamPromotions(PromotionType.LARGE_ORDER_DISCOUNT)
        .limit(50)
        .forEach(promotion -> System.out.println(promotion.id() + " " + promotion.status()));
```

Pass an offer id to list only the promotions that apply to one offer:

```java
client.pricing().promotions()
        .streamPromotions(PromotionType.WHOLESALE_PRICE_LIST, "12345")
        .forEach(promotion -> System.out.println(promotion.id()));
```

### Read one promotion

A promotion's `benefits()` are a sealed hierarchy — match on the concrete type:

```java
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.Benefit;

Promotion promotion = client.pricing().promotions().get(promotionId);

for (Benefit benefit : promotion.benefits()) {
    if (benefit instanceof Benefit.LargeOrderDiscount large) {
        large.thresholds().forEach(tier ->
                System.out.println("from " + tier.orderValueFrom() + " → " + tier.discountPercentage() + "%"));
    } else if (benefit instanceof Benefit.MultiPackDiscount multi) {
        System.out.println("buy " + multi.buyQuantity() + ", " + multi.discountedQuantity()
                + " at " + multi.discountPercentage() + "% off");
    } else if (benefit instanceof Benefit.WholesalePriceList wholesale) {
        System.out.println(wholesale.name());
    }
}
```

A benefit family introduced by Allegro after this SDK release is surfaced as
`Benefit.UnknownBenefit` (carrying the wire type) rather than failing the read.

### Create, modify and deactivate

Build the benefits and pick which offers they apply to. A criterion is either an
explicit set of offers, every offer (`allOffers()`), or offers assigned
externally (`assignedExternally()`).

```java
import io.github.mgrtomaszzurawski.allegro.sdk.core.Money;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.OfferCriterion;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.pricing.model.PromotionRequest;
import java.util.List;

Promotion created = client.pricing().promotions().create(
        PromotionRequest.builder()
                .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                        new Benefit.OrderValueThreshold(Money.of("100.00", "PLN"), "10"))))
                .addOfferCriterion(OfferCriterion.containing(List.of("12345")))
                .build());

client.pricing().promotions().modify(created.id(),
        PromotionRequest.builder()
                .addBenefit(new Benefit.LargeOrderDiscount(List.of(
                        new Benefit.OrderValueThreshold(Money.of("150.00", "PLN"), "12"))))
                .addOfferCriterion(OfferCriterion.allOffers())
                .build());

client.pricing().promotions().deactivate(created.id());
```

A promotion needs at least one benefit and at least one offer criterion; the
builder fails fast otherwise. Reading promotions requires `sale:offers:read`;
creating, modifying and deactivating them require `sale:offers:write`.
