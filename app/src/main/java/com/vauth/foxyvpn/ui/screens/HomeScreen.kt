package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.AccountLiveStats
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.VpnAccount
import com.vauth.foxyvpn.data.model.emojiFlag
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    val multiStats by FoxyVpnService.multiAccountStats.collectAsState()
    val connectedCount by FoxyVpnService.connectedAccountCount.collectAsState()
    val accounts by app.tokenStore.accountsFlow.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()

    val haptics = LocalHapticFeedback.current
    val systemDark = isSystemInDarkTheme()
    var detailsOpen by remember { mutableStateOf(false) }
    var pingMs by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    // Measure edge latency on demand when the details panel opens.
    LaunchedEffect(detailsOpen, state) {
        if (detailsOpen && state == ConnectionState.CONNECTED) {
            val proxy = selectedProxy
            pingMs = -1
            if (proxy != null) {
                pingMs = PingUtil.measureTcpLatencyMs(proxy.host, proxy.port) ?: -2
            }
        }
    }

    // Elapsed-time clock while connected.
    var connectedSince by remember { mutableStateOf(0L) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            connectedSince = System.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                elapsedSeconds = ((System.currentTimeMillis() - connectedSince) / 1_000L).toInt()
            }
        } else {
            elapsedSeconds = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoxyVPN", style = MaterialTheme.typography.titleLargeEmphasized) },
                actions = {
                    IconButton(onClick = onOpenAccounts) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Accounts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val mode = themeController.mode
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeController.toggle(systemDark)
                        },
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Filled.LightMode
                                ThemeMode.DARK -> Icons.Filled.DarkMode
                                ThemeMode.AMOLED -> Icons.Filled.Contrast
                            },
                            contentDescription = "Theme: $mode (tap to cycle)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            HeroCard(
                state = state,
                elapsedSeconds = elapsedSeconds,
                onToggle = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                },
            )

            Spacer(Modifier.height(10.dp))

            // Tappable fact chips: place / ping / accounts -> details.
            FactChipsRow(
                state = state,
                placeLabel = selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
                flag = selectedProxy?.emojiFlag.orEmpty(),
                pingMs = pingMs,
                connectedCount = connectedCount,
                totalAccounts = accounts.size,
                onOpenDetails = { detailsOpen = !detailsOpen },
            )

            AnimatedVisibility(visible = detailsOpen && state == ConnectionState.CONNECTED) {
                DetailsPanel(
                    accounts = accounts,
                    stats = multiStats,
                    pingMs = pingMs,
                    placeLabel = selectedProxy?.let {
                        listOfNotNull(it.countryName.ifBlank { null }, it.host).joinToString(" · ")
                    } ?: "Recommended",
                    onRefreshPing = {
                        val proxy = selectedProxy
                        scope.launch {
                            pingMs = -1
                            pingMs = proxy?.let { PingUtil.measureTcpLatencyMs(it.host, it.port) } ?: -2
                        }
                    },
                )
            }

            val error = lastError
            if (error != null && state != ConnectionState.CONNECTING) {
                Spacer(Modifier.height(10.dp))
                ErrorCard(error = error)
            }

            Spacer(Modifier.height(10.dp))

            ServerRow(
                flag = selectedProxy?.emojiFlag.orEmpty(),
                name = selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
                detail = selectedProxy?.let { "${it.host}:${it.port}" } ?: "Tap to choose a location",
                onClick = onOpenServers,
            )

            Spacer(Modifier.height(6.dp))

            AccountsRow(
                accounts = accounts,
                connectedCount = connectedCount,
                state = state,
                onClick = onOpenAccounts,
            )

            Spacer(Modifier.height(6.dp))

            SpeedRow(stats = multiStats, live = state == ConnectionState.CONNECTED)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(
    state: ConnectionState,
    elapsedSeconds: Int,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING
    val container by animateColorAsState(
        targetValue = if (live) cs.primaryContainer else cs.surfaceContainer,
        animationSpec = tween(400),
        label = "hero-container",
    )
    val ink by animateColorAsState(
        targetValue = if (live) cs.onPrimaryContainer else cs.onSurface,
        animationSpec = tween(400),
        label = "hero-ink",
    )
    val sub by animateColorAsState(
        targetValue = if (live) cs.onPrimaryContainer.copy(alpha = 0.75f) else cs.onSurfaceVariant,
        animationSpec = tween(400),
        label = "hero-sub",
    )

    Surface(
        shape = RoundedCornerShape(40.dp),
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectCore(
                state = state,
                elapsedSeconds = elapsedSeconds,
                onToggle = onToggle,
            )
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "hero-state",
            ) { s ->
                Text(
                    text = when (s) {
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.DISCONNECTED -> "Disconnected"
                    },
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = ink,
                    textAlign = TextAlign.Center,
                )
            }
            if (busy) {
                Spacer(Modifier.height(10.dp))
                LinearWavyProgressIndicator(
                    modifier = Modifier.width(150.dp),
                    color = cs.primary,
                    trackColor = cs.primary.copy(alpha = 0.18f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap the core to cancel",
                    style = MaterialTheme.typography.bodySmall,
                    color = sub,
                )
            }
        }
    }
}

