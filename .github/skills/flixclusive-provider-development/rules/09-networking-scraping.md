# 09 — Networking & Scraping Rules

## HTTP client

- Prefer a single `OkHttpClient` shared across capability APIs.
- Configure timeouts intentionally (avoid default infinite waits).
- Set a consistent User-Agent if the upstream site requires it.

## Requests

- Centralize base URLs and endpoints as constants.
- Handle redirects explicitly when extracting final stream URLs.
- Treat non-2xx responses as errors; surface actionable messages.

## Parsing

- Use Jsoup for HTML parsing; keep selectors resilient (avoid overly brittle DOM paths).
- For JSON APIs, use Kotlinx Serialization (or a minimal, well-scoped parser).
- Prefer Core Stubs helpers when available:
  - `OkHttpClient.request(...)`
  - `Response.fromJson<T>(...)`
- Keep parsing code pure and testable (input string -> output model).

## WebView fallback

- Use `WebViewInterceptor` only when a site requires browser execution (JS challenges, cookie bootstrap, CAPTCHA).
- Prefer attaching it to a client only for the requests that need it:
  - `val clientWithWebView = client.addWebViewInterceptor(myWebViewInterceptor)`
- Keep WebView interception isolated from the main capability API logic.
- Always call `destroy()` when the interception flow is finished.

Common pitfalls:

- Memory leaks from long-lived WebView instances
- Threading mistakes (WebView APIs often require the main thread)
- Excessive resource usage from repeated interception

## Reliability

- Expect upstream changes (HTML structure, API fields).
- Add guardrails:
  - null checks
  - fallback selectors
  - clear errors when a required field is missing

## Concurrency

- Avoid unbounded parallel requests.
- Prefer sequential extraction unless you have clear performance needs.
- Respect coroutine cancellation.

## Ethics and safety

- Don’t hard-code user credentials.
- Don’t exfiltrate data.
- Don’t store or log sensitive session data.
