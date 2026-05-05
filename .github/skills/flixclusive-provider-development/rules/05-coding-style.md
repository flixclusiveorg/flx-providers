# 05 — Coding Style (Kotlin)

Follow the style already used by the template provider.

## Kotlin formatting

- 4-space indentation.
- Use trailing commas in multiline argument/parameter lists.
- Prefer named arguments when passing multiple values.
- Prefer `val` over `var`.

## Structure

- Keep provider entry class small; put logic in capability APIs and helpers.
- Prefer one responsibility per file/class.
- Extract parsing and mapping into dedicated functions.

## Organize by feature (not by type)

Group code by the user-facing feature so related code is easy to find:

```
com.example.my_provider/
├── api/
│   ├── search/
│   ├── catalog/
│   ├── metadata/
│   └── links/
├── settings/
├── common/
└── MyProviderPlugin.kt
```

## Keep methods short and focused

- Prefer small functions that do one job (fetch, parse, map, emit).
- Avoid mixing network I/O, parsing, and mapping in one giant method body.

## Member order

When ordering members inside a class, keep them grouped in this order:

1. `companion object`
2. `private val`
3. `private var`
4. `val`
5. `lateinit var`
6. `var`
7. overridden methods
8. `private fun`
9. `fun`

Within each group, sort members alphabetically when practical.

## Coroutines

- Don’t block threads in `suspend` functions.
  - Use `withContext(FlxDispatchers.IO.dispatcher)` for blocking I/O or parsing.
- Respect cancellation (don’t swallow `CancellationException`).

## Error handling

- Fail fast with meaningful exceptions when extraction is impossible.
- Avoid returning silently-invalid data (empty URLs, missing IDs, etc.).
- Don’t log secrets (tokens, cookies, user identifiers).

## Data models

- Prefer using `core-stubs` models (e.g., `FilmSearchItem`, `FilmMetadata`, `MediaLink`) instead of duplicating DTOs.
- Use Kotlinx Serialization when interacting with JSON APIs if the SDK expects serializable types.

## Readability pitfalls to avoid

- Avoid premature abstraction (interfaces with a single implementation).
- Avoid overly complex expressions; split into intermediate `val`s when it improves clarity.
- Avoid excessive comments; prefer self-explanatory names. Use comments mainly to explain *why*.
- Avoid overusing the Elvis operator (`?:`) when an explicit `if` improves readability.
