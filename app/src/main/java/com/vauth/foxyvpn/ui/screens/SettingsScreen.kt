package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.ui.l10n.s

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.BuildConfig
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.ui.theme.AppearanceController

private const val GITHUB_URL = "https://github.com/vauth/foxyvpn"

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    appearance: AppearanceController,
    onOpenLogs: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var exitCheckEnabled by remember { mutableStateOf(settingsStore.exitCheckEnabled) }
    var dnsProvider by remember { mutableStateOf(settingsStore.dnsProvider) }
    var customDohUrl by remember { mutableStateOf(settingsStore.customDohUrl) }
    var bootstrapDnsProvider by remember { mutableStateOf(settingsStore.bootstrapDnsProvider) }
    var customBootstrapIp by remember { mutableStateOf(settingsStore.customBootstrapIp) }
    var socksBindAddress by remember { mutableStateOf(settingsStore.socksBindAddress) }
    var socksPort by remember { mutableStateOf(settingsStore.socksPort) }
    var httpProxyEnabled by remember { mutableStateOf(settingsStore.httpProxyEnabled) }
    var httpProxyPort by remember { mutableStateOf(settingsStore.httpProxyPort) }
    var proxyOnlyMode by remember { mutableStateOf(settingsStore.proxyOnlyMode) }
    var customEdgeAddress by remember { mutableStateOf(settingsStore.customEdgeAddress) }
    var upstreamProxyEnabled by remember { mutableStateOf(settingsStore.upstreamProxyEnabled) }
    var upstreamProxyType by remember { mutableStateOf(settingsStore.upstreamProxyType) }
    var upstreamProxyHost by remember { mutableStateOf(settingsStore.upstreamProxyHost) }
    var upstreamProxyPort by remember { mutableStateOf(settingsStore.upstreamProxyPort) }
    var upstreamProxyUsername by remember { mutableStateOf(settingsStore.upstreamProxyUsername) }
    var upstreamProxyPassword by remember { mutableStateOf(settingsStore.upstreamProxyPassword) }
    var excludedApps by remember { mutableStateOf(settingsStore.excludedApps) }
    var blockAds by remember { mutableStateOf(settingsStore.dnsProvider == SettingsStore.DnsProvider.ADGUARD) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDnsProviderDialog by remember { mutableStateOf(false) }
    var showBootstrapDnsDialog by remember { mutableStateOf(false) }
    var showSocksBindDialog by remember { mutableStateOf(false) }
    var showSocksPortDialog by remember { mutableStateOf(false) }
    var showHttpPortDialog by remember { mutableStateOf(false) }
    var showEdgeAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyTypeDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyCredentialsDialog by remember { mutableStateOf(false) }
    var showSplitTunnelDialog by remember { mutableStateOf(false) }

    if (showDnsProviderDialog) {
        DnsProviderPickerDialog(
            currentProvider = dnsProvider,
            currentCustomUrl = customDohUrl,
            onDismiss = { showDnsProviderDialog = false },
            onConfirm = { provider, url ->
                dnsProvider = provider
                settingsStore.dnsProvider = provider
                customDohUrl = url
                settingsStore.customDohUrl = url
                showDnsProviderDialog = false
            },
        )
    }
    if (showBootstrapDnsDialog) {
        BootstrapDnsPickerDialog(
            currentProvider = bootstrapDnsProvider,
            currentCustomIp = customBootstrapIp,
            onDismiss = { showBootstrapDnsDialog = false },
            onConfirm = { provider, ip ->
                bootstrapDnsProvider = provider
                settingsStore.bootstrapDnsProvider = provider
                customBootstrapIp = ip
                settingsStore.customBootstrapIp = ip
                showBootstrapDnsDialog = false
            },
        )
    }
    if (showSocksBindDialog) {
        SocksBindAddressPickerDialog(
            current = socksBindAddress,
            onDismiss = { showSocksBindDialog = false },
            onConfirm = {
                socksBindAddress = it
                settingsStore.socksBindAddress = it
                showSocksBindDialog = false
            },
        )
    }
    if (showSocksPortDialog) {
        SocksPortPickerDialog(
            current = socksPort,
            onDismiss = { showSocksPortDialog = false },
            onConfirm = {
                socksPort = it
                settingsStore.socksPort = it
                showSocksPortDialog = false
            },
        )
    }
    if (showHttpPortDialog) {
        HttpPortPickerDialog(
            current = httpProxyPort,
            onDismiss = { showHttpPortDialog = false },
            onConfirm = {
                httpProxyPort = it
                settingsStore.httpProxyPort = it
                showHttpPortDialog = false
            },
        )
    }
    if (showEdgeAddressDialog) {
        CustomEdgeAddressDialog(
            current = customEdgeAddress,
            onDismiss = { showEdgeAddressDialog = false },
            onConfirm = {
                customEdgeAddress = it
                settingsStore.customEdgeAddress = it
                showEdgeAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyTypeDialog) {
        UpstreamProxyTypePickerDialog(
            current = upstreamProxyType,
            onDismiss = { showUpstreamProxyTypeDialog = false },
            onConfirm = {
                upstreamProxyType = it
                settingsStore.upstreamProxyType = it
                showUpstreamProxyTypeDialog = false
            },
        )
    }
    if (showUpstreamProxyAddressDialog) {
        UpstreamProxyAddressDialog(
            currentHost = upstreamProxyHost,
            currentPort = upstreamProxyPort,
            onDismiss = { showUpstreamProxyAddressDialog = false },
            onConfirm = { host, port ->
                upstreamProxyHost = host
                upstreamProxyPort = port
                settingsStore.upstreamProxyHost = host
                settingsStore.upstreamProxyPort = port
                showUpstreamProxyAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyCredentialsDialog) {
        UpstreamProxyCredentialsDialog(
            currentUsername = upstreamProxyUsername,
            currentPassword = upstreamProxyPassword,
            onDismiss = { showUpstreamProxyCredentialsDialog = false },
            onConfirm = { username, password ->
                upstreamProxyUsername = username
                upstreamProxyPassword = password
                settingsStore.upstreamProxyUsername = username
                settingsStore.upstreamProxyPassword = password
                showUpstreamProxyCredentialsDialog = false
            },
        )
    }
    if (showSplitTunnelDialog) {
        SplitTunnelDialog(
            current = excludedApps,
            onDismiss = { showSplitTunnelDialog = false },
            onConfirm = {
                excludedApps = it
                settingsStore.excludedApps = it
                showSplitTunnelDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("Settings"), style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SettingsSection(title = s("Connection")) {
                SettingsSwitchRow(
                    title = "Verify exit location",
                    subtitle = s("Check that the tunnel exits in the selected country"),
                    checked = exitCheckEnabled,
                    onCheckedChange = {
                        exitCheckEnabled = it
                        settingsStore.exitCheckEnabled = it
                    },
                )
            }

            InterfaceSettingsSection(appearance = appearance)

            SettingsSection(title = s("DNS")) {
                SettingsNavRow(
                    title = s("Tunnel DNS"),
                    subtitle = when (dnsProvider) {
                        SettingsStore.DnsProvider.SYSTEM_DEFAULT -> "Default"
                        SettingsStore.DnsProvider.CUSTOM -> customDohUrl.ifBlank { "Custom DoH (Not configured)" }
                        else -> dnsProvider.label
                    },
                    onClick = { showDnsProviderDialog = true },
                )
                SettingsNavRow(
                    title = s("Resolver DNS"),
                    subtitle = when (bootstrapDnsProvider) {
                        SettingsStore.BootstrapDnsProvider.CUSTOM -> customBootstrapIp.ifBlank { "Custom IP (Not configured)" }
                        SettingsStore.BootstrapDnsProvider.SYSTEM -> "System DNS"
                        else -> bootstrapDnsProvider.label
                    },
                    onClick = { showBootstrapDnsDialog = true },
                )
                SettingsSwitchRow(
                    title = s("Block ads"),
                    subtitle = "Use AdGuard DNS inside the tunnel to block ads and trackers",
                    checked = blockAds,
                    onCheckedChange = {
                        blockAds = it
                        val provider = if (it) SettingsStore.DnsProvider.ADGUARD
                        else SettingsStore.DnsProvider.SYSTEM_DEFAULT
                        dnsProvider = provider
                        settingsStore.dnsProvider = provider
                    },
                )
            }

            SettingsSection(title = s("Local proxy")) {
                SettingsSwitchRow(
                    title = s("Proxy-only mode"),
                    subtitle = "Run local proxy servers (SOCKS5 & HTTP), without a VPN interface",
                    checked = proxyOnlyMode,
                    onCheckedChange = {
                        proxyOnlyMode = it
                        settingsStore.proxyOnlyMode = it
                    },
                )
                SettingsNavRow(
                    title = s("Local address"),
                    subtitle = socksBindAddress,
                    onClick = { showSocksBindDialog = true },
                )
                SettingsNavRow(
                    title = "SOCKS5 port",
                    subtitle = socksPort.toString(),
                    onClick = { showSocksPortDialog = true },
                )
                SettingsSwitchRow(
                    title = s("Enable HTTP proxy"),
                    subtitle = "Run local HTTP proxy alongside SOCKS5",
                    checked = httpProxyEnabled,
                    onCheckedChange = {
                        httpProxyEnabled = it
                        settingsStore.httpProxyEnabled = it
                    },
                )
                if (httpProxyEnabled) {
                    SettingsNavRow(
                        title = s("HTTP port"),
                        subtitle = httpProxyPort.toString(),
                        onClick = { showHttpPortDialog = true },
                    )
                }
            }

            SettingsSection(title = s("Split tunneling")) {
                SettingsNavRow(
                    title = s("Excluded apps"),
                    subtitle = if (excludedApps.isEmpty()) "None" else "${excludedApps.size} app(s) bypass the VPN",
                    onClick = { showSplitTunnelDialog = true },
                )
            }

            SettingsSection(title = s("Advanced")) {
                SettingsNavRow(
                    title = s("Custom edge address"),
                    subtitle = customEdgeAddress.ifBlank { "Not set" },
                    onClick = { showEdgeAddressDialog = true },
                )
                SettingsSwitchRow(
                    title = s("Chain through upstream proxy"),
                    subtitle = s("Connect to the VPN server through another proxy first"),
                    checked = upstreamProxyEnabled,
                    onCheckedChange = {
                        upstreamProxyEnabled = it
                        settingsStore.upstreamProxyEnabled = it
                    },
                )
                if (upstreamProxyEnabled) {
                    SettingsNavRow(
                        title = s("Proxy type"),
                        subtitle = upstreamProxyType.name,
                        onClick = { showUpstreamProxyTypeDialog = true },
                    )
                    SettingsNavRow(
                        title = s("Proxy address"),
                        subtitle = if (upstreamProxyHost.isBlank()) "Not set" else "$upstreamProxyHost:$upstreamProxyPort",
                        onClick = { showUpstreamProxyAddressDialog = true },
                    )
                    SettingsNavRow(
                        title = s("Proxy credentials"),
                        subtitle = if (upstreamProxyUsername.isBlank()) "None" else upstreamProxyUsername,
                        onClick = { showUpstreamProxyCredentialsDialog = true },
                    )
                }
            }

            SettingsSection(title = s("About")) {
                SettingsNavRow(
                    title = "View logs",
                    subtitle = s("Recent connection activity"),
                    onClick = onOpenLogs,
                )
                SettingsStaticRow(title = "Version", subtitle = BuildConfig.VERSION_NAME)
                SettingsStaticRow(title = s("Created by"), subtitle = "muse")
                SettingsNavRow(
                    title = "Source code",
                    subtitle = GITHUB_URL,
                    leadingIcon = Icons.Filled.Code,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) },
                )
            }

            SettingsSection(title = s("Account")) {
                SettingsNavRow(
                    title = s("Manage accounts"),
                    subtitle = s("Multi-account list, quotas and batch import"),
                    leadingIcon = Icons.Filled.AccountCircle,
                    onClick = onOpenAccount,
                )
                Button(
                    onClick = { showSignOutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(s("  Sign out"))
                }
                if (showSignOutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSignOutConfirm = false },
                        title = { Text(s("Sign out?")) },
                        text = {
                            Text(
                                s("This removes every saved account from this device. You can sign in again later.")
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showSignOutConfirm = false
                                onSignOut()
                            }) { Text(s("Sign out")) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutConfirm = false }) {
                                Text(s("Cancel"))
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = cs.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = cs.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsStaticRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DesignOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) cs.primaryContainer else cs.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, cs.primary) else null,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surface),
                contentAlignment = Alignment.Center,
            ) {
                preview()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) cs.onPrimaryContainer else cs.onSurface,
            )
        }
    }
}

