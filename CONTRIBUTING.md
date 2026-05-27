# Contributing to Flixclusive Providers

Thank you for your interest in contributing! This document covers everything you need to set up a development environment, build providers, and submit a pull request.

---

## Prerequisites

- **JDK 17** or higher
- **Android SDK** (API level 23–36)
- A physical Android device or emulator for deployment testing
- `adb` available in your `PATH`

---

## Getting Started

1. Fork the repository and clone your fork:

   ```bash
   git clone https://github.com/<your-handle>/flx-providers.git
   cd flx-providers
   ```

2. If a provider requires API credentials (e.g., Trakt), add them to `local.properties`:

   ```properties
   TRAKT_CLIENT_ID=your_client_id
   TRAKT_CLIENT_SECRET=your_client_secret
   ```

   > **Never commit `local.properties`.** It is already in `.gitignore`.

3. Open the project in Android Studio or build entirely from the command line.

---

## Build & Deploy

```bash
# Build and package a single provider (.flx artifact)
./gradlew :ProviderName:make

# Deploy to a connected device or emulator
./gradlew :ProviderName:deployWithAdb

# Deploy using the debug build of Flixclusive
./gradlew :ProviderName:deployWithAdb --debug-app

# Deploy and wait for a debugger to attach
./gradlew :ProviderName:deployWithAdb --wait-for-debugger
```

Built `.flx` artifacts are placed in `providers/ProviderName/build/`.

After deployment the provider appears in Flixclusive's provider list. Use the **"Test provider"** option in the app to run an automated capability check.

---

## Adding a New Provider

1. Create a new module directory under `providers/`:

   ```
   providers/MyProvider/
   ├── build.gradle.kts
   └── src/main/kotlin/
   ```

2. Register the module in `settings.gradle.kts`:

   ```kotlin
   include(
       "util",
       "Trakt",
       "Stremio",
       "MyProvider",  // <- add this
   )
   ```

3. Configure `providers/MyProvider/build.gradle.kts` with a stable `id`, version, and the capabilities your source supports. Use the existing providers as a reference.

4. Implement only the capability interfaces your source supports. Return `null` from capability getters that are not applicable. Refer to the [Provider Documentation](https://flixclusiveorg.github.io/provider-docs/) and the [Core Stubs API Reference](https://flixclusiveorg.github.io/core-stubs/) for the full API surface.

---

## Pull Request Guidelines

- **One PR per provider or logical change.** Keep changes focused and reviewable.
- **Branch naming:** `feature/provider-name` or `fix/short-description`.
- **Commit messages:** follow the [Conventional Commits](https://www.conventionalcommits.org/) format — e.g., `feat(trakt): add cross-match support`, `fix(stremio): handle null catalog response`.
- Ensure the provider builds cleanly (`./gradlew :ProviderName:make`) before opening a PR.
- Add or update tests where applicable.

---

## Code Conventions

- Follow the existing Kotlin style in the codebase (4-space indentation, no wildcard imports).
- Expose only the capability APIs your provider actually supports; return `null` from unsupported capability getters.
- Cache capability API instances with `by lazy` to avoid repeated instantiation.
- Keep network logic separate from provider entry classes — follow the feature-package structure used by existing providers.
- Do not commit `local.properties`, secrets, or generated build artifacts.

---

## Resources

- [Provider Documentation](https://flixclusiveorg.github.io/provider-docs/)
- [Core Stubs API Reference](https://flixclusiveorg.github.io/core-stubs/)
- [Discord Community](https://discord.gg/7yPSPveReu)
