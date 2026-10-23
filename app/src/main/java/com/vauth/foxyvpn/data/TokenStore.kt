package com.vauth.foxyvpn.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vauth.foxyvpn.data.model.RuntimeAuth
import com.vauth.foxyvpn.data.model.VpnAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val TAG = "TokenStore"

private val accountsJson = Json { ignoreUnknownKeys = true }

class TokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "foxyvpn_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    // Cumulative per-account byte usage. Plain prefs (not sensitive), keyed
    // by account id. Accumulated from the pool's live stats so the quota bar
    // survives reconnects; reset when the server reports a fresh quota.
    private val usagePrefs = context.getSharedPreferences("foxyvpn_usage", Context.MODE_PRIVATE)
    private val _usageFlow = MutableStateFlow(loadUsage())
    val usageFlow: StateFlow<Map<String, Long>> = _usageFlow

    fun getUsedBytes(accountId: String): Long = _usageFlow.value[accountId] ?: 0L

    fun addUsedBytes(accountId: String, deltaBytes: Long) {
        if (deltaBytes <= 0L) return
        val next = (_usageFlow.value[accountId] ?: 0L) + deltaBytes
        usagePrefs.edit().putLong(usageKey(accountId), next).apply()
        _usageFlow.value = _usageFlow.value + (accountId to next)
    }

    fun resetUsedBytes(accountId: String) {
        usagePrefs.edit().remove(usageKey(accountId)).apply()
        _usageFlow.value = _usageFlow.value - accountId
    }

    private fun usageKey(accountId: String) = "used_$accountId"

    private fun loadUsage(): Map<String, Long> =
        usagePrefs.all.mapNotNull { (k, v) ->
            if (k.startsWith("used_") && v is Long) k.removePrefix("used_") to v else null
        }.toMap()

    fun saveAuth(auth: RuntimeAuth) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, auth.accessToken)
            .putString(KEY_REFRESH_TOKEN, auth.refreshToken)
            .putLong(KEY_EXPIRES_AT, auth.expiresAtEpochSeconds)
            .apply()
    }

    fun loadAuth(): RuntimeAuth? = runCatching {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return@runCatching null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        RuntimeAuth(access, refresh, expiresAt)
    }.onFailure {
        AppLogger.w(TAG, "could not read the stored session", it)
    }.getOrNull()

    fun hasValidAccessToken(): Boolean {
        val auth = loadAuth() ?: return false
        if (auth.expiresAtEpochSeconds <= 0L) return false
        val nowSeconds = System.currentTimeMillis() / 1000
        return auth.expiresAtEpochSeconds - nowSeconds > CLOCK_SKEW_TOLERANCE_SECONDS
    }

    fun hasStoredSession(): Boolean = loadAuth() != null

    fun hasRefreshToken(): Boolean = loadAuth()?.refreshToken != null

    fun clear() {
        prefs.edit().clear().apply()
        usagePrefs.edit().clear().apply()
        _usageFlow.value = emptyMap()
        _accountsFlow.value = emptyList()
    }

    /**
     * Clears ONLY the legacy single-session keys. The multi-account list
     * (accounts_json_v1) and usage stats are NEVER touched here.
     *
     * Background: restoreSession() used to call clear() when the legacy
     * session died, which wiped every saved account on every cold start —
     * the "close the app, lose everything" bug.
     */
    fun clearLegacySession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    // ------------------------------------------------------------------
    // Multi-account storage. Accounts live in the same encrypted prefs
    // ("foxyvpn_tokens") as the legacy single session, serialized as JSON.
    // ------------------------------------------------------------------

    private val _accountsFlow = MutableStateFlow(loadAccounts())
    val accountsFlow: StateFlow<List<VpnAccount>> = _accountsFlow

    fun getAccounts(): List<VpnAccount> = _accountsFlow.value

    fun getEnabledAccounts(): List<VpnAccount> = _accountsFlow.value.filter { it.isEnabled }

    fun hasAnyAccount(): Boolean = _accountsFlow.value.isNotEmpty()

    fun saveAccounts(accounts: List<VpnAccount>) {
        val encoded = runCatching {
            accountsJson.encodeToString(ListSerializer(VpnAccount.serializer()), accounts)
        }.getOrElse {
            AppLogger.w(TAG, "could not encode the account list", it)
            return
        }
        prefs.edit().putString(KEY_ACCOUNTS_JSON, encoded).apply()
        _accountsFlow.value = accounts
    }

    fun addAccount(account: VpnAccount) {
        val current = _accountsFlow.value.toMutableList()
        current.removeAll { it.id == account.id || it.email.equals(account.email, ignoreCase = true) }
        current.add(account)
        saveAccounts(current)
    }

    fun addAccounts(accounts: List<VpnAccount>) {
        if (accounts.isEmpty()) return
        val current = _accountsFlow.value.toMutableList()
        for (account in accounts) {
            current.removeAll { it.id == account.id || it.email.equals(account.email, ignoreCase = true) }
            current.add(account)
        }
        saveAccounts(current)
    }

    fun removeAccount(id: String) {
        saveAccounts(_accountsFlow.value.filterNot { it.id == id })
        resetUsedBytes(id)
    }

    fun toggleAccount(id: String, enabled: Boolean) {
        saveAccounts(_accountsFlow.value.map { if (it.id == id) it.copy(isEnabled = enabled) else it })
    }

    fun updateAccount(account: VpnAccount) {
        saveAccounts(_accountsFlow.value.map { if (it.id == account.id) account else it })
    }

    private fun loadAccounts(): List<VpnAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return emptyList()
        return runCatching {
            accountsJson.decodeFromString(ListSerializer(VpnAccount.serializer()), raw)
        }.onFailure {
            AppLogger.w(TAG, "could not decode the stored account list", it)
        }.getOrDefault(emptyList())
    }

    /**
     * First-run migration: wraps the legacy single session (if any) into the
     * account list as "Primary Account" so existing installs keep working.
     */
    fun migrateLegacySessionIfPresent(): VpnAccount? {
        if (hasAnyAccount()) return null
        val legacy = loadAuth() ?: return null
        val primary = VpnAccount(
            email = "Primary Account",
            auth = legacy,
        )
        saveAccounts(listOf(primary))
        return primary
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNTS_JSON = "accounts_json_v1"

        private const val CLOCK_SKEW_TOLERANCE_SECONDS = 60L
    }
}