/**
 * The morphing connect core: a canvas-drawn blob that morphs between a
 * circle (idle) and a soft burst (connected), rotating slowly while active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectCore(
    state: ConnectionState,
    elapsedSeconds: Int,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val live = state == ConnectionState.CONNECTED
    val busy = state == ConnectionState.CONNECTING

    val morph by animateFloatAsState(
        targetValue = if (live) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "core-morph",
    )
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(live, busy) {
        if (live || busy) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(14_000))
            }
        }
    }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "core-squash",
    )
    val start = cs.primary
    val end = cs.tertiary

    Box(
        modifier = Modifier
            .size(168.dp)
            .graphicsLayer {
                scaleX = squash
                scaleY = squash
            }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cornerFraction = 0.5f - 0.22f * morph // circle -> soft squircle
            val radius = size.minDimension / 2f * cornerFraction
            rotate(rotation.value) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(start, end), Offset.Zero, Offset(size.width, size.height)),
                    size = size,
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        }
        if (live) {
            Text(
                text = formatElapsed(elapsedSeconds),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = cs.onPrimary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Power,
                contentDescription = if (busy) "Cancel" else "Connect",
                tint = cs.onPrimary,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

private fun formatElapsed(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactChipsRow(
    state: ConnectionState,
    placeLabel: String,
    flag: String,
    pingMs: Int,
    connectedCount: Int,
    totalAccounts: Int,
    onOpenDetails: () -> Unit,
) {
    val live = state == ConnectionState.CONNECTED
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FactChip(onClick = onOpenDetails) {
            if (flag.isNotBlank()) {
                Text(flag, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
            } else {
                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(placeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (live) {
            FactChip(onClick = onOpenDetails) {
                Text(
                    when (pingMs) {
                        -1 -> "ping …"
                        -2 -> "ping n/a"
                        else -> "$pingMs ms"
                    },
                    maxLines = 1,
                )
            }
            FactChip(onClick = onOpenDetails) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("$connectedCount/$totalAccounts", maxLines = 1)
            }
        }
    }
}

@Composable
private fun FactChip(onClick: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = cs.surfaceContainerHigh,
        contentColor = cs.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.labelLarge,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DetailsPanel(
    accounts: List<VpnAccount>,
    stats: List<AccountLiveStats>,
    pingMs: Int,
    placeLabel: String,
    onRefreshPing: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val byId = remember(stats) { stats.associateBy { it.accountId } }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Connection details",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefreshPing, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh ping", modifier = Modifier.size(18.dp))
                }
            }
            DetailRow("Location", placeLabel)
            DetailRow(
                "Ping",
                when (pingMs) {
                    -1 -> "measuring…"
                    -2 -> "unavailable"
                    else -> "$pingMs ms"
                },
            )
            if (accounts.isEmpty()) {
                Text(
                    "No accounts yet — add some from the Accounts screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            } else {
                accounts.forEach { account ->
                    val s = byId[account.id]
                    DetailRow(
                        label = account.email.ifBlank { "Account" },
                        value = when {
                            s == null || !account.isEnabled -> "idle"
                            s.isExhausted -> "quota exhausted"
                            !s.isConnected -> "dialing…"
                            else -> "↓ ${formatBytesPerSecond(s.speedDownBytesPerSec)} · ↑ ${formatBytesPerSecond(s.speedUpBytesPerSec)}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorCard(error: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cs.errorContainer,
        contentColor = cs.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ServerRow(flag: String, name: String, detail: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (flag.isNotBlank()) {
                    Text(flag, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = cs.onPrimary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountsRow(
    accounts: List<VpnAccount>,
    connectedCount: Int,
    state: ConnectionState,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val statusColors = com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = cs.onTertiaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Accounts", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    if (accounts.isEmpty()) "Tap to add your first account"
                    else "${accounts.count { it.isEnabled }} enabled · $connectedCount connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            // Per-account status dots.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                accounts.take(8).forEach { account ->
                    val dot = when {
                        state == ConnectionState.CONNECTED && account.isEnabled -> statusColors.connected
                        !account.isEnabled -> cs.outlineVariant
                        else -> cs.outline
                    }
                    val animated by animateColorAsState(dot, tween(300), label = "acct-dot")
                    Box(Modifier.size(10.dp).clip(CircleShape).background(animated))
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpeedRow(stats: List<AccountLiveStats>, live: Boolean) {
    val cs = MaterialTheme.colorScheme
    val down = stats.sumOf { it.speedDownBytesPerSec }
    val up = stats.sumOf { it.speedUpBytesPerSec }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpeedTile(
            icon = Icons.Filled.Download,
            label = "Download",
            value = if (live) formatBytesPerSecond(down) else "—",
            badge = cs.tertiaryContainer,
            onBadge = cs.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        SpeedTile(
            icon = Icons.Filled.Upload,
            label = "Upload",
            value = if (live) formatBytesPerSecond(up) else "—",
            badge = cs.primaryContainer,
            onBadge = cs.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SpeedTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    badge: Color,
    onBadge: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainerHigh, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = onBadge, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1)
            }
        }
    }
}
