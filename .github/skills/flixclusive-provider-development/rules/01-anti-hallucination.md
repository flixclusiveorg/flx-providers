# 01 — Anti-hallucination (Read First)

These rules override all other rules.

## Non‑negotiables

- Read existing code before writing new code.
  - Use the example provider for template-specific patterns: `providers/BasicDummyProvider/`.
  - Use Provider Docs pages as the canonical implementation guides (capabilities, settings UI, WebView, etc.).
- Do not invent SDK APIs.
  - Confirm capability interfaces and models from the `core-stubs` dependency or official docs.
- Do not invent Gradle tasks or plugin behavior.
  - Confirm from existing Gradle files in this repo, or by running `./gradlew tasks`.
- If key requirements are missing, ask 1–3 targeted questions before implementing.

## Source of truth

Use this order:

1. `core-stubs` API docs (exact signatures): [`core-stubs` docs](https://flixclusiveorg.github.io/core-stubs/)
2. Provider Docs (recommended patterns + examples): [provider-docs](https://flixclusiveorg.github.io/provider-docs/)
3. This repository’s code (template conventions, Gradle wiring, example provider).
4. Source fallback when local sources/indices are missing: [flixclusiveorg/core-stubs](https://github.com/flixclusiveorg/core-stubs)

## Scope discipline

- Make the smallest change that satisfies the request.
- Don’t refactor unrelated code.
- Don’t change CI/workflows, publishing, or repository settings unless explicitly requested.

## Validation expectation

When changes affect build logic or Kotlin compilation, prefer the narrowest check that proves correctness:

- `./gradlew :<ProviderModule>:compileDebugKotlin`
- `./gradlew :<ProviderModule>:make`

If execution is not possible, ensure changes are consistent with existing patterns and compile signatures.