@Composable
fun DesignPreview(design: SettingsStore.AppDesign) {
    val cs = MaterialTheme.colorScheme
    when (design) {
        SettingsStore.AppDesign.PIXEL -> {
            // Mini hero card with central dot.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(cs.primary),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest),
                )
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest),
                )
            }
        }
        SettingsStore.AppDesign.PORTAL -> {
            // Mini portal: concentric rings.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.18f)),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.30f)),
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(cs.primary),
                )
            }
        }
        SettingsStore.AppDesign.OLD -> {
            // Mini classic list.
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .align(Alignment.CenterHorizontally),
                )
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(cs.surfaceContainerHighest),
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsProviderPickerDialog(
    currentProvider: SettingsStore.DnsProvider,
    currentCustomUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.DnsProvider, String) -> Unit,
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var urlText by remember { mutableStateOf(currentCustomUrl) }
    val isCustom = selectedProvider == SettingsStore.DnsProvider.CUSTOM
    val isValidCustomUrl = SettingsStore.isValidDohUrl(urlText)
    val canSave = !isCustom || isValidCustomUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Tunnel DNS")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "DNS used inside the VPN tunnel for all app and browsing traffic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SettingsStore.DnsProvider.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                            )
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(provider.label)
                            val detail = when (provider) {
                                SettingsStore.DnsProvider.SYSTEM_DEFAULT -> "Resolved at the VPN edge"
                                SettingsStore.DnsProvider.CUSTOM -> "Specify an HTTPS DoH endpoint URL"
                                else -> provider.dohUrl ?: ""
                            }
                            if (detail.isNotEmpty()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (isCustom) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it.trim() },
                        label = { Text("DoH URL (https://...)") },
                        placeholder = { Text("https://example.com/dns-query") },
                        singleLine = true,
                        isError = urlText.isNotBlank() && !isValidCustomUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProvider, urlText) },
                enabled = canSave,
            ) {
                Text(s("Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun BootstrapDnsPickerDialog(
    currentProvider: SettingsStore.BootstrapDnsProvider,
    currentCustomIp: String,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.BootstrapDnsProvider, String) -> Unit,
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var ipText by remember { mutableStateOf(currentCustomIp) }
    val isCustom = selectedProvider == SettingsStore.BootstrapDnsProvider.CUSTOM
    val isValidCustomIp = SettingsStore.isValidIpAddress(ipText.trim())
    val canSave = !isCustom || isValidCustomIp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Resolver DNS")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "DNS used to resolve the VPN server address before the tunnel connects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SettingsStore.BootstrapDnsProvider.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                            )
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(provider.label)
                            Text(
                                provider.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (isCustom) {
                    OutlinedTextField(
                        value = ipText,
                        onValueChange = { ipText = it.trim() },
                        label = { Text(s("Resolver IP address")) },
                        placeholder = { Text("e.g. 76.76.2.2") },
                        singleLine = true,
                        isError = ipText.isNotBlank() && !isValidCustomIp,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProvider, ipText.trim()) },
                enabled = canSave,
            ) {
                Text(s("Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun CustomEdgeAddressDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    val trimmed = value.trim()
    val isValid = trimmed.isEmpty() || SettingsStore.isValidEdgeHost(trimmed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Custom edge address")) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(s("Host or IP")) },
                    singleLine = true,
                    isError = !isValid,
                )
                Text(
                    "Leave empty to resolve the server hostname normally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = isValid) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun SocksBindAddressPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Local address")) },
        text = {
            Column {
                SettingsStore.SOCKS_BIND_ADDRESS_PRESETS.forEach { (address, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == address, onClick = { selected = address })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = selected == address, onClick = { selected = address })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun SocksPortPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Local port")) },
        text = {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                label = { Text("Port (1\u201365535)") },
                singleLine = true,
                isError = portText.isNotBlank() && !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsedPort?.let { onConfirm(it) } }, enabled = isValid) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun HttpPortPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("HTTP port")) },
        text = {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                label = { Text("Port (1\u201365535)") },
                singleLine = true,
                isError = portText.isNotBlank() && !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsedPort?.let { onConfirm(it) } }, enabled = isValid) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun UpstreamProxyTypePickerDialog(
    current: SettingsStore.UpstreamProxyType,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.UpstreamProxyType) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Proxy type")) },
        text = {
            Column {
                SettingsStore.UpstreamProxyType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == type, onClick = { selected = type })
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = selected == type, onClick = { selected = type })
                        Text(type.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun UpstreamProxyAddressDialog(
    currentHost: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var host by remember { mutableStateOf(currentHost) }
    var portText by remember { mutableStateOf(currentPort.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = host.isNotBlank() && parsedPort != null && parsedPort in 1..65_535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Proxy address")) },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(s("Host or IP")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Port (1\u201365535)") },
                    singleLine = true,
                    isError = portText.isNotBlank() && (parsedPort == null || parsedPort !in 1..65_535),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsedPort?.let { onConfirm(host, it) } }, enabled = isValid) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun UpstreamProxyCredentialsDialog(
    currentUsername: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf(currentUsername) }
    var password by remember { mutableStateOf(currentPassword) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Proxy credentials")) },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(s("Username (optional)")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s("Password (optional)")) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(username, password) }) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

private data class InstalledAppEntry(val packageName: String, val label: String)

@Composable
private fun SplitTunnelDialog(
    current: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(current) }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps by androidx.compose.runtime.produceState<List<InstalledAppEntry>>(initialValue = emptyList()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val packageManager = context.packageManager
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .map { info: android.content.pm.ApplicationInfo ->
                    InstalledAppEntry(info.packageName, info.loadLabel(packageManager).toString())
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s("Split tunneling")) },
        text = {
            Column {
                Text(
                    "Selected apps bypass the VPN and use your normal connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(s("Search apps")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                if (installedApps.isEmpty()) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally)
                    )
                } else if (filteredApps.isEmpty()) {
                    Text(
                        "No apps match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = selected.contains(app.packageName)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (isChecked) {
                                            selected - app.packageName
                                        } else {
                                            selected + app.packageName
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selected = if (it) selected + app.packageName else selected - app.packageName
                                    },
                                )
                                Text(app.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(s("Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s("Cancel")) }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
