# Provider Template

<a href="https://discord.gg/7yPSPveReu"><img src="https://img.shields.io/discord/1255770492049162240?label=discord&labelColor=7289da&color=2c2f33&style=for-the-badge" alt="Discord"></a>
<a href="https://flixclusiveorg.github.io/provider-docs/"><img src="https://img.shields.io/badge/docs-provider--docs-blue?style=for-the-badge" alt="Docs"></a>
<a href="https://flixclusiveorg.github.io/core-stubs/"><img src="https://img.shields.io/badge/api-core--stubs-green?style=for-the-badge" alt="API Reference"></a>

This is the template repository for creating a custom provider for the [Flixclusive](https://github.com/flixclusiveorg/Flixclusive) application.

> **Important:** When using this template, enable **"Include all branches"**. The `build` branch is required by the GitHub CI that automates provider packaging and publishing.

---

## What is a Provider?

A **provider** is an extension that allows Flixclusive users to customize where their content comes from. Think of it like a browser extension for a streaming app — providers run inside Flixclusive and fetch media data (titles, metadata, streams, subtitles) from external sources on behalf of the user.

Each provider is composed of focused capability APIs. You implement only the capabilities your source supports:

| Capability | Interface | What it does |
|---|---|---|
| Search | `SearchProviderApi` | Returns paginated search results for a query |
| Catalog | `CatalogProviderApi` | Exposes home-screen browsable sections |
| Metadata | `MediaMetadataProviderApi` | Fetches full movie/show details |
| Media Links | `MediaLinkProviderApi` | Emits streams and/or subtitles for playback |
| Cross-match | `CrossMatchProviderApi` | Resolves your IDs from another provider's IDs |
| Tracker | `TrackerProviderApi` | Integrates with list management + scrobble services |

---

## Getting Started

### 1. Create your repository from this template

- Click **"Use this template"** → **"Create a new repository"**
- Enable **"Include all branches"** (the `build` branch is required for CI)

### 2. Configure author and repository metadata

Open the root `build.gradle.kts` and fill in the shared `flxProvider` block (usually inside `subprojects { ... }`):

```kotlin
subprojects {
    flxProvider {
        author(
            name = "your-github-handle",
            image = "https://github.com/your-github-handle.png",
            socialLink = "https://github.com/your-github-handle",
        )
        setRepository("https://github.com/your-github-handle/your-providers-repo")
    }
}
```

### 3. Create or customize a provider module

The `providers/` folder contains a working example provider (`Stremio`, `Trakt`) you can use as a reference. To create a new provider:

1. Copy the example module folder: `providers/BasicDummyProvider/` → `providers/MyProvider/`
2. Register it in `settings.gradle.kts`:

   ```kotlin
   include(
       "BasicDummyProvider",
       "MyProvider",  // <- your new module
   )
   ```

3. Update `providers/MyProvider/build.gradle.kts`:

   ```kotlin
   import com.flixclusive.model.provider.Language
   import com.flixclusive.model.provider.ProviderStatus
   import com.flixclusive.model.provider.ProviderType

   android {
       namespace = "com.example.provider.my_provider"
   }

   flxProvider {
       id = "prov-my-provider"       // stable unique ID — never change after first release
       providerName = "My Provider"
       description  = "Short description of what this provider does."

       versionMajor = 1
       versionMinor = 0
       versionPatch = 0
       versionBuild = 0

       status       = ProviderStatus.Working
       language     = Language.Multiple
       providerType = ProviderType.All
       adult        = false
   }

   dependencies {
       implementation(libs.core.stubs.provider)
   }
   ```

4. Create your entry class:

   ```kotlin
   import com.flixclusive.provider.FlixclusiveProvider
   import com.flixclusive.provider.ProviderPlugin

   @FlixclusiveProvider
   class MyProvider : ProviderPlugin() {
       // expose only the capabilities your source supports
   }
   ```

### 4. Build and deploy

```bash
# Build and package the provider artifact (.flx)
./gradlew :MyProvider:make

# Deploy to a connected emulator or device (builds first automatically)
./gradlew :MyProvider:deployWithAdb

# Deploy using the debug build of Flixclusive
./gradlew :MyProvider:deployWithAdb --debug-app

# Deploy and wait for a debugger to attach
./gradlew :MyProvider:deployWithAdb --wait-for-debugger
```

After deployment, the provider appears in Flixclusive's provider list. Use the **"Test provider"** option in the app to run an automated capability check.

To attach Android Studio's debugger after `--wait-for-debugger`, click the **"Attach debugger to Android process"** button (bug icon with arrow) and select the Flixclusive process.

<img src="https://i.imgur.com/d1k3ZZD.png" alt="Attach debugger to Android process">

---

## Project Structure

```
providers/
└── MyProvider/
    ├── build.gradle.kts           # Provider-specific Gradle config (id, version, type, etc.)
    └── src/main/
        ├── kotlin/                # Your provider's Kotlin source
        └── res/                   # Resources (only if requiresResources = true)
build.gradle.kts                   # Root build: applies flx-provider plugin, shared author/repo metadata
settings.gradle.kts                # Lists all provider modules
gradle/libs.versions.toml          # Manages SDK + plugin versions
```

---

## Capability Quick Reference

Each capability is a focused interface. Implement only what your source supports and return `null` from the others.

```kotlin
@FlixclusiveProvider
class MyProvider : ProviderPlugin() {

    private val client by lazy { OkHttpClient() }

    private val searchApi by lazy { MySearchApi(client, id) }
    private val catalogApi by lazy { MyCatalogApi(client, id) }
    private val metadataApi by lazy { MyMetadataApi(client) }
    private val mediaLinkApi by lazy { MyMediaLinkApi(client) }

    override suspend fun getSearchApi(context: Context)    = searchApi
    override suspend fun getCatalogApi(context: Context)   = catalogApi
    override suspend fun getMetadataApi(context: Context)  = metadataApi
    override suspend fun getMediaLinkApi(context: Context) = mediaLinkApi

    // Return null for unsupported capabilities:
    override suspend fun getCrossMatchApi(context: Context) = null
    override suspend fun getTrackerApi(context: Context)    = null
}
```

---

## Additional Resources

- [**Provider Documentation**](https://flixclusiveorg.github.io/provider-docs/)
- [**Core Stubs API Reference**](https://flixclusiveorg.github.io/core-stubs/)
- [**Discord Community**](https://discord.gg/7yPSPveReu)

## Support

Join the Discord community for questions, reviews, and developer support:

<a href="https://discord.gg/7yPSPveReu"><img src="https://img.shields.io/discord/1255770492049162240?label=discord&labelColor=7289da&color=2c2f33&style=for-the-badge" alt="Discord"></a>
