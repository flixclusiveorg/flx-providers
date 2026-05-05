# 07 — Media Link Extraction Rules

## Declare what you emit

Implement `MediaLinkProviderApi.supportedLinkTypes` accurately.

- Streams only:
  - `override val supportedLinkTypes = setOf(MediaLinkType.STREAMS)`
- Subtitles only:
  - `override val supportedLinkTypes = setOf(MediaLinkType.SUBTITLES)`
- Both:
  - `override val supportedLinkTypes = setOf(MediaLinkType.STREAMS, MediaLinkType.SUBTITLES)`

## Emitting links

- `getLinks(...)` returns a `Flow<MediaLink>`.
- Prefer `flow { ... }` and `emit(...)` each discovered link.
- Emit multiple qualities/variants as separate `Stream` items.

## IO + parsing

- Do network + HTML parsing on an IO dispatcher:
  - Prefer `FlxDispatchers.withIOContext { ... }` for blocking work.
- Prefer Core Stubs helpers when available:
  - `OkHttpClient.request(...)`
  - `Response.asJsoup()` for HTML parsing

## Link quality and correctness

- Never emit empty/invalid URLs.
- `Stream.name` should be human-readable (quality, source, server name).
- For subtitle language codes, use standard short codes when possible (e.g., `en`, `es`, `pt-BR`).

## Flags and auth

- If the stream/subtitle requires auth headers, attach `Flag.RequiresAuth(customHeaders = ...)`.
- If the URL is a handoff to another site, attach `Flag.ThirdPartyGateway(...)`.
- `MediaLink` does not expose `isThirdPartyGateway` or `thirdPartyGatewayInfo` — model and inspect gateway/auth constraints via `flags`.
- When you need to read a specific flag type from a `MediaLink`, you can either filter `flags` or use the helper:
  - `import com.flixclusive.model.provider.link.MediaLink.Companion.getFlagOfType`
  - `val gateway = mediaLink.getFlagOfType<Flag.ThirdPartyGateway>()`
- Prefer `Flag.ThirdPartyGateway` for cross-site handoff metadata.
- `Flag.Trusted` exists but is deprecated in Core Stubs; avoid introducing new usages.

## Performance and cancellation

- Do network + parsing on an appropriate dispatcher (IO).
- Respect cancellation; avoid infinite retries.
- Keep extraction deterministic and debuggable (small steps, clear errors).

## Concrete example (Flow + OkHttp + Jsoup + emitting streams)

Adapted from the Provider Docs “Fetching media links” guide. This example demonstrates:

- `supportedLinkTypes` declaration
- `flow { emit(...) }` usage
- `OkHttpClient.request(...)` + `FlxDispatchers.withIOContext { ... }`
- `Response.asJsoup()` parsing
- emitting `Stream(...)` with `Flag.ThirdPartyGateway(...)`

```kotlin
class TestMediaLinkApi(
  private val client: OkHttpClient,
) : MediaLinkProviderApi {
  override val supportedLinkTypes: Set<MediaLinkType> = setOf(MediaLinkType.STREAMS)

  override fun getLinks(
    film: FilmMetadata,
    episode: Episode?,
  ): Flow<MediaLink> = flow {
    val mediaType = film.filmType.type
    require(mediaType == "movie" || mediaType == "tv") {
      "Invalid media type: $mediaType"
    }

    val id = film.externalIds[FilmIdSource.TMDB] ?: film.id

    val response = FlxDispatchers.withIOContext {
      client.request(
        url = "https://www.themoviedb.org/${mediaType}/${id}/watch?locale=US",
      ).execute()
    }

    val html = response.asJsoup()

    html.select("div.ott_provider li a").forEach { element ->
      val href = element.attr("href")
      val title = element.attr("title")
      val logoUrl = element.select("img").attr("src")

      val providerName = title
        .split(" on ")
        .lastOrNull()
        ?.trim()
        .orEmpty()
        .ifBlank { "Unknown Provider" }

      val url = href
        .split("&r=")
        .getOrNull(1)
        ?.split("&")
        ?.firstOrNull()

      if (url != null) {
        val decodedUrl = URLDecoder.decode(url, "UTF-8")

        emit(
          Stream(
            name = providerName,
            description = title,
            url = decodedUrl,
            flags = setOf(
              Flag.ThirdPartyGateway(
                name = providerName,
                url = decodedUrl,
                logo = logoUrl,
                description = title,
              ),
            ),
          )
        )
      }
    }
  }
}
```
