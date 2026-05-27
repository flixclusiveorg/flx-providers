# 09 — Networking & Scraping Rules

## HTTP client (MUST)

- Share a single `OkHttpClient` instance across all capability APIs.
- Create it with `by lazy` in `ProviderPlugin` and pass it into each API class.
- Configure timeouts intentionally — do not rely on OkHttp defaults.
- Set a consistent `User-Agent` header if the upstream site requires one.

### Concrete example — OkHttp with HTTP caching (adapted from `providers/Stremio/StremioClient.kt`)

```kotlin
class MyOkHttpClient(context: Context) {
    val instance: OkHttpClient by lazy {
        val cacheDir = File(context.cacheDir, "my_provider_http_cache")
        val cache = Cache(cacheDir, 10L * 1024 * 1024) // 10 MB

        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=60, stale-while-revalidate=604800")
                    .build()
            }
            .build()
    }
}
```

## Requests (SHOULD)

- Centralize base URLs and endpoint paths as `private const val` constants.
- Handle redirects explicitly when extracting final stream URLs.
- Treat non-2xx responses as errors and surface clear, actionable messages.

## Parsing (MUST)

- Use Jsoup for HTML parsing; keep CSS selectors resilient.
- Use Kotlinx Serialization (`@Serializable` DTOs) for JSON APIs.
- Use Core Stubs helpers when available:
  - `OkHttpClient.request(url, method, headers, body)` — builds and executes a request
  - `Response.fromJson<T>(errorMessage)` — parses the body as JSON
  - `Response.asJsoup()` — parses the body as an HTML document
- Keep parsing functions pure and testable: (input `String` → output model).

## WebView fallback (SHOULD)

Use `WebViewInterceptor` only when a site requires browser execution (JS challenges, CAPTCHA, cookie bootstrap):

```kotlin
val clientWithWebView = client.addWebViewInterceptor(myCaptchaInterceptor)
```

- Keep WebView interception isolated — do not attach it to the main API client.
- Always call `destroy()` on the interceptor when the flow is finished.
- Common pitfalls: memory leaks from long-lived WebView instances; main-thread violations.

## Reliability (SHOULD)

- Expect upstream HTML structure to change — avoid brittle single-path selectors.
- Null-check selectors and emit clear errors when a required field is missing.
- Do not silently return default/empty values that hide real failures.

## Concurrency (MUST)

- Avoid unbounded parallel requests.
- Prefer sequential extraction unless you have clear performance requirements.
- Respect coroutine cancellation.

## Ethics and security (MUST)

- Never hard-code user credentials in source code.
- Never log or exfiltrate sensitive session data.
- Never store authentication material in plaintext in insecure storage.
