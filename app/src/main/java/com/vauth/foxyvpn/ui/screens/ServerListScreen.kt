package com.vauth.foxyvpn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.vauth.foxyvpn.ui.i18n.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.data.ProxyStateStore
import com.vauth.foxyvpn.data.RECOMMENDED_COUNTRY_CODE
import com.vauth.foxyvpn.data.ServerListClient
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.data.model.ProxyCandidate
import com.vauth.foxyvpn.data.model.VpnCountry
import com.vauth.foxyvpn.data.model.emojiFlag
import com.vauth.foxyvpn.ui.theme.AppearanceController
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed class LocationRow {
    data class RecommendedRow(val country: VpnCountry) : LocationRow()
    data class CountryHeader(val country: VpnCountry) : LocationRow()
    data class CityRow(val country: VpnCountry, val cityIndex: Int) : LocationRow()
}

private enum class PingState { PENDING, DONE, FAILED }

@Composable
fun ServerListScreen(
    serverListClient: ServerListClient,
    proxyStateStore: ProxyStateStore,
    settingsStore: SettingsStore,
    appearance: AppearanceController,
    onServerSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var countries by remember { mutableStateOf<List<VpnCountry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedState by proxyStateStore.selectedProxyFlow.collectAsState()

    val pingResults = remember { mutableStateMapOf<String, Int>() }
    val pingStates = remember { mutableStateMapOf<String, PingState>() }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { serverListClient.loadCachedCountries() }
        if (cached.isNotEmpty()) {
            countries = cached
            isLoading = false
        }
        runCatching { serverListClient.fetchCountries() }
            .onSuccess { fresh ->
                countries = fresh
                errorMessage = null
            }
            .onFailure {
                if (countries.isEmpty()) {
                    errorMessage = it.message ?: "Failed to load server list"
                }
            }
        isLoading = false
    }

    val ordered = remember(countries) {
        countries.sortedWith(compareBy({ it.code != RECOMMENDED_COUNTRY_CODE }, { it.name }))
    }

    val rows = remember(ordered) {
        buildList {
            for (country in ordered) {
                if (country.code == RECOMMENDED_COUNTRY_CODE) {
                    add(LocationRow.RecommendedRow(country))
                    continue
                }
                add(LocationRow.CountryHeader(country))
                country.cities.indices.forEach { index -> add(LocationRow.CityRow(country, index)) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null && countries.isEmpty() -> Text(
                    text = errorMessage.orEmpty(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp),
                )
                else -> {
                    if (appearance.doubleColumnDisplay) {
                        LocationGrid(
                            rows = rows,
                            countries = countries,
                            selectedState = selectedState,
                            showPing = appearance.showServerPing,
                            showUsage = appearance.showServerUsage,
                            monospace = appearance.monospaceAddress,
                            pingResults = pingResults,
                            pingStates = pingStates,
                            proxyStateStore = proxyStateStore,
                            onError = { errorMessage = it },
                            onServerSelected = onServerSelected,
                        )
                    } else {
                        LazyColumn {
                    items(
                        rows,
                        key = { row ->
                            when (row) {
                                is LocationRow.RecommendedRow -> "recommended:${row.country.code}"
                                is LocationRow.CountryHeader -> "country:${row.country.code}"
                                is LocationRow.CityRow ->
                                    "city:${row.country.code}:${row.country.cities[row.cityIndex].code}"
                            }
                        },
                    ) { row ->

                        val selected = selectedState
                        when (row) {
                            is LocationRow.RecommendedRow -> ListItem(
                                leadingContent = {
                                    Icon(Icons.Filled.Public, contentDescription = null)
                                },
                                headlineContent = { Text("Recommended", fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("Automatically chosen for the best performance") },
                                trailingContent = {
                                    if (row.country.code == selected?.countryCode) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Selected")
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable {
                                        val candidates = ServerListClient.candidatesForCountry(countries, row.country.code)
                                        val chosen = candidates.randomOrNull()
                                        if (chosen != null) {
                                            proxyStateStore.save(chosen)
                                            onServerSelected()
                                        } else {
                                            errorMessage = "No available servers for ${row.country.name}"
                                        }
                                    },
                            )
                            is LocationRow.CountryHeader -> ListItem(
                                leadingContent = {
                                    Text(
                                        text = row.country.emojiFlag,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                },
                                headlineContent = { Text(row.country.name, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            is LocationRow.CityRow -> {
                                val city = row.country.cities[row.cityIndex]
                                val serverCount = city.servers.size

                                val pingKey = "${row.country.code}:${city.code}"
                                val pingMs = pingResults[pingKey]
                                val pingState = pingStates[pingKey] ?: PingState.PENDING

                                if (appearance.showServerPing) {
                                    LaunchedEffect(pingKey) {
                                        if (pingStates.containsKey(pingKey)) return@LaunchedEffect
                                        val target = city.servers.firstOrNull { !it.quarantined }
                                            ?.let { ServerListClient.defaultConnectTarget(it) }
                                        if (target == null) {
                                            pingStates[pingKey] = PingState.FAILED
                                            return@LaunchedEffect
                                        }
                                        val result = PingUtil.measureTcpLatencyMs(target.first, target.second)
                                        if (result != null) {
                                            pingResults[pingKey] = result
                                            pingStates[pingKey] = PingState.DONE
                                        } else {
                                            pingStates[pingKey] = PingState.FAILED
                                        }
                                    }
                                }

                                ListItem(

                                    headlineContent = { Text(city.code) },
                                    supportingContent = {
                                        val primaryHostname = city.servers.firstOrNull()?.hostname
                                        val serverLabel = when {
                                            primaryHostname == null -> "No servers available"
                                            serverCount > 1 -> "$primaryHostname (+${serverCount - 1} more)"
                                            else -> primaryHostname
                                        }
                                        val pingLabel = when {
                                            !appearance.showServerPing -> null
                                            pingMs != null -> "$pingMs ms"
                                            pingState == PingState.FAILED -> "ping unavailable"
                                            else -> "pinging\u2026"
                                        }
                                        Text(
                                            text = if (pingLabel != null) "$serverLabel \u2022 $pingLabel" else serverLabel,
                                            fontFamily = if (appearance.monospaceAddress) androidx.compose.ui.text.font.FontFamily.Monospace
                                            else androidx.compose.ui.text.font.FontFamily.Default,
                                        )
                                    },
                                    trailingContent = {
                                        if (row.country.code == selected?.countryCode &&
                                            city.code == selected?.cityCode
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected")
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(start = 24.dp, end = 8.dp)
                                        .clickable {
                                            val candidates = ServerListClient.candidatesForCity(countries, row.country.code, city.code)
                                            val chosen = candidates.randomOrNull()
                                            if (chosen != null) {
                                                proxyStateStore.save(chosen)
                                                onServerSelected()
                                            } else {
                                                errorMessage = "No available servers in ${city.code}"
                                            }
                                        },
                                )
                            }
                        }
                    }
                        }
                    }
                }
            }
        }
    }
}

/** Two-column card grid for the location list (double-column display setting). */
@Composable
private fun LocationGrid(
    rows: List<LocationRow>,
    countries: List<VpnCountry>,
    selectedState: ProxyCandidate?,
    showPing: Boolean,
    showUsage: Boolean,
    monospace: Boolean,
    pingResults: SnapshotStateMap<String, Int>,
    pingStates: SnapshotStateMap<String, PingState>,
    proxyStateStore: ProxyStateStore,
    onError: (String) -> Unit,
    onServerSelected: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            rows,
            key = { row ->
                when (row) {
                    is LocationRow.RecommendedRow -> "recommended:${row.country.code}"
                    is LocationRow.CountryHeader -> "country:${row.country.code}"
                    is LocationRow.CityRow ->
                        "city:${row.country.code}:${row.country.cities[row.cityIndex].code}"
                }
            },
            span = { row ->
                when (row) {
                    is LocationRow.CityRow -> GridItemSpan(1)
                    else -> GridItemSpan(2)
                }
            },
        ) { row ->
            when (row) {
                is LocationRow.RecommendedRow -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cs.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val candidates = ServerListClient.candidatesForCountry(countries, row.country.code)
                                val chosen = candidates.randomOrNull()
                                if (chosen != null) {
                                    proxyStateStore.save(chosen)
                                    onServerSelected()
                                } else {
                                    onError("No available servers for ${row.country.name}")
                                }
                            },
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Public, contentDescription = null, tint = cs.onPrimaryContainer)
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Recommended", fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
                                Text(
                                    "Best performance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onPrimaryContainer.copy(alpha = 0.75f),
                                )
                            }
                            if (row.country.code == selectedState?.countryCode) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = cs.onPrimaryContainer)
                            }
                        }
                    }
                }
                is LocationRow.CountryHeader -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    ) {
                        Text(row.country.emojiFlag, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(8.dp))
                        Text(row.country.name, fontWeight = FontWeight.SemiBold)
                    }
                }
                is LocationRow.CityRow -> {
                    val city = row.country.cities[row.cityIndex]
                    val pingKey = "${row.country.code}:${city.code}"
                    val pingMs = pingResults[pingKey]
                    val pingState = pingStates[pingKey] ?: PingState.PENDING
                    val isSelected = row.country.code == selectedState?.countryCode &&
                        city.code == selectedState?.cityCode

                    if (showPing) {
                        LaunchedEffect(pingKey) {
                            if (pingStates.containsKey(pingKey)) return@LaunchedEffect
                            val target = city.servers.firstOrNull { !it.quarantined }
                                ?.let { ServerListClient.defaultConnectTarget(it) }
                            if (target == null) {
                                pingStates[pingKey] = PingState.FAILED
                                return@LaunchedEffect
                            }
                            val result = PingUtil.measureTcpLatencyMs(target.first, target.second)
                            if (result != null) {
                                pingResults[pingKey] = result
                                pingStates[pingKey] = PingState.DONE
                            } else {
                                pingStates[pingKey] = PingState.FAILED
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) cs.primaryContainer else cs.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, cs.primary) else null,
                        modifier = Modifier.clickable {
                            val candidates = ServerListClient.candidatesForCity(countries, row.country.code, city.code)
                            val chosen = candidates.randomOrNull()
                            if (chosen != null) {
                                proxyStateStore.save(chosen)
                                onServerSelected()
                            } else {
                                onError("No available servers in ${city.code}")
                            }
                        },
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    city.code,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = cs.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            if (showUsage) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${city.servers.size} server(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = city.servers.firstOrNull()?.hostname ?: "No servers",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                                ),
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (showPing) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        pingMs != null -> "$pingMs ms"
                                        pingState == PingState.FAILED -> "ping unavailable"
                                        else -> "pinging…"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
