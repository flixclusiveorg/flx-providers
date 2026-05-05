# 03 — Gradle Rules (Provider Template)

## General

- This repo uses Kotlin DSL (`build.gradle.kts`).
- The root build applies the `flx-provider` plugin to all subprojects; do not re-apply it in provider modules.
- Prefer adding dependencies via the version catalog (`gradle/libs.versions.toml`).

## Versions (keep plugin + SDK compatible)

- Provider SDK and plugin versions are managed in `gradle/libs.versions.toml`.
- When upgrading `core-stubs`, consider upgrading `core-gradle` in the same change so the Gradle plugin and SDK stay compatible.

## Root plugin wiring (recommended)

- The recommended template setup applies `flx-provider` once from the root `build.gradle.kts` (often inside `subprojects { ... }`).
- Provider modules should only contain module-specific Android config and `flxProvider { ... }` values.

## Provider module essentials

In `providers/<ProviderModule>/build.gradle.kts`:

- Always depend on the provider SDK:
  ```kotlin
  dependencies {
      implementation(libs.core.stubs.provider)
  }
  ```
- Keep the module’s namespace unique:
  ```kotlin
  android {
      namespace = "com.yourorg.provider.my_provider"
  }
  ```
- Configure provider metadata via `flxProvider { ... }`:
  - `id` is required and must be stable across releases.
  - Version fields (`versionMajor`, `versionMinor`, `versionPatch`, `versionBuild`) must be set.
  - Use `status` to reflect real availability.

## Provider identity + metadata (`flxProvider`)

- You do not construct `ProviderMetadata` / `ProviderManifest` manually.
  - The `flx-provider` plugin generates them from `flxProvider { ... }`.
- Common module-level fields worth setting:
  - Identity: `id` (stable), `providerName`, `description`
  - Versioning: `versionMajor`, `versionMinor`, `versionPatch`, `versionBuild`
  - Content spec: `language`, `providerType`, `adult`
  - Build options: `requiresResources`, `excludeFromUpdaterJson`
  - Optional: `changelog` (Markdown string)

Example:

```kotlin
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType

flxProvider {
    id = "prov-your-provider"
    providerName = "Your Provider"
    description = "Short description."

    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0

    status = ProviderStatus.Working
    language = Language.Multiple
    providerType = ProviderType.All
    adult = false

    requiresResources = true
    // excludeFromUpdaterJson = true
}
```

## Dependency guidance

- Prefer `compileOnly(...)` for libraries provided by the host app/runtime (this template uses that for OkHttp/Jsoup/Compose).
- Use `implementation(...)` when the dependency must ship inside the provider artifact.

## Unsupported libraries

- `fatImplementation` is no longer used in this template.
- If a library must be included in the provider artifact, use `implementation(...)`.
- If the host app/runtime already provides the library, keep it on `compileOnly(...)`.

## Common tasks

- Build/package a provider:
  - `./gradlew :<ProviderModule>:make`
- Deploy to device/emulator (requires ADB + the app installed):
  - `./gradlew :<ProviderModule>:deployWithAdb`
  - Add `--debug-app` for debug builds of the app.
  - Add `--wait-for-debugger` to attach a debugger.
