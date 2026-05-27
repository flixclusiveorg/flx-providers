---
name: flixclusive-provider-development
description: >
  Comprehensive skill for building Flixclusive providers in this repository. Use this when asked to
  create a new provider module, implement provider capabilities (catalog/search/metadata/media links/
  cross-match/tracker), wire a ProviderPlugin, add a SettingsScreen, update Gradle, debug build/deploy,
  or refactor provider code.
---

# Flixclusive Provider Development Skill

This skill governs Flixclusive **provider** development. It is built around the provider SDK in
`core-stubs` and the working provider implementations in this repository.

---

## How to Use

**Read every rule file before generating or editing code.** Rules are in `rules/` alongside this file.

| Rule File | When to Read |
|---|---|
| `rules/01-anti-hallucination.md` | **Always — read before every task.** Overrides all other rules. |
| `rules/02-repo-structure.md` | When creating a provider module or navigating this repo. |
| `rules/03-gradle.md` | When touching `build.gradle.kts`, adding dependencies, or configuring `flxProvider`. |
| `rules/04-provider-architecture.md` | When wiring a `ProviderPlugin` and capability APIs. |
| `rules/05-coding-style.md` | When writing any Kotlin code (non-UI). |
| `rules/06-capabilities.md` | When implementing catalog/search/metadata/media links/cross-match/tracker. |
| `rules/07-media-links.md` | When extracting streams/subtitles and emitting `MediaLink` values. |
| `rules/08-settings-auth.md` | When adding user settings, login/auth flows, or storing tokens. |
| `rules/09-networking-scraping.md` | When adding HTTP calls, HTML parsing, or site-specific extraction logic. |
| `rules/10-testing-debugging.md` | When debugging providers, writing tests, or using the app's test provider flow. |
| `rules/11-release-packaging.md` | When versioning, building, deploying, or preparing releases. |
| `rules/12-naming-conventions.md` | When naming modules, packages, provider IDs, and files. |

---

## CRITICAL: Correct SDK Type Names

The following aliases **no longer exist or are deprecated** — never use them in generated code:

| Wrong | Correct (actual SDK) |
|---|---|
| `FilmSearchItem` | `PartialMedia` |
| `FilmMetadata` / `Film` | `MediaMetadata` (full) / `PartialMedia` (lightweight) |
| `TvShow` | `Show` |
| `PaginatedResponse<T>` | `PaginatedMedia<T>` |
| `ProviderCatalog` | `Catalog` |
| `MetadataProviderApi` | `MediaMetadataProviderApi` |
| `FilmIdSource` | `MediaIdSource` |
| `supportedIdSources` on CrossMatch | does NOT exist in SDK — remove |
| `Flow<MediaLink>` return from `getLinks` | `suspend fun getLinks(…, onLinkFound: (MediaLink) -> Unit)` |
| `override val features` (TrackerProviderApi) | `override suspend fun getFeatures(): Set<TrackerFeature>` |
| `TrackerFeature.LISTS_READ/WRITE` etc. | `TrackerFeature.LIST_MANAGEMENT` + `TrackerFeature.SCROBBLE` |

### ProviderPlugin capability getters are `suspend`

```kotlin
// ALL getters are suspend and return nullable types
override suspend fun getCatalogApi(context: Context): CatalogProviderApi? = catalogApi
override suspend fun getSearchApi(context: Context): SearchProviderApi? = searchApi
override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi? = metadataApi
override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi? = mediaLinkApi
override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi? = null
override suspend fun getTrackerApi(context: Context): TrackerProviderApi? = null
```

### MediaMetadataProviderApi is split — not a single `getMetadata`

```kotlin
// SDK interface
interface MediaMetadataProviderApi {
    suspend fun getMovie(media: PartialMedia): Movie
    suspend fun getShow(media: PartialMedia): Show
}
```

### getLinks uses a callback — not Flow

```kotlin
// SDK interface
interface MediaLinkProviderApi {
    val supportedLinkTypes: Set<MediaLinkType>
    suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode? = null,
        onLinkFound: (MediaLink) -> Unit,
    )
}
```

