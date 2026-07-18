# Catalogue — `client.catalog()`

The catalogue is Allegro's read-only reference data: the category tree, per-category
parameters, the product database, and vehicle/part compatibility lists. It is what an offer is
classified and described against. All of it is public data, so an app-only **client-credentials**
token is enough — no user login required.

> **Status:** the `catalog().categories()` sub-facade ships — tree navigation, per-category
> parameters, and name-based suggestions; `products()` and `compatibility()` follow in the same
> bucket. See [`API-SURFACE.md`](../API-SURFACE.md) §E for the full planned surface.

## Categories

```java
try (AllegroClient client = AllegroClient.create(credentials, AllegroEnvironment.PRODUCTION)) {
    CatalogCategories categories = client.catalog().categories();

    // Walk the tree from the top.
    for (Category root : categories.roots()) {
        System.out.println(root.id() + "  " + root.name());
    }

    // One level down.
    List<Category> children = categories.childrenOf("165929");

    // A single category, with what it permits.
    Category category = categories.get("165929");
    if (category.leaf()) {
        // Only leaf categories can hold offers.
        boolean canCreateProduct = category.options() != null
                && category.options().productCreationEnabled();
    }
}
```

### What you get back

`Category` is an immutable record:

| Component | Meaning |
|---|---|
| `id()` | opaque category id (treat as a string; may be integer- or UUID-shaped) |
| `name()` | display name, localized per request (marketplace default for now) |
| `leaf()` | `true` when the category has no children — only leaves hold offers |
| `parentId()` | the parent category id, or `null` for a root |
| `options()` | `CategoryOptions` (product creation, advertisements, …), or `null` |

### Errors

Every call maps non-2xx responses to the SDK's remediation-grouped exceptions: a bad category
id surfaces as `AllegroBadRequestException` (with the typed `errors()` list), a missing one as
`AllegroNotFoundException`, throttling as `AllegroRateLimitException` (honouring `Retry-After`),
and 5xx/transport failures as `AllegroServerException`. GET calls are retried on 429/5xx per the
configured `RetryPolicy`.

## Category parameters

Every category defines the parameters its offers and products may (or must) set — each with a
value type, its constraints, and, for dictionary parameters, the allowed values.

```java
for (CategoryParameter parameter : categories.parameters("165929")) {
    System.out.println(parameter.name() + " (" + parameter.type() + ")"
            + (parameter.required() ? " [required]" : ""));
    if (parameter.type() == CategoryParameterType.DICTIONARY) {
        for (DictionaryValue value : parameter.dictionary()) {
            System.out.println("  " + value.id() + " = " + value.value());
        }
    }
}
```

`CategoryParameter.type()` selects which part of the record carries the constraint:

| Type | Where the constraint lives |
|---|---|
| `DICTIONARY` | `dictionary()` — the selectable `DictionaryValue`s; `restrictions().multipleChoices()` |
| `FLOAT` | `restrictions()` — `minValue` / `maxValue` / `range` / `precision` |
| `INTEGER` | `restrictions()` — `minValue` / `maxValue` / `range` |
| `STRING` | `restrictions()` — `minLength` / `maxLength` / `allowedNumberOfValues` |
| `OTHER` | a type this SDK version does not model — `restrictions()` is `null`, `dictionary()` empty |

Use `parameter.id()` (and, for dictionaries, a `value.id()`) to set the value when you build an
offer or product.

## Category suggestions

Given a product or offer title, Allegro suggests the categories it best fits — the same hint the
sell form makes. Matches come best-first; walk `parent()` for the breadcrumb up to the root.

```java
for (CategorySuggestion match : categories.suggest("wiertarka udarowa Bosch")) {
    StringBuilder breadcrumb = new StringBuilder(match.name());
    for (CategorySuggestion node = match.parent(); node != null; node = node.parent()) {
        breadcrumb.insert(0, node.name() + " > ");
    }
    System.out.println(match.id() + "  " + breadcrumb);
}
```

## Products

Products are the shared descriptions offers are built from. Search the database lazily — the
returned stream follows Allegro's opaque `page.id` cursor for you, so a bounded consumer only
fetches the pages it needs.

```java
CatalogProducts products = client.catalog().products();

products.search(ProductSearchRequest.builder().phrase("iphone 15").categoryId("257").build())
        .limit(50)
        .forEach(summary -> System.out.println(summary.id() + "  " + summary.name()));
```

A search needs a phrase — `build()` fails fast otherwise; a category is an optional filter that
Allegro only honours alongside a phrase. Each `ProductSummary` carries the product id, name,
category id, publication status (`LISTED` / `PROPOSED`) and image URLs.

Read the full product — its parameter values and flags — with `get`:

```java
Product product = products.get(summary.id());
for (ProductParameterValue parameter : product.parameters()) {
    System.out.println(parameter.name() + " = " + String.join(", ", parameter.values()));
}
```

Before building a product for a category, learn the schema that category expects with
`parametersIn` — the product-side counterpart of `categories().parameters(...)`:

```java
for (ProductParameter parameter : products.parametersIn("257")) {
    System.out.println(parameter.id() + "  " + parameter.name()
            + "  (" + parameter.type() + (parameter.required() ? ", required)" : ")"));
}
```

Each `ProductParameter` carries the value `type` (`DICTIONARY` / `FLOAT` / `INTEGER` /
`STRING`, or `OTHER` for a type this release does not yet model), whether it is `required`,
its `unit`, and — by type — either the numeric/text `restrictions` or the selectable
`dictionary` values. It shares those value types with `CategoryParameter` but omits the two
components that apply only to an offer's parameters (`requiredForProduct`, display options).

## Verifying against the sandbox

The read-only demo scenario navigates the live category tree and confirms the mapped fields
arrive (the read-only counterpart of the write→read rule in [`TESTING.md`](../TESTING.md)):

```bash
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-categories
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-products -Pdemo.account=seller
```
