package com.vauth.foxyvpn

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.ui.navigation.FoxyNavGraph
import com.vauth.foxyvpn.ui.l10n.LocalL10n
import com.vauth.foxyvpn.ui.l10n.rememberResolvedLang
import com.vauth.foxyvpn.ui.theme.AppLanguage
import com.vauth.foxyvpn.ui.theme.FoxyVpnTheme
import com.vauth.foxyvpn.ui.theme.rememberAppearanceController
import com.vauth.foxyvpn.vpn.FoxyVpnService

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            FoxyVpnService.start(this)
        } else {

            Toast.makeText(
                this,
                "FoxyVPN needs Android's VPN permission to tunnel this device's traffic. " +
                    "Proxy-only mode in Settings works without it.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {  }

    private fun requestConnect() {
        requestNotificationPermissionIfNeeded()

        if ((application as FoxyVpnApp).settingsStore.proxyOnlyMode) {
            FoxyVpnService.start(this)
            return
        }

        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            FoxyVpnService.start(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FoxyVpnApp
        setContent {
            val appearance = rememberAppearanceController(app.settingsStore)

            val density = LocalDensity.current
            val fontScale = appearance.uiFontScale.scale
            val scaledDensity = remember(density, fontScale) {
                if (fontScale == 1f) density else Density(density.density, density.fontScale * fontScale)
            }
            val systemDirection = LocalLayoutDirection.current
            val layoutDirection = when (appearance.appLanguage) {
                AppLanguage.System -> systemDirection
                AppLanguage.English -> LayoutDirection.Ltr
                AppLanguage.Persian -> LayoutDirection.Rtl
            }
            val resolvedLang = rememberResolvedLang(appearance.appLanguage)

            FoxyVpnTheme(
                themeMode = appearance.themeMode,
                dynamicColor = appearance.dynamicColor,
                colorStyle = appearance.colorStyle,
                accentColorIndex = appearance.accentColorIndex,
                colorContrast = appearance.colorContrast,
            ) {
                CompositionLocalProvider(
                    LocalDensity provides scaledDensity,
                    LocalLayoutDirection provides layoutDirection,
                    LocalL10n provides resolvedLang,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        FoxyNavGraph(
                            navController = navController,
                            app = app,
                            appearance = appearance,
                            onRequestConnect = { requestConnect() },
                            onDisconnect = { FoxyVpnService.stop(this) },
                        )
                    }
                }
            }
        }
    }
}
