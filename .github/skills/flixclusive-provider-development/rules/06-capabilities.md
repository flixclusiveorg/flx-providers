# 06 — Capability Implementation Rules

A provider exposes functionality by implementing capability interfaces and returning them from the
`ProviderPlugin` `suspend` getters.

When implementing a capability, use the matching Provider Docs guide as your baseline (see the
Provider Docs index in `SKILL.md`). For real working implementations, read the corresponding
class in `providers/Stremio/` or `providers/Trakt/`.

## Capability exposure in `ProviderPlugin` (MUST)

- Override only the capabilities you support.
- Return `null` for unsupported capabilities.
- Return **stable instances** — cache via `by lazy`.
- All getters are `suspend` and return nullable types.

```kotlin
@FlixclusiveProvider
class MyProviderPlugin : ProviderPlugin() {
    private val client by lazy { OkHttpClient() }

    private val searchApi by lazy {
        MySearchApi(client = client, providerId = id)
    }

    override suspend fun getSearchApi(context: Context): SearchProviderApi = searchApi
    override suspend fun getTrackerApi(context: Context): TrackerProviderApi? = null  // unsupported
}
```

---

## Catalog (`CatalogProviderApi`)

```kotlin
interface CatalogProviderApi {
    suspend fun getCatalogs(): List<Catalog>
    suspend fun getCatalogItems(catalog: Catalog, page: Int = 1): PaginatedMedia<PartialMedia>
}
```

### Rules

- `getCatalogs()` returns the static list of home-page browsable sections.
- `getCatalogItems(catalog, page)` is paginated; pages are **1-based**.
- Set `hasNextPage` correctly on `PaginatedMedia`.
- `Catalog.providerId` MUST equal `manifest.id` (use `id` from `ProviderPlugin`).

### Concrete example (adapted from `providers/Stremio/StremioCatalogProvider.kt`)

```kotlin
class MyCatalogApi(
    private val client: OkHttpClient,
    private val providerId: String,
) : CatalogProviderApi {

    private val trending = Catalog(
        name        = "Trending",
        url         = "https://example.com/api/trending",
        providerId  = providerId,
        canPaginate = true,
        description = "Most popular right now",
    )

    override suspend fun getCatalogs(): List<Catalog> = listOf(trending)

    override suspend fun getCatalogItems(catalog: Catalog, page: Int): PaginatedMedia<PartialMedia> {
        val dto = client
            .request(url = "${catalog.url}?page=$page")
            .execute()
            .fromJson<MySearchResponseDto>()

        val items = dto.results.mapNotNull { it.toPartialMediaOrNull(providerId) }

        return PaginatedMedia(
            page        = page,
            results     = items,
            hasNextPage = page < dto.totalPages,
            totalPages  = dto.totalPages,
        )
    }
}
```

---

## Search (`SearchProviderApi`)

```kotlin
interface SearchProviderApi {
    val filters: FilterList
    suspend fun search(query: String, page: Int = 1, filters: FilterList = this.filters): PaginatedMedia<PartialMedia>
}
```

### Rules

- Provide `filters: FilterList` (can be empty `FilterList()`).
- Normalize input: trim `query`; return empty `PaginatedMedia` for blank queries.
- Pages are **1-based**.
- Set `totalPages = 0` when unknown; set `hasNextPage` based on whether the result set is non-empty.

### Concrete example (adapted from `providers/Stremio/StremioSearchProvider.kt`)

```kotlin
class MySearchApi(
    private val client: OkHttpClient,
    private val providerId: String,
) : SearchProviderApi {
    override val filters: FilterList = FilterList() // no filters

    override suspend fun search(query: String, page: Int, filters: FilterList): PaginatedMedia<PartialMedia> {
        val q = query.trim()
        if (q.isBlank()) return PaginatedMedia(page = page, results = emptyList(), hasNextPage = false, totalPages = 0)

        val dto = client
            .request(url = "https://example.com/api/search?q=${q.encodeURL()}&page=$page")
            .execute()
            .fromJson<MySearchResponseDto>()

        val items = dto.results.mapNotNull { it.toPartialMediaOrNull(providerId) }

        return PaginatedMedia(
            page        = dto.page,
            results     = items,
            hasNextPage = dto.page < dto.totalPages,
            totalPages  = dto.totalPages,
        )
    }
}
```

