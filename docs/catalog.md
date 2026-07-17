# Catalogue — `client.catalog()`

The catalogue is Allegro's read-only reference data: the category tree, per-category
parameters, the product database, and vehicle/part compatibility lists. It is what an offer is
classified and described against. All of it is public data, so an app-only **client-credentials**
token is enough — no user login required.

> **Status:** starter slice. Only `catalog().categories()` (the category tree) ships in this
> first PR; `products()` and `compatibility()` follow in the same bucket. See
> [`API-SURFACE.md`](../API-SURFACE.md) §E for the full planned surface.

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

## Verifying against the sandbox

The read-only demo scenario navigates the live category tree and confirms the mapped fields
arrive (the read-only counterpart of the write→read rule in [`TESTING.md`](../TESTING.md)):

```bash
./gradlew :allegro-demo:run -Pdemo.scenario=catalog-categories
```
