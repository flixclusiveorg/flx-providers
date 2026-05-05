# 04 — Provider Architecture

## Entry class (`ProviderPlugin`)

- Every provider must have a single entry class that:
  - Extends `ProviderPlugin`
  - Is annotated with `@FlixclusiveProvider`
  - Exposes capability APIs via `getCatalogApi(context)`, `getSearchApi(context)`, `getMetadataApi(context)`, `getMediaLinkApi(context)`, `getCrossMatchApi(context)`, `getTrackerApi(context)`

- `Provider` may still exist as a deprecated alias for `ProviderPlugin`. Prefer `ProviderPlugin` for new code.

See the example implementation in `providers/BasicDummyProvider/.../BasicDummyProvider.kt`.

## Lateinit runtime state

`ProviderPlugin.manifest` and `ProviderPlugin.settings` are initialized by the host app.

- Do not read `manifest`/`settings` in property initializers or `init {}` blocks.
- Prefer `by lazy { ... }` or compute values inside capability getters so the fields are available.

## Capability APIs

- Implement each capability as a focused class (e.g., `MyProviderSearchProviderApi : SearchProviderApi`).
- Keep capability implementations stable:
  - Create them once (commonly via `by lazy`) and reuse.
  - Avoid creating a new API instance per call.
- Capability getters return nullable types (`...Api?`).
  - Return `null` when the provider does not support that capability.

## Settings and state

- Persist user configuration and auth state using `provider.settings` (`ProviderSettings`).
- Keep keys centralized (constants) and use typed getters/setters.

## Context usage

- `get*Api(context)` provides an Android `Context`.
- Prefer not to store `Context` long-term; pass it where needed.

## Settings UI (optional)

- Override `SettingsScreen()` only if you provide custom UI.
- Prefer extracting composables into separate functions/files for readability.

## Legacy `getApi(...)`

- `ProviderPlugin.getApi(context, client)` returns the deprecated `ProviderApi`.
- Only override this when you specifically need legacy integration.
- Prefer capability getters for new code.

## Concrete reference (copy/paste skeleton)

Use this as a starting point when wiring capability APIs.

```kotlin
@FlixclusiveProvider
class ExampleProviderPlugin : ProviderPlugin() {
  private val client by lazy { OkHttpClient() }

  // Create once; reuse for all calls.
  private val catalogApi by lazy {
    ExampleCatalogApi(
      client = client,
      providerId = id,
    )
  }

  private val searchApi by lazy {
    ExampleSearchApi(
      client = client,
      providerId = id,
    )
  }

  private val metadataApi by lazy {
    ExampleMetadataApi(
      client = client,
      providerId = id,
    )
  }

  private val mediaLinkApi by lazy {
    ExampleMediaLinkApi(
      client = client,
    )
  }

  override fun getCatalogApi(context: Context): CatalogProviderApi = catalogApi
  override fun getSearchApi(context: Context): SearchProviderApi = searchApi
  override fun getMetadataApi(context: Context): MetadataProviderApi = metadataApi
  override fun getMediaLinkApi(context: Context): MediaLinkProviderApi = mediaLinkApi

  // Optional capabilities: return null when unsupported.
  override fun getCrossMatchApi(context: Context): CrossMatchProviderApi? = null
  override fun getTrackerApi(context: Context): TrackerProviderApi? = null

  // Optional settings UI.
  // @Composable
  // override fun SettingsScreen() { ... }
}
```

Notes:

- Capability getters in `ProviderPlugin` are nullable (`...Api?`). Overriding with a non-null return type is OK for supported capabilities.
- Put `manifest`/`settings` reads inside `by lazy { ... }` blocks (or inside getters) to avoid touching lateinit runtime fields too early.
