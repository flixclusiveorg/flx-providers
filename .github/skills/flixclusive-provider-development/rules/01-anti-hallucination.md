# 01 — Anti-hallucination (Read First, Always)

These rules override all other rules.

## Non-negotiables

- Read existing code before writing any new code.
  - Use the real provider implementations as canonical templates:
    - Search/Catalog/Metadata/Links/CrossMatch → `providers/Stremio/`
    - Tracker/Auth/DataStore → `providers/Trakt/`
  - Use Provider Docs pages as the implementation guides (capabilities, settings UI, WebView, etc.).
- Do not invent SDK APIs, types, or method signatures.
  - Confirm every signature from `core-stubs` source or the official API docs before writing.
  - The following names are **wrong** and must never appear in generated code:

    | Wrong | Correct |
    |---|---|
    | `FilmSearchItem` | `PartialMedia` |
    | `FilmMetadata` / `Film` | `MediaMetadata` / `PartialMedia` |
    | `TvShow` | `Show` |
    | `PaginatedResponse<T>` | `PaginatedMedia<T>` |
    | `ProviderCatalog` | `Catalog` |
    | `MetadataProviderApi` | `MediaMetadataProviderApi` |
    | `FilmIdSource` | `MediaIdSource` |

- `getLinks` does **not** return `Flow<MediaLink>`. Its actual signature is:
  ```kotlin
  suspend fun getLinks(media: MediaMetadata, episode: Episode? = null, onLinkFound: (MediaLink) -> Unit)
  ```
- `MediaMetadataProviderApi` is **split** — it has `getMovie` and `getShow`, not a single `getMetadata`:
  ```kotlin
  interface MediaMetadataProviderApi {
      suspend fun getMovie(media: PartialMedia): Movie
      suspend fun getShow(media: PartialMedia): Show
  }
  ```
- All `ProviderPlugin` capability getters are `suspend` functions returning nullable types:
  ```kotlin
  override suspend fun getSearchApi(context: Context): SearchProviderApi? = searchApi
  ```
- `TrackerProviderApi.getFeatures()` is a `suspend` function, not a property.
  - `TrackerFeature` has only two values: `LIST_MANAGEMENT` and `SCROBBLE`.
- `CrossMatchProviderApi` does NOT have a `supportedIdSources` property — remove it if present.
- Do not invent Gradle tasks or plugin behavior.
  - Confirm from existing Gradle files in this repo, or by running `./gradlew tasks`.
- If key requirements are missing, ask 1–3 targeted questions before implementing.

## Source of truth (priority order)

1. `core-stubs` source code (exact signatures): local repo at `../core-stubs/` or [API docs](https://flixclusiveorg.github.io/core-stubs/)
2. Real provider implementations in this repo: `providers/Stremio/`, `providers/Trakt/`
3. Provider Docs (recommended patterns + guides): [provider-docs](https://flixclusiveorg.github.io/provider-docs/)
4. Source fallback: [flixclusiveorg/core-stubs on GitHub](https://github.com/flixclusiveorg/core-stubs)

## Scope discipline

- Make the smallest change that satisfies the request.
- Don't refactor unrelated code, don't rename unrelated symbols.
- Don't change CI/workflows, publishing, or repository settings unless explicitly requested.
- Don't add docstrings, comments, or type annotations to code you didn't change.

## Validation expectation

When changes affect build logic or Kotlin compilation, prefer the narrowest check:

- `./gradlew :<ProviderModule>:compileDebugKotlin`
- `./gradlew :<ProviderModule>:make`

If execution is not possible, ensure changes are consistent with existing patterns and compile signatures.
