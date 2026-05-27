# 08 — Settings & Auth Rules

## `settings` is a `DataStore<Preferences>` (MUST)

- `ProviderPlugin.manifest` and `ProviderPlugin.settings` are assigned by the host app at runtime.
- **MUST NOT** access them in `init {}` blocks or property initializers.
- **MUST** access them inside `by lazy { ... }`, capability getters, or `SettingsScreen()`.

```kotlin
// WRONG — settings not yet initialized
val apiKey = settings.getString("api_key", null)  // ❌

// CORRECT — lazy; only evaluated after host sets settings
private val searchApi by lazy {
    MySearchApi(apiKey = settings.getString("api_key", null) ?: "")  // ✅
}
```

## Persisting configuration (MUST)

- Store tokens, usernames, feature toggles, and preferences in `settings`.
- Centralize preference keys as `private const val` constants.
- Use Core Stubs typed DataStore extension helpers.

### Supported types

| Type | Helpers |
|---|---|
| Boolean | `settings.getBool(key, default)` / `settings.setBool(key, value)` |
| Int | `settings.getInt(key, default)` / `settings.setInt(key, value)` |
| Long | `settings.getLong(key, default)` / `settings.setLong(key, value)` |
| Float | `settings.getFloat(key, default)` / `settings.setFloat(key, value)` |
| String | `settings.getString(key, default)` / `settings.setString(key, value)` |
| Object | `settings.getObject<T>(key)` / `settings.setObject(key, value)` (T must be `@Serializable`) |
| Utility | `settings.exists(key)`, `settings.remove(key)`, `settings.resetSettings()` |

## Security (MUST)

- Do **not** store raw passwords.
- Do **not** log tokens, cookies, or session identifiers.
- Prefer short-lived tokens with refresh tokens where the upstream service supports it.

## SettingsScreen (SHOULD)

- Provide `SettingsScreen()` only when the provider requires user configuration or auth.
- Extract all composable logic into separate files/functions for readability.
- The settings screen should: show auth status, allow login/logout, allow clearing cache if relevant.

### Concrete example — basic settings (Material 3)

```kotlin
private const val KEY_HD_ENABLED = "hd_enabled"
private const val KEY_API_KEY    = "api_key"

@Composable
override fun SettingsScreen() {
    MyProviderSettings(settings = settings)
}

@Composable
private fun MyProviderSettings(settings: DataStore<Preferences>) {
    var isHdEnabled by remember { mutableStateOf(settings.getBool(KEY_HD_ENABLED, false)) }
    var apiKey by remember { mutableStateOf(settings.getString(KEY_API_KEY, null) ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Enable HD quality", modifier = Modifier.weight(1f))
            Switch(
                checked = isHdEnabled,
                onCheckedChange = { v -> isHdEnabled = v; settings.setBool(KEY_HD_ENABLED, v) },
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value    = apiKey,
            onValueChange = { v -> apiKey = v; settings.setString(KEY_API_KEY, v) },
            label    = { Text("API key") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

### Concrete example — OAuth token storage (adapted from `providers/Trakt/`)

```kotlin
private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_TOKEN_EXPIRY  = "token_expiry_ms"

// Store token after OAuth exchange
settings.setString(KEY_ACCESS_TOKEN, authToken.accessToken)
settings.setString(KEY_REFRESH_TOKEN, authToken.refreshToken)
settings.setLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + authToken.expiresInMs)

// Check authentication
fun isAuthenticated(): Boolean {
    val token  = settings.getString(KEY_ACCESS_TOKEN, null) ?: return false
    val expiry = settings.getLong(KEY_TOKEN_EXPIRY, 0L)
    return token.isNotBlank() && System.currentTimeMillis() < expiry
}

// Clear auth state on logout
fun logout() {
    settings.remove(KEY_ACCESS_TOKEN)
    settings.remove(KEY_REFRESH_TOKEN)
    settings.remove(KEY_TOKEN_EXPIRY)
}
```

## Tracker auth behavior (MUST)

- Back `isAuthenticated()` by the persisted token/expiry state — keep it fast and side-effect-free.
- Return `false` when logged out or the token is expired; the host routes the user to `SettingsScreen`.
- Clear all persisted auth state on logout so `isAuthenticated()` stays accurate.
