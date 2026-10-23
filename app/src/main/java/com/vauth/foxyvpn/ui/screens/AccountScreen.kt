package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.GUARDIAN_ENDPOINT_DEFAULT
import com.vauth.foxyvpn.data.GuardianClient
import com.vauth.foxyvpn.data.TokenStore
import com.vauth.foxyvpn.data.formatBytes
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.AccountLiveStats
import com.vauth.foxyvpn.data.model.BatchImportItemResult
import com.vauth.foxyvpn.data.model.VpnAccount
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountScreen(
    authRepository: FxaAuthRepository,
    tokenStore: TokenStore,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val accounts by tokenStore.accountsFlow.collectAsState()
    val liveStats by FoxyVpnService.multiAccountStats.collectAsState()
    val statsById = remember(liveStats) { liveStats.associateBy { it.accountId } }

    var showImportSheet by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var refreshingQuotas by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }

    fun refreshQuotas() {
        refreshingQuotas = true
        banner = null
        scope.launch {
            var ok = 0
            for (account in tokenStore.getAccounts()) {
                val renewed = runCatching {
                    authRepository.ensureFreshAccessTokenForAccount(account, force = false)
                }.getOrNull() ?: account.auth
                val entitlement = runCatching {
                    GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, renewed.accessToken)
                }.getOrNull()
                if (entitlement != null) {
                    tokenStore.updateAccount(
                        account.copy(
                            auth = renewed,
                            quotaRemaining = entitlement.quotaRemaining,
                            uid = entitlement.uid.ifBlank { null },
                            lastError = null,
                        ),
                    )
                    ok++
                } else {
                    tokenStore.updateAccount(account.copy(lastError = "Quota refresh failed"))
                }
            }
            banner = "Updated $ok of ${tokenStore.getAccounts().size} account(s)."
            refreshingQuotas = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshQuotas() }, enabled = !refreshingQuotas) {
                        if (refreshingQuotas) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh quotas")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .widthIn(max = 560.dp),
        ) {
            banner?.let {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showImportSheet = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Batch import")
                }
                FilledTonalButton(
                    onClick = { showLoginDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add account")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (accounts.isEmpty()) {
                EmptyAccountsHint()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            stats = statsById[account.id],
                            onToggle = { tokenStore.toggleAccount(account.id, !account.isEnabled) },
                            onDelete = { tokenStore.removeAccount(account.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showImportSheet) {
        BatchImportBottomSheet(
            authRepository = authRepository,
            onDismiss = { showImportSheet = false },
        )
    }
    if (showLoginDialog) {
        SingleAccountLoginDialog(
            authRepository = authRepository,
            onDismiss = { showLoginDialog = false },
        )
    }
}

@Composable
private fun EmptyAccountsHint() {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = cs.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No accounts yet",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add one account or paste a whole list — each gets its own VPN session and they share the load.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(
    account: VpnAccount,
    stats: AccountLiveStats?,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val dotTarget = when {
        !account.isEnabled -> cs.outlineVariant
        stats?.isExhausted == true -> cs.error
        stats?.isConnected == true -> cs.primary
        else -> cs.outline
    }
    val dot by animateColorAsState(dotTarget, tween(300), label = "card-dot")

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    account.email.ifBlank { "Account" },
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(checked = account.isEnabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove account",
                        tint = cs.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Quota bar.
            val remaining = account.quotaRemaining
            if (remaining != null) {
                Spacer(Modifier.height(8.dp))
                val fraction = (remaining.toFloat() / account.quotaMax.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatBytes(remaining)} left of ${formatBytes(account.quotaMax)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }

            // Live per-account stats.
            if (stats != null && account.isEnabled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        stats.isExhausted -> "Quota exhausted"
                        !stats.isConnected -> "Not connected"
                        else -> "↓ ${formatBytesPerSecond(stats.speedDownBytesPerSec)} · " +
                            "↑ ${formatBytesPerSecond(stats.speedUpBytesPerSec)} · " +
                            "${stats.activeStreams} streams"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            account.lastError?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = cs.error, maxLines = 2)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchImportBottomSheet(
    authRepository: FxaAuthRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BatchImportItemResult>>(emptyList()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Batch import", style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                "Paste one entry per line (email:password) or a JSON array of " +
                    "{email, password} / {email, accessToken, refreshToken} objects.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("name@example.com:password") },
            )
            if (progress.isNotBlank()) {
                Text(progress, style = MaterialTheme.typography.labelMedium)
            }
            if (results.isNotEmpty()) {
                val okCount = results.count { it.success }
                Text(
                    "Imported $okCount of ${results.size}",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                results.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (r.success) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (r.success) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            r.identifier,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!r.success) {
                            Text(
                                r.error.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    importing = true
                    results = emptyList()
                    scope.launch {
                        val out = authRepository.batchImportAccounts(input) { current, total, label ->
                            progress = "$current/$total · $label"
                        }
                        results = out
                        progress = ""
                        importing = false
                    }
                },
                enabled = !importing && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (importing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (importing) "Importing…" else "Import")
            }
        }
    }
}

@Composable
private fun SingleAccountLoginDialog(
    authRepository: FxaAuthRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add account", style = MaterialTheme.typography.titleLargeEmphasized)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                authRepository.loginSingleAccount(email.trim(), password)
                                    .onSuccess { onDismiss() }
                                    .onFailure { error = it.message ?: "Sign-in failed" }
                                busy = false
                            }
                        },
                        enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Sign in")
                    }
                }
            }
        }
    }
}
