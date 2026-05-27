# 05 — Coding Style (Kotlin)

Follow the style established in `providers/Stremio/` and `providers/Trakt/`.

## Tier markers

- **MUST** — non-negotiable; always apply
- **SHOULD** — strong preference; deviate only with a clear reason
- **MAY** — optional; apply when it improves readability

## Kotlin formatting (MUST)

- 4-space indentation.
- Trailing commas in multiline argument/parameter lists.
- Named arguments when passing 3+ values of the same type.
- Prefer `val` over `var`.

## Structure (MUST)

- Keep the `ProviderPlugin` entry class small — delegate all logic to capability API classes.
- One responsibility per file/class.
- Extract parsing and mapping into dedicated functions.

## Organize by feature, not by type (MUST)

Group code by the user-facing feature so related code is co-located and easy to find:

```
com.example.my_provider/
├── api/
│   ├── search/
│   │   └── MySearchApi.kt
│   ├── catalog/
│   │   └── MyCatalogApi.kt
│   ├── metadata/
│   │   └── MyMetadataApi.kt
│   └── links/
│       └── MyMediaLinkApi.kt
├── settings/
│   └── MySettingsScreen.kt
├── common/
│   ├── MyOkHttpClient.kt
│   └── Extensions.kt
└── MyProviderPlugin.kt
```

Real example: `providers/Stremio/src/main/kotlin/com/flixclusive/provider/stremio/`

## Keep methods short and focused (MUST)

Prefer small, single-purpose functions: fetch → parse → map → emit/return.

Bad:
```kotlin
// fetch, parse, and map all in one block — avoid
override suspend fun getLinks(media: MediaMetadata, episode: Episode?, onLinkFound: (MediaLink) -> Unit) {
    val html = client.request(url = buildUrl(media)).execute().asJsoup()
    html.select("div.stream a").forEach { el ->
        val url = el.attr("href")
        if (url.isNotBlank()) {
            onLinkFound(Stream(name = el.text(), url = url))
        }
    }
}
```

Good:
```kotlin
override suspend fun getLinks(media: MediaMetadata, episode: Episode?, onLinkFound: (MediaLink) -> Unit) {
    val html = fetchPage(media)
    parseStreamLinks(html).forEach(onLinkFound)
}

private suspend fun fetchPage(media: MediaMetadata): Document { ... }
private fun parseStreamLinks(html: Document): List<Stream> { ... }
```

## Member order (SHOULD)

Within a class, order members as follows:

1. `companion object`
2. `private val` (properties)
3. `private var`
4. `val` (public/internal properties)
5. `var`
6. Overridden methods
7. `private fun`
8. `fun` (public/internal)

Within each group, sort alphabetically when practical.

## Coroutines (MUST)

- Never block threads in `suspend` functions.
  Use `withContext(Dispatchers.IO)` (or the Core Stubs `FlxDispatchers.IO`) for blocking I/O.
- Respect cancellation — never swallow `CancellationException`.

## Error handling (MUST)

- Fail fast with a meaningful exception when extraction is impossible.
- Do not return silently-invalid data (empty URLs, missing IDs, default fallbacks that hide real failures).
- Do not log secrets (tokens, cookies, user identifiers).

## Data models (MUST)

- Use `core-stubs` model types — never duplicate them:
  - `PartialMedia` for search/catalog results
  - `Movie` / `Show` for full metadata
  - `MediaLink`, `Stream`, `Subtitle` for links
  - `Catalog` for catalog definitions
- Use Kotlinx Serialization (`@Serializable`, `@SerialName`) for DTO classes.

## Readability pitfalls to avoid (SHOULD)

- Avoid premature abstraction — don't create an interface with only one implementation.
- Avoid overly complex expressions; split into intermediate `val`s.
- Avoid excessive comments; prefer self-explanatory names. Comments explain *why*, not *what*.
- Avoid overusing the Elvis operator (`?:`) when an explicit `if` is clearer.