---

## Metadata (`MediaMetadataProviderApi`)

```kotlin
interface MediaMetadataProviderApi {
    suspend fun getMovie(media: PartialMedia): Movie
    suspend fun getShow(media: PartialMedia): Show
}
```

### Rules

- The interface is **split** — `getMovie` and `getShow` are separate methods, NOT a single `getMetadata`.
- `media` is a `PartialMedia` (the lightweight search/catalog item).
- Return fully populated `Movie` or `Show` with stable IDs and `providerId` set.
- Prefer stable external IDs (e.g., `media.externalIds[MediaIdSource.IMDB]`) when available.

### Concrete example (adapted from `providers/Stremio/StremioMetadataProvider.kt`)

```kotlin
class MyMetadataApi(
    private val client: OkHttpClient,
) : MediaMetadataProviderApi {

    override suspend fun getMovie(media: PartialMedia): Movie {
        val dto = client
            .request(url = "https://example.com/api/movie/${media.id}")
            .execute()
            .fromJson<MyMovieDto>()
        return dto.toMovie(providerId = media.providerId)
    }

    override suspend fun getShow(media: PartialMedia): Show {
        val dto = client
            .request(url = "https://example.com/api/show/${media.id}")
            .execute()
            .fromJson<MyShowDto>()
        return dto.toShow(providerId = media.providerId)
    }
}
```

---

## Media Links (`MediaLinkProviderApi`)

```kotlin
interface MediaLinkProviderApi {
    val supportedLinkTypes: Set<MediaLinkType>
    suspend fun getLinks(media: MediaMetadata, episode: Episode? = null, onLinkFound: (MediaLink) -> Unit)
}
```

### Rules

- Declare what you emit via `supportedLinkTypes`: `STREAMS`, `SUBTITLES`, or both.
- `getLinks` uses a **callback** (`onLinkFound`) — it does NOT return a `Flow<MediaLink>`.
- Call `onLinkFound(link)` for each discovered link; do not buffer everything in memory.
- Never emit empty or invalid URLs.

Full details and examples are in `rules/07-media-links.md`.

---

## Cross-match (`CrossMatchProviderApi`)

```kotlin
interface CrossMatchProviderApi {
    suspend fun getById(sourceIds: Map<MediaIdSource, String>): MediaMetadata?
    suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata?
}
```

### Rules

- There is **no** `supportedIdSources` property in the SDK — do not add it.
- `getById` should be deterministic — only return a result when you have a confident ID match.
- `getByFuzzy` is a best-effort fallback; it is OK to return `null`.
- The `media` input to `getByFuzzy` comes from **another** provider.

### Concrete example (adapted from `providers/Stremio/StremioCrossMatcher.kt`)

```kotlin
class MyCrossMatchApi(
    private val client: OkHttpClient,
    private val providerId: String,
) : CrossMatchProviderApi {

    override suspend fun getById(sourceIds: Map<MediaIdSource, String>): MediaMetadata? {
        val imdbId = sourceIds[MediaIdSource.IMDB] ?: return null
        return fetchByImdb(imdbId)
    }

    override suspend fun getByFuzzy(media: MediaMetadata): MediaMetadata? {
        // Best-effort title search; return null if no confident match
        return null
    }

    private suspend fun fetchByImdb(id: String): MediaMetadata? {
        // ...
        return null
    }
}
```

---

## Tracker (`TrackerProviderApi`)

```kotlin
interface TrackerProviderApi {
    suspend fun getFeatures(): Set<TrackerFeature>   // method, not a property
    suspend fun isAuthenticated(): Boolean
    // ... list CRUD, scrobble, etc.
}
```

### Rules

- `TrackerFeature` has exactly **two** values: `LIST_MANAGEMENT` and `SCROBBLE`.
- `getFeatures()` is a `suspend` **method**, not `override val features`.
- Back `isAuthenticated()` by stored auth state — make it fast and side-effect-free.
- Tracker operations assume the user is authenticated; fail fast if not.

Full details and examples are in the Provider Docs [Tracker guide](https://flixclusiveorg.github.io/provider-docs/impl/tracker) and `providers/Trakt/`.
