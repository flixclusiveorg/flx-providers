# 12 — Naming Conventions

Follow existing patterns in `providers/Stremio/` and `providers/Trakt/`.

## Modules (MUST)

- Provider modules live under `providers/` and use **PascalCase**.
- Examples: `Stremio`, `Trakt`, `MyNewProvider`

## Packages (MUST)

- Use a stable reverse-DNS package: `com.flixclusive.provider.<module_name>`
- Use **snake_case** for the package segment, matching the module name.
- Examples:
  - `com.flixclusive.provider.stremio`
  - `com.flixclusive.provider.trakt`
  - `com.flixclusive.provider.my_new_provider`

## Entry class (MUST)

- The entry class extends `ProviderPlugin` and is annotated with `@FlixclusiveProvider`.
- Named after the provider with a `Plugin` suffix.
- Examples: `StremioPlugin`, `TraktPlugin`, `MyNewProviderPlugin`

## Capability API classes (MUST)

Name capability implementations with a clear provider prefix and capability suffix:

| Capability | Naming pattern | Example |
|---|---|---|
| Search | `<Provider>SearchProvider` | `StremioSearchProvider` |
| Catalog | `<Provider>CatalogProvider` | `StremioCatalogProvider` |
| Metadata | `<Provider>MetadataProvider` | `StremioMetadataProvider` |
| Media Links | `<Provider>LinkProvider` | `StremioLinkProvider` |
| Cross-match | `<Provider>CrossMatcher` | `StremioCrossMatcher` |
| Tracker | `<Provider>Tracker` | `TraktTracker` |

## Provider ID (`flxProvider.id`) (MUST)

- Lowercase + hyphen-separated.
- Prefix with `prov-`.
- Stable and globally unique; do not change after first release.
- No spaces, uppercase letters, or version numbers.
- Examples: `prov-stremio`, `prov-trakt`, `prov-my-source`

## Provider display name (`flxProvider.providerName`) (SHOULD)

- Human-readable Title Case.
- Keep it short — do not stuff version numbers or regions into the display name.
- Examples: `"Stremio"`, `"Trakt"`, `"My Source"`

## DTO classes (SHOULD)

- Suffix data transfer objects with `Dto`: `StreamDto`, `MetaDto`, `SubtitleDto`
- One DTO per remote response shape; do not reuse DTOs across incompatible endpoints.

## Settings keys (SHOULD)

- Centralize as `private const val KEY_*` constants at the top of the file that uses them.
- Use `snake_case`: `"api_key"`, `"access_token"`, `"hd_enabled"`
