# 02 — Repository Structure

## Template setup (one-time)

- If you created this repo from the provider template, ensure **"Include all branches"** was enabled.
  The template CI setup depends on the additional branch.
- Configure shared provider metadata once in the root `build.gradle.kts` (inside a `subprojects { ... }` block):

  ```kotlin
  subprojects {
      flxProvider {
          author(
              name = "your-github-handle",
              image = "https://github.com/your-github-handle.png",
              socialLink = "https://github.com/your-github-handle",
          )
          setRepository("https://github.com/your-handle/your-providers-repo")
      }
  }
  ```

## What lives where

- `providers/<ProviderModule>/` — each provider is its own Android library module.
- `settings.gradle.kts` — controls which provider modules are included.
- Root `build.gradle.kts` — applies the `flx-provider` plugin and sets common Android config.
- `gradle/libs.versions.toml` — version catalog for SDK and plugin versions.
- `core/util/` — shared utilities available to all provider modules in this repo.

## Creating a new provider module

1. Copy `providers/Stremio/` (or `providers/BasicDummyProvider/`) as a starting point:

   ```
   providers/
   └── MyProvider/
       ├── build.gradle.kts
       └── src/main/
           ├── kotlin/
           └── res/
   ```

2. Add the module name to `settings.gradle.kts`:

   ```kotlin
   include(
       "Stremio",
       "Trakt",
       "MyProvider",
   )

   rootProject.children.forEach {
       it.projectDir = file("providers/${it.name}")
   }
   ```

3. Update the new module's `build.gradle.kts`:
   - `android { namespace = "com.yourorg.provider.my_provider" }`
   - `flxProvider { id = "prov-my-provider" versionMajor = 1 ... }`
   - `dependencies { implementation(libs.core.stubs.provider) }`

4. Create the entry class (extends `ProviderPlugin`, annotated with `@FlixclusiveProvider`).

## Provider module shape

```
<ProviderModule>/
├── build.gradle.kts
└── src/
    └── main/
        ├── kotlin/
        │   └── com/yourorg/provider/my_provider/
        │       ├── api/
        │       │   ├── search/
        │       │   ├── catalog/
        │       │   ├── metadata/
        │       │   └── links/
        │       ├── settings/
        │       ├── common/
        │       └── MyProviderPlugin.kt
        └── res/
```

- The package folder structure does not need to match the module name exactly.
- The `build.gradle.kts` and `src/main` source set must always exist.

## Real-world examples

- Study `providers/Stremio/` for capability-driven layout (search, catalog, metadata, links, cross-match).
- Study `providers/Trakt/` for tracker integration, OAuth flows, and DataStore token storage.
