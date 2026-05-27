# 10 — Testing & Debugging

## What to unit test (SHOULD)

Prioritize tests for:

- JSON/HTML parsing functions (input `String` → output model)
- URL building/normalization
- Pagination logic (`hasNextPage` derivation)
- Filter state mapping

Keep extraction IO thin so business logic is easy to test in isolation.

## Do not instantiate `ProviderPlugin` in unit tests (MUST)

`ProviderPlugin.manifest` and `ProviderPlugin.settings` are `lateinit` — they are assigned by the
host app at runtime. Instantiating `ProviderPlugin` in a unit test will throw `UninitializedPropertyAccessException`.

**MUST** test capability API classes directly instead:

```kotlin
// WRONG
val plugin = MyProviderPlugin()
plugin.getSearchApi(context) // manifest is not set — crashes ❌

// CORRECT — test the capability class directly
class MySearchApiTest {
    private val api = MySearchApi(
        client     = OkHttpClient(),
        providerId = "prov-my-provider",
    )

    @Test
    fun `search returns paginated results`() = runTest {
        val result = api.search(query = "Inception", page = 1, filters = api.filters)
        assertTrue(result.results.isNotEmpty())
    }
}
```

## Test dependencies

```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

## Running unit tests

```bash
./gradlew :MyProvider:testDebugUnitTest
```

## In-app testing flow

```bash
# 1. Build and package
./gradlew :MyProvider:make

# 2. Deploy to a connected emulator or device
./gradlew :MyProvider:deployWithAdb               # release/pre-release app
./gradlew :MyProvider:deployWithAdb --debug-app   # debug app

# 3. Attach debugger (optional)
./gradlew :MyProvider:deployWithAdb --wait-for-debugger
# Then click "Attach debugger to Android process" in Android Studio
```

After deployment, use the **"Test provider"** option in Flixclusive to run the built-in capability check.

## Logging (SHOULD)

Use Core Stubs logging helpers from `com.flixclusive.core.util.log`:

```kotlin
debugLog("Loading search results for: $query")
warnLog("Search returned no results for: $query")

try {
    // ...
} catch (e: Exception) {
    errorLog(e)
    throw e  // re-throw; do not swallow
}
```

## Debug mindset (SHOULD)

- Reproduce with the smallest possible input first.
- Validate each stage independently: search → metadata → links.
- Prefer deterministic failures (throw clear exceptions) over silent empty results.
- Convert frequently used Gradle commands into Android Studio run configurations for convenience.
