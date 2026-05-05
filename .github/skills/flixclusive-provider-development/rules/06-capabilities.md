# 06 — Capability Implementation Rules

A provider exposes functionality by implementing capability interfaces and returning them from `ProviderPlugin` getters.

When implementing a capability, use the matching Provider Docs guide as your baseline (see the Provider Docs index in `SKILL.md`).

## Capability exposure (in `ProviderPlugin`)

- Override only what you support.
- Return `null` for unsupported capabilities.
- Return stable instances (cache via `by lazy`).

### Concrete example (stable instances + nullable getters)

This pattern is used throughout the Provider Docs implementation guides.

```kotlin
private const val KEY_TMDB_API_KEY = "tmdb_api_key"

@FlixclusiveProvider
class TestProviderPlugin : ProviderPlugin() {
  private val client by lazy { OkHttpClient() }

  private val searchApi by lazy {
    TestSearchApi(
      client = client,
      providerId = id,
      tmdbApiKey = settings.getString(KEY_TMDB_API_KEY, null) ?: "",
    )
  }

  // ProviderPlugin getters are nullable (`SearchProviderApi?`). Returning non-null is OK.
  override fun getSearchApi(context: Context): SearchProviderApi = searchApi

  // Unsupported capability: return null.
  override fun getTrackerApi(context: Context): TrackerProviderApi? = null
}
```

## Catalog (`CatalogProviderApi`)

- Implement `getCatalogs()` as the canonical async loader.
- Return a `List<Catalog>` from `getCatalogs()`.
- Implement pagination in `getCatalogItems(catalog, page)` using `PaginatedResponse`.
  - Pages are 1-based.
  - Set `hasNextPage` correctly.

### Catalog shape expectations

- Catalogs are curated home-page sections.
- `Catalog` should have:
  - `name` (display name)
  - `url` (resolvable endpoint/URL used by `getCatalogItems`)
  - `canPaginate` (whether the host can show “Load more”)
  - `providerId` (use the provider’s manifest id)
  - Optional: `image`, `description`, `headers`

### Concrete example (catalogs + paging)

This is adapted from the Provider Docs “Loading catalogs” guide. It intentionally shows:

- `Catalog(...)` construction
- `OkHttpClient.request(...)` + `Response.fromJson<T>()`
- `PaginatedResponse(...)` assembly

```kotlin
class TestCatalogApi(
  private val client: OkHttpClient,
  private val providerId: String,
  private val tmdbApiKey: String,
) : CatalogProviderApi {
  private val trendingMovies = Catalog(
    name = "Trending Movies",
    url = "https://api.themoviedb.org/3/discover/movie",
    canPaginate = true,
    providerId = providerId,
    description = "Popular movies right now",
  )

  private val popularTvShows = Catalog(
    name = "Popular TV Shows",
    url = "https://api.themoviedb.org/3/discover/tv",
    canPaginate = true,
    image = "https://api.themoviedb.org/sample_icon.png",
    providerId = providerId,
  )

  override suspend fun getCatalogs(): List<Catalog> = listOf(
    trendingMovies,
    popularTvShows,
  )

  private fun buildCatalogUrl(
    baseUrl: String,
    page: Int,
    apiKey: String,
  ): String = "$baseUrl?api_key=$apiKey&page=$page"

  override suspend fun getCatalogItems(
    catalog: Catalog,
    page: Int,
  ): PaginatedResponse<FilmSearchItem> {
    // DTOs/mappers are intentionally reused from the Search example below.
    val dto = client
      .request(url = buildCatalogUrl(catalog.url, page, tmdbApiKey))
      .execute()
      .fromJson<TmdbSearchResponseDto>()

    // Catalog endpoints often don't return `media_type`, so force it based on which catalog is being loaded.
    val forcedType = when (catalog.url) {
      trendingMovies.url -> FilmType.MOVIE
      popularTvShows.url -> FilmType.TV_SHOW
      else -> null
    }

    val results = dto.results
      .mapNotNull { it.toFilmSearchItemOrNull(providerId = providerId, forcedType = forcedType) }

    val hasNext = if (dto.totalPages != 0) dto.page < dto.totalPages else results.isNotEmpty()

    return PaginatedResponse(
      page = dto.page,
      results = results,
      hasNextPage = hasNext,
      totalPages = dto.totalPages,
    )
  }
}
```

## Search (`SearchProviderApi`)

- Provide `filters: FilterList` (can be empty).
- Implement `search(title, page, filters)`.
- Normalize input (trim) and treat blank search sanely.
- Return `PaginatedResponse<FilmSearchItem>`.

### PaginatedResponse contract

- `page` is 1-based.
- `totalPages` can be `0` if unknown.
- `hasNextPage` should be derived from:
  - `page < totalPages` when `totalPages` is known, or
  - a deterministic heuristic when unknown (for example, `results.isNotEmpty()` for fixed page sizes).

