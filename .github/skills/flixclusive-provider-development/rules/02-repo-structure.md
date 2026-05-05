# 02 — Repository Structure

## Template setup (one-time)

- If you created this repo from the provider template, ensure **"Include all branches"** was enabled.
   - The template CI setup depends on the additional branch.
- Configure shared provider metadata once in the root `build.gradle.kts` (commonly in a `subprojects { ... }` block):
   - `flxProvider.author(...)`
   - `flxProvider.setRepository("https://github.com/<you>/<repo>")`

## What lives where

- `providers/<ProviderModule>/` — each provider is its own Android library module.
- `settings.gradle.kts` — controls which provider modules are included.
- Root `build.gradle.kts` — applies the `flx-provider` plugin and sets common Android config.

## Creating a new provider module (recommended workflow)

1. Copy `providers/BasicDummyProvider/` into a new folder:
   - Example: `providers/MyNewProvider/`
2. Add the module name to `settings.gradle.kts`:
   ```kotlin
   include(
       "BasicDummyProvider",
       "MyNewProvider",
   )
   ```
3. Keep the existing projectDir mapping (this template expects modules in `providers/`):
   ```kotlin
   rootProject.children.forEach {
       it.projectDir = file("providers/${it.name}")
   }
   ```
4. Update the new module’s `build.gradle.kts`:
   - `android { namespace = "..." }`
   - `flxProvider { id = "..." versionMajor/minor/patch/build = ... }`
   - Dependencies (at minimum `implementation(libs.core.stubs.provider)`)
5. Rename packages/classes:
   - Provider entry class should extend `ProviderPlugin` and be annotated with `@FlixclusiveProvider`.

## Provider module shape

Provider modules are standard Kotlin/Android library modules. At minimum, keep:

```
<ProviderModule>/
├── build.gradle.kts
└── src/
   └── main/
      ├── kotlin/
      └── res/
```

- The package folder structure under `src/main/kotlin` does not need to match the module name.
- The `build.gradle.kts` and `src/main` source set must exist.

## Provider entry point

A provider’s entry point is the class extending `ProviderPlugin` (see `providers/BasicDummyProvider/.../BasicDummyProvider.kt`).

- Cache API instances (use `by lazy`) and return them from capability getters.
- Return `null` from capability getters you don’t support.
