# Pricing

Seller pricing tools, reached via `client.pricing()`. This guide covers automatic
pricing rules, fee preview, fee quotes, turnover discounts and deposit types.
Rebate promotions follow in a later release.

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

preview.commissions().forEach(c -> System.out.println(c.name() + ": " + c.feeAmount()));
preview.quotes().forEach(q -> System.out.println(q.name() + " every " + q.cycleDuration()));
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
