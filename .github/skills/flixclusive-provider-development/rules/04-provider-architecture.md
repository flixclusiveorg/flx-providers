# 04 — Provider Architecture

## Entry class (`ProviderPlugin`)

Every provider MUST have a single entry class that:

- Extends `ProviderPlugin`
- Is annotated with `@FlixclusiveProvider`
- Exposes capability APIs via `suspend` getters

```kotlin
@FlixclusiveProvider
class MyProviderPlugin : ProviderPlugin() {

    private val client by lazy { OkHttpClient() }

    private val searchApi by lazy {
        MySearchApi(client = client, providerId = id)
    }

    private val catalogApi by lazy {
        MyCatalogApi(client = client, providerId = id)
    }

    private val metadataApi by lazy {
        MyMetadataApi(client = client)
    }

    private val mediaLinkApi by lazy {
        MyMediaLinkApi(client = client)
    }

    // All getters are suspend and return nullable types.
    // Overriding with a non-null return is fine for supported capabilities.
    override suspend fun getSearchApi(context: Context): SearchProviderApi    = searchApi
    override suspend fun getCatalogApi(context: Context): CatalogProviderApi  = catalogApi
    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi = metadataApi
    override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi    = mediaLinkApi

    // Return null for unsupported capabilities
    override suspend fun getCrossMatchApi(context: Context): CrossMatchProviderApi? = null
    override suspend fun getTrackerApi(context: Context): TrackerProviderApi?       = null
}
```

## Lateinit runtime fields

`ProviderPlugin.manifest` and `ProviderPlugin.settings` are initialized by the host app **after** the
class is instantiated.

**MUST NOT** read `manifest` or `settings` in:
- `init {}` blocks
- property initializers (e.g., `val foo = manifest.id`) — this will crash

**MUST** read them inside:
- `by lazy { ... }` blocks (evaluated on first access, after initialization)
- Inside capability getters or methods
- Inside `SettingsScreen()` composable

```kotlin
// WRONG — crashes because manifest is not yet set
val myId = manifest.id  // ❌

// CORRECT — lazy evaluates after host has set manifest
private val searchApi by lazy {
    MySearchApi(providerId = id)  // id = manifest.id, safe here ✅
}
```

## Capability APIs

- Implement each capability as a focused class (e.g., `MyProviderSearchProviderApi : SearchProviderApi`).
- **Create once via `by lazy` and reuse** — do not create a new instance per getter call.
- Capability getters return nullable types. Return `null` when a capability is not supported.

## Settings and state

- Persist user configuration and auth state using `provider.settings` (a `DataStore<Preferences>`).
- Keep preference keys as `private const val` constants in one place.
- Use typed DataStore extension helpers (from `core-stubs` DataStore extensions).

## Context usage

- `get*Api(context)` provides an Android `Context`.
- Do not store `Context` in a long-lived property; pass it where needed.

## Settings UI (optional)

- Override `SettingsScreen()` only if the provider requires custom settings or auth.
- Extract composables into separate files/functions for readability.

```kotlin
@Composable
override fun SettingsScreen() {
    MyProviderSettingsScreen(settings = settings)
}
```

## Real-world reference

- `providers/Stremio/src/main/kotlin/.../StremioPlugin.kt` — capability-driven entry class
- `providers/Trakt/src/main/kotlin/.../TraktPlugin.kt` — tracker + auth entry class
