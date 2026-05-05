# 08 — Settings & Auth Rules

Providers should store user configuration and auth state using `ProviderSettings` (`provider.settings`).

Important runtime note:

- `ProviderPlugin.manifest` and `ProviderPlugin.settings` are assigned by the host app at runtime.
- Avoid accessing them in `init {}` blocks or property initializers unless you use `by lazy { ... }`.

## Persistence

- Store tokens, usernames, feature toggles, and preferences in settings.
- Keep settings keys as constants in one place.
- Prefer typed APIs (`getString`, `setString`, `getBool`, `setBool`, etc.).

## Supported settings types

- `ProviderSettings` supports primitives and Kotlinx-Serializable objects:
  - Primitives: `Bool`, `Int`, `Long`, `Float`, `String`
  - Objects: `setObject(key, value)` / `getObject<T>(key)` (T must be `@Serializable`)

Useful helpers:

- `exists(key)`, `remove(key)`, `toggleBool(key, default)`, `resetSettings()`, `allKeys`
- `getUnknown(key, default)`, `setUnknown(key, value)`

## Security basics

- Don’t store raw passwords.
- Don’t print/log tokens, cookies, or session identifiers.
- Prefer short-lived tokens + refresh tokens if the upstream service supports it.

## SettingsScreen (Compose)

- Provide Settings UI only when needed.
- Keep UI code separate from extraction logic.
- Settings UI should:
  - show auth status
  - allow login/logout
  - allow clearing cached state if relevant

## Tracker auth behavior

When implementing `TrackerProviderApi`:

- Back `isAuthenticated()` by your persisted auth state (token/cookie + expiry when applicable).
- Return `false` when logged out or expired; the host will route the user to `SettingsScreen` for login.
- Use `SettingsScreen` to complete login and persist tokens, and clear persisted auth state on logout so `isAuthenticated()` stays accurate.

## Concrete example (Compose SettingsScreen + ProviderSettings)

Adapted from the Provider Docs “Creating a Settings UI” guide.

```kotlin
private const val KEY_HD_ENABLED = "hd_enabled"
private const val KEY_API_KEY = "api_key"

@Composable
override fun SettingsScreen() {
  var isHdEnabled by remember {
    mutableStateOf(settings.getBool(KEY_HD_ENABLED, false))
  }

  var apiKey by remember {
    mutableStateOf(settings.getString(KEY_API_KEY, "") ?: "")
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Enable HD quality",
        modifier = Modifier.weight(1f),
      )

      Switch(
        checked = isHdEnabled,
        onCheckedChange = { newValue ->
          isHdEnabled = newValue
          settings.setBool(KEY_HD_ENABLED, newValue)
        },
      )
    }

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
      value = apiKey,
      onValueChange = { newValue ->
        apiKey = newValue
        settings.setString(KEY_API_KEY, newValue)
      },
      label = { Text("API key") },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
```

### Concrete example (persisting a custom object)

```kotlin
@Serializable
data class ServerConfig(
  val url: String,
  val port: Int,
)

settings.setObject("server_config", ServerConfig(url = "localhost", port = 8080))
val config = settings.getObject<ServerConfig>("server_config")
```
