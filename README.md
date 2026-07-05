# Flixclusive Providers

<a href="https://github.com/flixclusiveorg/flx-providers/actions/workflows/build.yml"><img src="https://github.com/flixclusiveorg/flx-providers/actions/workflows/build.yml/badge.svg" alt="Build"></a>
<a href="https://discord.gg/7yPSPveReu"><img src="https://img.shields.io/discord/1255770492049162240?label=discord&labelColor=7289da&color=2c2f33&style=flat-square" alt="Discord"></a>
<a href="https://flixclusiveorg.github.io/provider-docs/"><img src="https://img.shields.io/badge/docs-provider--docs-blue?style=flat-square" alt="Docs"></a>
<a href="https://flixclusiveorg.github.io/core-stubs/"><img src="https://img.shields.io/badge/api-core--stubs-green?style=flat-square" alt="API Reference"></a>
<a href="https://github.com/flixclusiveorg/flx-providers/tree/builds"><img src="https://img.shields.io/badge/releases-builds%20branch-orange?style=flat-square" alt="Releases"></a>

The official collection of provider extensions for [Flixclusive](https://github.com/flixclusiveorg/Flixclusive). Each provider is a pluggable module that connects Flixclusive to an external media source — fetching titles, metadata, streams, subtitles, and watchlist data on the user's behalf.

---

## Table of Contents

- [About](#about)
- [Providers](#providers)
- [Roadmap](#roadmap)
- [Build & Deploy](#build--deploy)
- [Contributing](#contributing)
- [Legal / DMCA Disclaimer](#legal--dmca-disclaimer)
- [Resources](#resources)

---

## About

A **provider** is a pluggable extension that runs inside Flixclusive and adapts it to a specific third-party service or API. Providers are independently versioned, packaged as `.flx` artifacts, and loaded at runtime by the app.

Each provider exposes only the capabilities its source supports:

| Capability | What it does |
|---|---|
| Catalog | Exposes browsable home-screen sections |
| Search | Returns paginated results for a query |
| Metadata | Fetches full movie/show details |
| Media Links | Emits streams and/or subtitles for playback |
| Cross-match | Resolves IDs across providers |
| Tracker | Integrates with watchlist management and scrobbling services |

---

## Providers

| Provider                     | Capabilities                                        |
|------------------------------|-----------------------------------------------------|
| [Stremio](providers/Stremio) | Catalog, Search, Metadata, Media Links, Cross-match |
| [Trakt](providers/Trakt)     | Catalog, Search, Metadata, Tracker, Cross-match     |
| [TMDB](providers/TMDB)       | Catalog, Search, Metadata, Cross-match, Media Links |

---

## Roadmap

- [x] Stremio
- [x] Trakt
- [x] TMDB
- [ ] Simkl
- [ ] Letterboxd

---

## Build & Deploy

> **Prerequisites:** Android SDK (API 23–36), JDK 17+, `adb` in your `PATH`.

```bash
# Build and package a provider artifact (.flx)
./gradlew :ProviderName:make

# Deploy to a connected device or emulator
./gradlew :ProviderName:deployWithAdb

# Deploy using the debug build of Flixclusive
./gradlew :ProviderName:deployWithAdb --debug-app

# Deploy and wait for a debugger to attach
./gradlew :ProviderName:deployWithAdb --wait-for-debugger
```

After deployment the provider appears in Flixclusive's provider list. Use the **"Test provider"** option in the app to run an automated capability check.

To attach Android Studio's debugger after `--wait-for-debugger`, click the **"Attach debugger to Android process"** button (bug icon with arrow) and select the Flixclusive process.

<img src="https://i.imgur.com/d1k3ZZD.png" alt="Attach debugger to Android process">

---

## Contributing

Contributions are welcome — whether that's a new provider, a bug fix, or an improvement to an existing one. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions, code conventions, and the pull request process.

---

## Legal / DMCA Disclaimer

This repository **does not host, store, or distribute any pirated, copyrighted, or otherwise unlicensed media content**. The providers in this repository are software adapters that interface with publicly accessible third-party APIs and services. No media files, streams, or infringing content are embedded in or served by this codebase.

**Users are solely responsible** for ensuring that their use of these providers complies with the terms of service of the connected third-party platforms and with the applicable laws in their jurisdiction. The maintainers of this repository accept no liability for how end users choose to use the software.

If you believe any content in this repository infringes your rights, please [open an issue](https://github.com/flixclusiveorg/flx-providers/issues) or contact the repository maintainers directly.

---

## Resources

- [Provider Documentation](https://flixclusiveorg.github.io/provider-docs/)
- [Core Stubs API Reference](https://flixclusiveorg.github.io/core-stubs/)
- [Discord Community](https://discord.gg/7yPSPveReu)