### Filters (when applicable)

- Filters belong to the API via `override val filters: FilterList`.
- The host UI renders those filters and passes the current state back into `search(..., filters)`.
- Use the SDK filter types:
  - `Filter.Select`, `Filter.CheckBox`, `Filter.TriState`, `Filter.Sort`
  - Group them with `FilterGroup` and expose via `FilterList`.

### Concrete example (filters + OkHttp + JSON parsing)

This is adapted from the Provider Docs “Searching media” + “Search filters” guides.

```kotlin
private const val FILTER_ALL = 0
private const val FILTER_MOVIE = 1
private const val FILTER_TV_SHOW = 2

class TmdbMediaTypeFilters(
  name: String = "Media type",
) : FilterGroup(
  name = name,
  Filter.Select(
    name = "",
    options = listOf("All", "Movies", "TV Shows"),
    state = FILTER_ALL,
  ),
) {
  private val select: Filter.Select<String>
    get() = first() as Filter.Select<String>

  val selectedIndex: Int
    get() = select.state
}

@Serializable
data class TmdbSearchResponseDto(
  val page: Int,
  @SerialName("total_pages") val totalPages: Int = 0,
  val results: List<TmdbSearchItemDto> = emptyList(),
)

@Serializable
data class TmdbSearchItemDto(
  val id: Int,
  @SerialName("media_type") val mediaType: String? = null,
  val title: String? = null,
  val name: String? = null,
  @SerialName("poster_path") val posterPath: String? = null,
  val overview: String? = null,
  @SerialName("vote_average") val voteAverage: Double? = null,
  val adult: Boolean? = null,
)

private fun TmdbSearchItemDto.toFilmSearchItemOrNull(
  providerId: String,
  forcedType: FilmType? = null,
): FilmSearchItem? {
  val resolvedType = forcedType ?: when (mediaType) {
    "movie" -> FilmType.MOVIE
    "tv" -> FilmType.TV_SHOW
    else -> null
  }

  if (resolvedType == null) return null

  val resolvedTitle = title ?: name.orEmpty()
  val poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

  return FilmSearchItem(
    id = id.toString(),
    providerId = providerId,
    filmType = resolvedType,
    homePage = "https://www.themoviedb.org/${resolvedType.type}/$id",
    title = resolvedTitle,
    posterImage = poster,
    adult = adult == true,
    externalIds = mapOf(FilmIdSource.TMDB to id.toString()),
    releaseDate = null,
    rating = voteAverage,
    overview = overview,
  )
}

class TestSearchApi(
  private val client: OkHttpClient,
  private val providerId: String,
  private val tmdbApiKey: String,
) : SearchProviderApi {
  override val filters: FilterList = FilterList(
    TmdbMediaTypeFilters(),
  )

  private fun buildTmdbRequestUrl(
    endpoint: String,
    query: String,
    page: Int,
    apiKey: String,
  ): String {
    val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
    return "$endpoint?api_key=$apiKey&query=$encodedQuery&page=$page"
  }

  override suspend fun search(
    title: String,
    page: Int,
    filters: FilterList,
  ): PaginatedResponse<FilmSearchItem> {
    val query = title.trim()
    if (query.isBlank()) {
      return PaginatedResponse(
        page = page,
        results = emptyList(),
        hasNextPage = false,
        totalPages = 0,
      )
    }

    val mediaTypeFilter = filters
      .filterIsInstance<TmdbMediaTypeFilters>()
      .firstOrNull()
    val mediaTypeIndex = mediaTypeFilter?.selectedIndex ?: FILTER_ALL

    val endpoint = when (mediaTypeIndex) {
      FILTER_ALL -> "https://api.themoviedb.org/3/search/multi"
      FILTER_TV_SHOW -> "https://api.themoviedb.org/3/search/tv"
      FILTER_MOVIE -> "https://api.themoviedb.org/3/search/movie"
      else -> "https://api.themoviedb.org/3/search/multi"
    }

    val dto = client
      .request(url = buildTmdbRequestUrl(endpoint, query, page, tmdbApiKey))
      .execute()
      .fromJson<TmdbSearchResponseDto>()

    val forcedType = when (mediaTypeIndex) {
      FILTER_MOVIE -> FilmType.MOVIE
      FILTER_TV_SHOW -> FilmType.TV_SHOW
      else -> null
    }

    val results = dto.results
      .asSequence()
      .mapNotNull { it.toFilmSearchItemOrNull(providerId = providerId, forcedType = forcedType) }
      .toList()

    return PaginatedResponse(
      page = dto.page,
      results = results,
      hasNextPage = dto.totalPages != 0 && dto.page < dto.totalPages || (dto.totalPages == 0 && results.isNotEmpty()),
      totalPages = dto.totalPages,
    )
  }
}
```

