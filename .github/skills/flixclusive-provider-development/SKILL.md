---
name: flixclusive-provider-development
description: >
  Comprehensive skill for building Flixclusive providers in this repository. Use this when asked to
  create a new provider module, implement provider capabilities (catalog/search/metadata/media links/
  cross-match/tracker), wire a ProviderPlugin, add a SettingsScreen, update Gradle, debug build/deploy,
  or refactor provider code.
---

# Flixclusive Provider Development Skill

This skill governs Flixclusive **provider** development in this template repository. It is designed
around the provider SDK exposed by `core-stubs`.

## How to Use

All rules are in the `rules/` directory alongside this file. **Read every rule file before generating
or editing code.** Each file is a self-contained rule set for a specific concern.

| Rule File | When to Read |
|---|---|
| `rules/01-anti-hallucination.md` | **Always.** Read before every task. These override all other rules. |
| `rules/02-repo-structure.md` | When creating a provider module or navigating this template. |
| `rules/03-gradle.md` | When touching `build.gradle.kts`, adding dependencies, or configuring `flxProvider`. |
| `rules/04-provider-architecture.md` | When wiring a `ProviderPlugin` and capability APIs. |
| `rules/05-coding-style.md` | When writing any Kotlin code (non-UI). |
| `rules/06-capabilities.md` | When implementing catalog/search/metadata/media links/cross-match/tracker. |
| `rules/07-media-links.md` | When extracting streams/subtitles and emitting `MediaLink` values. |
| `rules/08-settings-auth.md` | When adding user settings, login/auth flows, or storing tokens. |
| `rules/09-networking-scraping.md` | When adding HTTP calls, HTML parsing, or site-specific extraction logic. |
| `rules/10-testing-debugging.md` | When debugging providers, writing tests, or using the app’s test provider flow. |
| `rules/11-release-packaging.md` | When versioning, building, deploying, or preparing releases. |
| `rules/12-naming-conventions.md` | When naming modules, packages, provider IDs, and files. |

## Key Principle

> When in doubt, **read existing project code first**. Never assume. Never invent. Search, read,
> confirm — then generate.

## Core SDK Reference

- Primary docs: [core-stubs docs](https://flixclusiveorg.github.io/core-stubs/)
- Provider docs: [provider-docs](https://flixclusiveorg.github.io/provider-docs/)
- Source fallback (when local sources/indices are missing): [flixclusiveorg/core-stubs](https://github.com/flixclusiveorg/core-stubs)

## Provider Docs Index (use as the default guide map)

When implementing or documenting a feature, read the matching guide first:

| Topic | Guide |
|---|---|
| Installation | [Getting started: Installation](https://flixclusiveorg.github.io/provider-docs/getting-started/installation) |
| Development workflow | [Getting started: Development](https://flixclusiveorg.github.io/provider-docs/getting-started/development) |
| Provider architecture | [Provider: Overview](https://flixclusiveorg.github.io/provider-docs/provider/overview) |
| Gradle + metadata | [Provider: Configuration](https://flixclusiveorg.github.io/provider-docs/provider/configuration) |
| Entry class + capabilities | [Provider: Creating a provider](https://flixclusiveorg.github.io/provider-docs/provider/create_provider) |
| Testing | [Provider: Testing a provider](https://flixclusiveorg.github.io/provider-docs/provider/test_provider) |
| Search | [Implementation: Searching media](https://flixclusiveorg.github.io/provider-docs/impl/searching_media) |
| Search filters | [Implementation: Search filters](https://flixclusiveorg.github.io/provider-docs/impl/impl_filters) |
| Catalogs | [Implementation: Loading catalogs](https://flixclusiveorg.github.io/provider-docs/impl/loading_catalogs) |
| Metadata | [Implementation: Fetching metadata](https://flixclusiveorg.github.io/provider-docs/impl/fetching_metadata) |
| Media links | [Implementation: Fetching media links](https://flixclusiveorg.github.io/provider-docs/impl/fetching_links) |
| Settings UI | [Implementation: Settings UI](https://flixclusiveorg.github.io/provider-docs/impl/impl_settings) |
| Cross-match | [Implementation: Cross-match](https://flixclusiveorg.github.io/provider-docs/impl/cross_match) |
| Tracker | [Implementation: Tracker](https://flixclusiveorg.github.io/provider-docs/impl/tracker) |
| WebView interception | [Advanced: WebView](https://flixclusiveorg.github.io/provider-docs/advanced/webview) |
| Dependency packaging | [Advanced: Unsupported libraries](https://flixclusiveorg.github.io/provider-docs/advanced/unsupported_libs) |
| Kotlin style | [Best practices: Coding style](https://flixclusiveorg.github.io/provider-docs/best_practices/coding_style) |
