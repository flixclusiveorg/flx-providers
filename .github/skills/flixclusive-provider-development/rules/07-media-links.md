# 07 — Media Link Extraction Rules

## SDK interface (MUST read before implementing)

```kotlin
interface MediaLinkProviderApi {
    val supportedLinkTypes: Set<MediaLinkType>
    suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode? = null,
        onLinkFound: (MediaLink) -> Unit,
    )
}
```

`getLinks` is a `suspend fun` with a **callback** parameter. It does **not** return `Flow<MediaLink>`.
Call `onLinkFound(link)` for each link as soon as you discover it.

## Declare what you emit (MUST)

```kotlin
// Streams only
override val supportedLinkTypes = setOf(MediaLinkType.STREAMS)

// Subtitles only
override val supportedLinkTypes = setOf(MediaLinkType.SUBTITLES)

// Both
override val supportedLinkTypes = setOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)
```

## Emitting links (MUST)

- Call `onLinkFound(link)` immediately when each link is discovered.
- Do not buffer all links in a list and call them at the end — emit as you go.
- Never emit empty or invalid URLs.
- `Stream.name` should be human-readable (quality, source name, server name).
- For subtitle language codes, use standard BCP 47 codes where possible (`en`, `es`, `pt-BR`).

## IO and parsing (MUST)

- Do network calls and HTML parsing on an IO dispatcher.
  Use `withContext(Dispatchers.IO)` or Core Stubs `FlxDispatchers.IO`.
- Prefer Core Stubs helpers when available:
  - `OkHttpClient.request(...)`
  - `Response.asJsoup()` for HTML parsing
  - `Response.fromJson<T>()` for JSON

## Flags (SHOULD)

Use `Flag` values to declare constraints on links:

- `Flag.ThirdPartyGateway(name, url, logo)` — link is a handoff to another streaming site.
- `Flag.RequiresAuth(customHeaders)` — stream requires auth headers.
- Use `mediaLink.getFlagOfType<Flag.ThirdPartyGateway>()` to read a specific flag type.
- Do not use `Flag.Trusted` — it is deprecated.

## Concrete example (adapted from `providers/Stremio/StremioLinkProvider.kt`)

```kotlin
class MyMediaLinkApi(
    private val client: OkHttpClient,
) : MediaLinkProviderApi {

    override val supportedLinkTypes = setOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)

    override suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit,
    ) {
        val imdbId = media.externalIds[MediaIdSource.IMDB] ?: return

        val type = if (episode != null) "series" else "movie"
        val epSuffix = if (episode != null) ":${episode.season}:${episode.number}" else ""
        val url = "https://example.com/stream/$type/$imdbId$epSuffix.json"

        val dto = withContext(Dispatchers.IO) {
            client.request(url = url).execute().fromJson<MyStreamResponseDto>()
        }

        dto.streams.forEach { stream ->
            if (stream.url.isNullOrBlank()) return@forEach

            onLinkFound(
                Stream(
                    name  = stream.title ?: "Stream",
                    url   = stream.url,
                    flags = buildSet {
                        if (stream.behaviorHints?.notWebReady == true) {
                            add(Flag.ThirdPartyGateway(name = stream.title ?: "", url = stream.url))
                        }
                    },
                )
            )
        }

        dto.subtitles?.forEach { sub ->
            if (sub.url.isNullOrBlank()) return@forEach
            onLinkFound(
                Subtitle(
                    name     = sub.lang ?: "Unknown",
                    url      = sub.url,
                    language = sub.lang,
                )
            )
        }
    }
}
```

## Performance and cancellation (MUST)

- Keep extraction deterministic and debuggable.
- Respect coroutine cancellation — do not ignore it.
- Avoid infinite retries or unbounded parallel requests.
