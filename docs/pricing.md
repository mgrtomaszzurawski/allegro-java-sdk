# Pricing

Seller pricing tools, reached via `client.pricing()`. This guide covers the
automatic pricing rules shipped in the starter slice; fee preview, quotes,
promotions, turnover discounts and deposit types follow in the bucket's volume
release.

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

### Read and delete

```java
PricingRule fetched = client.pricing().automation().get(ruleId);

if (fetched.configuration() instanceof PricingRuleConfiguration.ChangeByPercentage percentage) {
    System.out.println(percentage.operation() + " " + percentage.value() + "%");
}

client.pricing().automation().delete(ruleId);   // built-in default rules cannot be deleted
```

`PricingRule.configuration()` is `null` for built-in default rules and pure
follow rules. `PricingRule.isDefault()` distinguishes the built-in defaults from
rules you created.

### Assigning a rule to offers

Defining and editing rules lives here in `pricing()`. Applying a rule to a set of
offers in bulk is a batch-offer command on the offers facade, not this
sub-facade; reading the rules currently assigned to a single offer arrives with
the bucket's volume release.

### Scopes

Reading rules requires `sale:offers:read`; creating, editing and deleting them
require `sale:offers:write`.
