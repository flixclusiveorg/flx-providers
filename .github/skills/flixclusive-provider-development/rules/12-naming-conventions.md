# 12 — Naming Conventions

Follow existing patterns in `providers/BasicDummyProvider`.

## Modules

- Provider modules live under `providers/` and use PascalCase names.
  - Example: `BasicDummyProvider`, `MyNewProvider`

## Packages

- Use a stable, reverse-DNS package.
- Keep provider-specific code under a provider package, e.g.:
  - `com.yourorg.provider.my_new_provider`

## Entry class

- The provider entry class should be the module’s main symbol and extend `ProviderPlugin`.
  - Example: `MyNewProvider : ProviderPlugin()`

## Capability APIs

- Name capability implementations clearly:
  - `MyNewProviderSearchProviderApi`
  - `MyNewProviderCatalogProviderApi`
  - `MyNewProviderMetadataProviderApi`
  - `MyNewProviderMediaLinkProviderApi`

## Provider ID (`flxProvider.id`)

- Lowercase + hyphen-separated.
- Stable and unique.
- Avoid spaces and uppercase.
- Common convention: prefix with `prov-`.
- Example: `prov-my-source`

## Provider display name (`flxProvider.providerName`)

- Human-readable Title Case.
- Keep it short; avoid stuffing version/region into the display name.
