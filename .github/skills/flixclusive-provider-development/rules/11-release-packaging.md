# 11 — Release & Packaging

## Provider ID stability (MUST)

- The `id` field in `flxProvider { ... }` MUST remain stable across all releases.
- **Changing `id` after the first release is a breaking change** — it disconnects existing installs from future updates.
- Use lowercase + hyphen format, prefixed with `prov-`: `prov-my-provider`

## Versioning (MUST)

Version fields live in `flxProvider { ... }` in the provider module's `build.gradle.kts`:

- `versionBuild` — increment on every build (CI typically manages this).
- `versionPatch` — bugfix / minor behavior correction.
- `versionMinor` — new feature or capability.
- `versionMajor` — breaking behavior change or major rewrite.

## Status (MUST)

Set `status` accurately — it is shown to users:

| Value | When to use |
|---|---|
| `ProviderStatus.Working` | Provider is fully functional |
| `ProviderStatus.Beta` | Working but not fully validated |
| `ProviderStatus.Maintenance` | Temporarily degraded or being updated |
| `ProviderStatus.Down` | Source is unreachable or broken |

## Changelog (SHOULD)

Keep `changelog` updated with user-facing bullet points:

```kotlin
flxProvider {
    changelog = """
    # 1.2.0
    ---
    - Added subtitle support
    - Fixed catalog pagination
    """.trimIndent()
}
```

## Metadata checklist before shipping (MUST)

Confirm the following `flxProvider { ... }` fields are correct:

- `id` — stable and unique
- `providerName` — human-readable Title Case
- `description` — short summary
- `language`, `providerType`, `adult` — accurately reflect content
- `status` — reflects current availability
- `requiresResources` — `true` only when `res/` files are actually included
- `excludeFromUpdaterJson` — only use to hide intentionally unfinished providers

## Build and deploy commands

```bash
# Package the .flx artifact
./gradlew :MyProvider:make

# Deploy to device/emulator
./gradlew :MyProvider:deployWithAdb

# Deploy using debug build of Flixclusive
./gradlew :MyProvider:deployWithAdb --debug-app
```
