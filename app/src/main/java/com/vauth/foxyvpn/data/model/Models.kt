package com.vauth.foxyvpn.data.model

import java.util.UUID
import kotlinx.serialization.Serializable

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

data class VpnProtocol(
    val name: String,
    val host: String = "",
    val port: Int = 0,
    val scheme: String = "",
    val templateString: String = "",
)

data class VpnServerNode(
    val hostname: String,
    val port: Int = 0,
    val quarantined: Boolean = false,
    val protocols: List<VpnProtocol> = emptyList(),
)

data class VpnCity(
    val name: String,
    val code: String,
    val servers: List<VpnServerNode> = emptyList(),
)

data class VpnCountry(
    val name: String,
    val code: String,
    val cities: List<VpnCity> = emptyList(),
)

data class ProxyCandidate(
    val host: String,
    val port: Int,
    val countryCode: String,
    val countryName: String,
    val cityCode: String = "",
) {
    val authority: String get() = "$host:$port"
}

data class Entitlement(
    val subscribed: Boolean,
    val uid: String,
    val maxBytes: Long?,
    val limitedBandwidth: Boolean,
    val quotaRemaining: Long? = null,
)

@Serializable
data class RuntimeAuth(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long,
)

/**
 * One Mozilla/FxA account held by the multi-account patch.
 *
 * @param quotaMax default 50 GiB, the Mozilla VPN monthly allowance per account.
 */
@Serializable
data class VpnAccount(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val auth: RuntimeAuth,
    val isEnabled: Boolean = true,
    val addedAtEpochSeconds: Long = System.currentTimeMillis() / 1000,
    val quotaMax: Long = DEFAULT_ACCOUNT_QUOTA_BYTES,
    val quotaRemaining: Long? = null,
    val uid: String? = null,
    val lastError: String? = null,
)

@Serializable
data class BatchImportItemResult(
    val identifier: String,
    val success: Boolean,
    val error: String? = null,
    val account: VpnAccount? = null,
)

@Serializable
data class AccountLiveStats(
    val accountId: String,
    val email: String,
    val isConnected: Boolean = false,
    val isExhausted: Boolean = false,
    val activeStreams: Int = 0,
    val bytesDown: Long = 0L,
    val bytesUp: Long = 0L,
    val speedDownBytesPerSec: Long = 0L,
    val speedUpBytesPerSec: Long = 0L,
)

const val DEFAULT_ACCOUNT_QUOTA_BYTES: Long = 50L * 1024L * 1024L * 1024L

fun countryCodeToEmojiFlag(countryCode: String): String {
    if (countryCode.equals("REC", ignoreCase = true) || countryCode.length != 2) return ""
    val upper = countryCode.uppercase()
    val first = upper[0]
    val second = upper[1]
    if (first !in 'A'..'Z' || second !in 'A'..'Z') return ""

    val firstCodePoint = 0x1F1E6 + (first - 'A')
    val secondCodePoint = 0x1F1E6 + (second - 'A')

    return String(Character.toChars(firstCodePoint)) + String(Character.toChars(secondCodePoint))
}

val VpnCountry.emojiFlag: String get() = countryCodeToEmojiFlag(code)
val ProxyCandidate.emojiFlag: String get() = countryCodeToEmojiFlag(countryCode)

