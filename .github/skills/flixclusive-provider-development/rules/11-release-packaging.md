# 11 — Release & Packaging

## Versioning

- Version fields live in `flxProvider { ... }` inside the provider module’s `build.gradle.kts`.
- Keep provider `id` stable across all releases; changing it breaks updates and identity.

Suggested practice:

- Bugfix: bump `versionPatch`
- Feature: bump `versionMinor`
- Breaking behavior change: bump `versionMajor`

## Changelog

- Keep `changelog` updated (it supports Markdown).
- Use clear, user-facing bullet points.

## Metadata checklist

Before shipping, confirm these `flxProvider { ... }` fields are correct:

- Identity: `id` (stable), `providerName`, `description`
- Content spec: `language`, `providerType`, `adult`
- Build: `requiresResources` (set true only when you actually ship resources)
- Visibility: `excludeFromUpdaterJson` only when intentionally hiding unfinished providers

## Build

- `./gradlew :<ProviderModule>:make` produces the distributable artifact.

## Deployment

- `./gradlew :<ProviderModule>:deployWithAdb` builds and deploys to a device/emulator.

## Status

- Set `status` accurately (`Working`, `Down`, `Maintenance`, `Beta`).
- Use `excludeFromUpdaterJson` only when intentionally hiding unfinished providers.