Minimal grouped filter example (useful when you don’t need a custom `FilterGroup` subclass):

```kotlin
override val filters: FilterList = FilterList(
  FilterGroup(
    name = "Content Filters",
    Filter.Select("Genre", listOf("Action", "Comedy", "Drama")),
    Filter.CheckBox("HD Only"),
    Filter.Sort("Sort By", listOf("Popularity", "Release Date")),
  ),
)
```

## Metadata (`MetadataProviderApi`)

- Implement `getMetadata(film: Film): FilmMetadata`.
- Ensure returned metadata includes stable IDs and the correct provider identifier.

### Metadata expectations

- Return a richer `FilmMetadata` (`Movie` or `TvShow`) for the given `Film` (often a `FilmSearchItem`).
- Prefer stable external IDs when available (for example `film.externalIds[FilmIdSource.TMDB]`).
- For `genres`, prefer setting `Genre.catalog` for navigation metadata; `Genre.id` and `Genre.url` are deprecated.
- `FilmDetails` exists in older code but is deprecated in favor of `FilmMetadata`.

### Concrete example (choose endpoint by film type)

Adapted from the Provider Docs “Fetching metadata” guide.

```kotlin
@Serializable
data class TmdbMovieDto(
  val id: Int,
  val title: String,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("backdrop_path") val backdropPath: String? = null,
  val homepage: String? = null,
  val overview: String? = null,
  val adult: Boolean = false,
  @SerialName("vote_average") val voteAverage: Double? = null,
) {
  fun toMovie(providerId: String): Movie {
    val poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdrop = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    return Movie(
      id = id.toString(),
      title = title,
      posterImage = poster,
      homePage = homepage,
      backdropImage = backdrop,
      externalIds = mapOf(FilmIdSource.TMDB to id.toString()),
      releaseDate = null,
      rating = voteAverage,
      providerId = providerId,
      adult = adult,
      overview = overview,
    )
  }
}

@Serializable
data class TmdbTvShowDto(
  val id: Int,
  @SerialName("name") val title: String,
  @SerialName("poster_path") val posterPath: String? = null,
  @SerialName("backdrop_path") val backdropPath: String? = null,
  val homepage: String? = null,
  val overview: String? = null,
  val adult: Boolean = false,
  @SerialName("vote_average") val voteAverage: Double? = null,
  @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
  @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
) {
  fun toTvShow(providerId: String): TvShow {
    val poster = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val backdrop = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    return TvShow(
      id = id.toString(),
      title = title,
      posterImage = poster,
      homePage = homepage,
      backdropImage = backdrop,
      externalIds = mapOf(FilmIdSource.TMDB to id.toString()),
      releaseDate = null,
      rating = voteAverage,
      providerId = providerId,
      adult = adult,
      overview = overview,
      totalEpisodes = numberOfEpisodes,
      totalSeasons = numberOfSeasons,
    )
  }
}

class TestMetadataApi(
  private val client: OkHttpClient,
  private val tmdbApiKey: String,
) : MetadataProviderApi {
  private fun buildTmdbDetailsUrl(
    base: String,
    id: String,
    apiKey: String,
  ): String = "$base/$id?api_key=$apiKey"

  override suspend fun getMetadata(film: Film): FilmMetadata {
    val providerId = film.providerId
    val tmdbId = film.externalIds[FilmIdSource.TMDB] ?: film.id

    val endpoint = when (film.filmType) {
      FilmType.MOVIE -> "https://api.themoviedb.org/3/movie"
      FilmType.TV_SHOW -> "https://api.themoviedb.org/3/tv"
    }

    val response = client
      .request(url = buildTmdbDetailsUrl(endpoint, tmdbId, tmdbApiKey))
      .execute()

    return when (film.filmType) {
      FilmType.MOVIE -> response.fromJson<TmdbMovieDto>().toMovie(providerId)
      FilmType.TV_SHOW -> response.fromJson<TmdbTvShowDto>().toTvShow(providerId)
    }
  }
}
```

## Media links (`MediaLinkProviderApi`)

- Declare what you can emit via `supportedLinkTypes: Set<MediaLinkType>`:
  - `setOf(MediaLinkType.STREAMS)`
  - `setOf(MediaLinkType.SUBTITLES)`
  - `setOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)`
- Implement `getLinks(film, episode)` as a `Flow<MediaLink>`.
  - Emit as you discover links; do not wait for all extraction if not necessary.

### Flags

- Use `Flag` values to declare constraints (auth headers, third-party gateways, trust/source labeling, etc.).
- Never emit empty/invalid URLs.

Concrete implementation examples for media-links are in `rules/07-media-links.md`.

## Cross-match (`CrossMatchProviderApi`)

