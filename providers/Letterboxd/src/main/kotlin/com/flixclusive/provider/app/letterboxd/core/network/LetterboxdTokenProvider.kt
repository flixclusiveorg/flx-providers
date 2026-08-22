package com.flixclusive.provider.app.letterboxd.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.provider.app.letterboxd.core.config.LetterboxdConfig
import com.flixclusive.provider.app.letterboxd.core.config.PrefsKey
import com.flixclusive.provider.app.letterboxd.core.model.AuthToken
import com.flixclusive.provider.app.letterboxd.core.model.Member
import com.flixclusive.provider.extensions.getObject
import com.flixclusive.provider.extensions.remove
import com.flixclusive.provider.extensions.setObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds two independent tokens:
 *
 * - the **app token** (Client Credentials), which powers public reads so the provider
 *   is useful before anyone signs in;
 * - the **member token** (Authorization Code), needed for `/me` and every write.
 */
internal class LetterboxdTokenProvider(
    private val settings: DataStore<Preferences>,
    private val authService: LetterboxdAuthService,
) {
    private val memberMutex = Mutex()
    private val appMutex = Mutex()

    suspend fun storedMemberToken(): AuthToken? = settings.getObject<AuthToken>(PrefsKey.MEMBER_AUTH)

    suspend fun isMemberAuthenticated(): Boolean = runCatching { storedMemberToken() != null }.getOrDefault(false)

    suspend fun storeMemberToken(token: AuthToken) = settings.setObject(PrefsKey.MEMBER_AUTH, token)

    suspend fun storeProfile(member: Member) = settings.setObject(PrefsKey.MEMBER, member)

    suspend fun profile(): Member? = settings.getObject<Member>(PrefsKey.MEMBER)

    suspend fun signOut() {
        settings.remove(PrefsKey.MEMBER_AUTH)
        settings.remove(PrefsKey.MEMBER)
    }

    suspend fun memberBearer(): String? =
        memberMutex.withLock {
            val token = storedMemberToken() ?: return null
            val fresh = if (token.needsRefresh) refreshLocked(token) else token
            fresh?.bearer
        }

    /** Member token when signed in, app token otherwise. Null only when neither is obtainable. */
    suspend fun bearer(): String? = memberBearer() ?: appBearer()

    private suspend fun appBearer(): String? =
        appMutex.withLock {
            if (!LetterboxdConfig.hasCredentials) {
                errorLog("Letterboxd: no client credentials were compiled in.")
                return null
            }

            val cached = settings.getObject<AuthToken>(PrefsKey.APP_AUTH)
            if (cached != null && !cached.needsRefresh) return cached.bearer

            // Client Credentials issues no refresh token — just ask for a new one.
            return runCatching { authService.clientCredentials() }
                .onSuccess { settings.setObject(PrefsKey.APP_AUTH, it) }
                .onFailure { errorLog("Letterboxd: could not obtain an app token — ${it.message}") }
                .getOrNull()
                ?.bearer
        }

    private suspend fun refreshLocked(token: AuthToken): AuthToken? {
        if (token.refreshToken.isBlank()) {
            if (token.isExpired) signOut()
            return token.takeIf { !it.isExpired }
        }

        return runCatching { authService.refresh(token.refreshToken) }
            .onSuccess { settings.setObject(PrefsKey.MEMBER_AUTH, it) }
            .onFailure {
                errorLog("Letterboxd: member token refresh failed — ${it.message}")
                signOut()
            }.getOrNull()
    }
}