---

## Anti-patterns — Never Do These

- Do not invent SDK types or method signatures; verify from `core-stubs` source or API docs.
- Do not use `FilmSearchItem`, `FilmMetadata`, `TvShow`, `PaginatedResponse`, `ProviderCatalog`, or `MetadataProviderApi` in any generated code.
- Do not use `Flow<MediaLink>` as the return type of `getLinks`.
- Do not access `manifest` or `settings` in `init {}` blocks or property initializers — use `by lazy`.
- Do not create a new capability API instance on every getter call — cache via `by lazy`.
- Do not change a provider's `id` after the first release — it breaks update identity.
- Do not invent Gradle tasks; confirm from `./gradlew tasks` or existing build files.
- Do not refactor code that is unrelated to the current task.
- Do not add error handling for scenarios that cannot happen in practice.

---

## Real-World Reference Implementations

Before generating code, read the relevant sections of the working providers in this repo:

| Provider | Location | What to study |
|---|---|---|
| Stremio | `providers/Stremio/` | Catalog, Search, Metadata, MediaLinks, CrossMatch, Settings UI, OkHttp caching |
| Trakt | `providers/Trakt/` | Tracker, OAuth/auth flow, DataStore token storage, custom Catalogs |

These are the canonical ground truth for patterns — not the rule files alone.

---

## Key Principle

> When in doubt — **read existing code first**. Check `core-stubs` source for every API signature you
> are unsure about. Never assume. Never invent. Search → read → confirm → generate.

---

## Core SDK Reference

- Primary API docs: [core-stubs docs](https://flixclusiveorg.github.io/core-stubs/)
- Provider docs: [provider-docs](https://flixclusiveorg.github.io/provider-docs/)
- Source fallback: [flixclusiveorg/core-stubs on GitHub](https://github.com/flixclusiveorg/core-stubs)

## Provider Docs Index

Use this as a guide map when implementing or documenting a feature:

| Topic | Guide |
|---|---|
| Installation | [Getting started: Installation](https://flixclusiveorg.github.io/provider-docs/getting-started/installation) |
| Development workflow | [Getting started: Development](https://flixclusiveorg.github.io/provider-docs/getting-started/development) |
| Provider architecture | [Provider: Overview](https://flixclusiveorg.github.io/provider-docs/provider/overview) |
| Gradle + metadata | [Provider: Configuration](https://flixclusiveorg.github.io/provider-docs/provider/configuration) |
| Entry class + capabilities | [Provider: Creating a provider](https://flixclusiveorg.github.io/provider-docs/provider/create_provider) |
| Testing | [Provider: Testing a provider](https://flixclusiveorg.github.io/provider-docs/provider/test_provider) |
| Search | [Implementation: Searching media](https://flixclusiveorg.github.io/provider-docs/impl/searching_media) |
| Search filters | [Implementation: Search filters](https://flixclusiveorg.github.io/provider-docs/impl/impl_filters) |
| Catalogs | [Implementation: Loading catalogs](https://flixclusiveorg.github.io/provider-docs/impl/loading_catalogs) |
| Metadata | [Implementation: Fetching metadata](https://flixclusiveorg.github.io/provider-docs/impl/fetching_metadata) |
| Media links | [Implementation: Fetching media links](https://flixclusiveorg.github.io/provider-docs/impl/fetching_links) |
| Settings UI | [Implementation: Settings UI](https://flixclusiveorg.github.io/provider-docs/impl/impl_settings) |
| Cross-match | [Implementation: Cross-match](https://flixclusiveorg.github.io/provider-docs/impl/cross_match) |
| Tracker | [Implementation: Tracker](https://flixclusiveorg.github.io/provider-docs/impl/tracker) |
| WebView interception | [Advanced: WebView](https://flixclusiveorg.github.io/provider-docs/advanced/webview) |
| Dependency packaging | [Advanced: Unsupported libraries](https://flixclusiveorg.github.io/provider-docs/advanced/unsupported_libs) |
| Kotlin style | [Best practices: Coding style](https://flixclusiveorg.github.io/provider-docs/best_practices/coding_style) |