- Implement when you can resolve other providers’ IDs deterministically.
- `supportedIdSources` is required; override it to declare which `FilmIdSource` you support.
- Prefer `getById(sourceIds)` for exact ID resolution; use `getByFuzzy(film)` as fallback.
  - `film` is assumed to be metadata from another provider (not from this provider).

### Cross-match behavior

- `getById(sourceIds)` should be deterministic and based on trusted IDs.
- `getByFuzzy(film)` should be a fallback only (best-effort matching).

### Concrete example (ID-first, fuzzy fallback)

Adapted from the Provider Docs “Cross-match” guide.

```kotlin
class TestCrossMatchApi(
  private val providerId: String,
) : CrossMatchProviderApi {
  override val supportedIdSources: Set<FilmIdSource> = setOf(
    FilmIdSource.TMDB,
    FilmIdSource.IMDB,
  )

  override suspend fun getById(
    sourceIds: Map<FilmIdSource, String>,
  ): FilmMetadata? {
    val tmdbId = sourceIds[FilmIdSource.TMDB]
    val imdbId = sourceIds[FilmIdSource.IMDB]

    return when {
      tmdbId != null -> fetchByTmdbId(tmdbId)
      imdbId != null -> fetchByImdbId(imdbId)
      else -> null
    }
  }

  override suspend fun getByFuzzy(
    film: FilmMetadata,
  ): FilmMetadata? {
    // Best-effort fallback only.
    return null
  }

  private suspend fun fetchByTmdbId(tmdbId: String): FilmMetadata? = TODO()
  private suspend fun fetchByImdbId(imdbId: String): FilmMetadata? = TODO()
}
```

## Tracker (`TrackerProviderApi`)

- Implement only if you integrate with a tracker service (lists and/or scrobble).
- Declare supported operations via `features: Set<TrackerFeature>`.
- Use the concrete feature names the SDK exposes: `LISTS_READ`, `LISTS_CREATE`, `LISTS_UPDATE`, `LISTS_DELETE`, `LIST_ITEMS_READ`, `LIST_ITEMS_ADD`, `LIST_ITEMS_REMOVE`, `SCROBBLE_START`, `SCROBBLE_STOP`.
- Implement `suspend fun isAuthenticated(): Boolean` and keep it fast + side-effect free (derive from stored auth state).
- If auth is required and the user is not authenticated, return `false` from `isAuthenticated()`.
- Tracker operations assume an authenticated user; if called while unauthenticated, fail fast with a normal exception (do not reference `TrackerAuthRequiredException`; it is not part of the SDK).

### Scrobble contract

- Use `ScrobbleAction.START` when playback starts/resumes.
- Use `ScrobbleAction.STOP` when playback stops/finishes.
- Do not add a pause action unless the SDK contract adds it.
- Ensure `progressPercent` is within 0..100.

### Concrete example skeleton (features + list ops + scrobble)

Adapted from the Provider Docs “Tracker” guide.

```kotlin
class TestTrackerApi(
  private val providerId: String,
) : TrackerProviderApi {
  override val features: Set<TrackerFeature> = setOf(
    TrackerFeature.LISTS_READ,
    TrackerFeature.LISTS_CREATE,
    TrackerFeature.LISTS_UPDATE,
    TrackerFeature.LISTS_DELETE,
    TrackerFeature.LIST_ITEMS_READ,
    TrackerFeature.LIST_ITEMS_ADD,
    TrackerFeature.LIST_ITEMS_REMOVE,
    TrackerFeature.SCROBBLE_START,
    TrackerFeature.SCROBBLE_STOP,
  )

  override suspend fun isAuthenticated(): Boolean = TODO(
    "Return whether the user is authenticated (derive from ProviderSettings auth state)"
  )

  override suspend fun getLists(): List<TrackerList> = emptyList()

  override suspend fun createList(
    name: String,
    description: String?,
  ): TrackerList = TODO("Create list remotely")

  override suspend fun updateList(
    list: TrackerList,
    name: String?,
    description: String?,
  ): TrackerList = TODO("Update list remotely")

  override suspend fun deleteList(list: TrackerList) {
    TODO("Delete list remotely")
  }

  override suspend fun getListItems(
    list: TrackerList,
    page: Int,
  ): PaginatedResponse<FilmSearchItem> {
    TODO("Load tracker list items")
  }

  override suspend fun addListItem(
    list: TrackerList,
    item: Film,
  ) {
    TODO("Add item to list")
  }

  override suspend fun removeListItem(
    list: TrackerList,
    item: Film,
  ) {
    TODO("Remove item from list")
  }

  override suspend fun scrobble(
    action: ScrobbleAction,
    film: FilmMetadata,
    episode: Episode?,
    progressPercent: Float,
    atMs: Long?,
  ) {
    require(progressPercent in 0f..100f)
    TODO("Send scrobble event")
  }
}
```
