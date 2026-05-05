# 10 — Testing & Debugging

## What to test

- Prioritize unit tests for:
  - parsing functions (HTML/JSON -> models)
  - URL normalization
  - pagination logic
  - filter mapping

Keep extraction IO thin so logic is easy to test.

## Unit testing guidance (important)

- Avoid instantiating `ProviderPlugin` in unit tests.
  - `manifest` and `settings` are `lateinit` and are assigned by the host app at runtime.
- Prefer testing capability API classes directly (search/catalog/metadata/links implementations).

## Test dependencies

If you add tests to a provider module, prefer these (available in this template’s version catalog):

```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

## Running tests

For an Android library provider module, common tasks include:

- `./gradlew :<ProviderModule>:testDebugUnitTest`

(Confirm available tasks if the module’s build type differs.)

## Manual debugging

- It’s recommended to convert commonly used Gradle commands into Android Studio run configurations.

- Build/package:
  - `./gradlew :<ProviderModule>:make`
- Deploy to a connected emulator/device:
  - `./gradlew :<ProviderModule>:deployWithAdb`
  - `--debug-app` for debug app builds
  - `--wait-for-debugger` to attach a debugger

## In-app testing flow

- Build/package: `./gradlew :<ProviderModule>:make`
- Deploy: `./gradlew :<ProviderModule>:deployWithAdb` (or add `--debug-app` / `--wait-for-debugger`)

## Logging

- Prefer Core Stubs logging helpers from `com.flixclusive.core.util.log`:
  - `debugLog("...")`
  - `warnLog("...")`
  - `errorLog(e)`

## Debug mindset

- Reproduce with the smallest input.
- Validate each stage (search -> metadata -> links) independently.
- Prefer deterministic errors over silent empty results.
