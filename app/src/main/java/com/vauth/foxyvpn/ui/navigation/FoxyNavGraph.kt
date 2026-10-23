package com.vauth.foxyvpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.ui.screens.AccountScreen
import com.vauth.foxyvpn.ui.screens.HomeScreen
import com.vauth.foxyvpn.ui.screens.LoginScreen
import com.vauth.foxyvpn.ui.screens.LogsScreen
import com.vauth.foxyvpn.ui.screens.ServerListScreen
import com.vauth.foxyvpn.ui.screens.SettingsScreen
import com.vauth.foxyvpn.ui.screens.SplashScreen
import com.vauth.foxyvpn.ui.screens.transitionTransform
import com.vauth.foxyvpn.ui.theme.AppearanceController

object FoxyRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val LOGS = "logs"
}

@Composable
fun FoxyNavGraph(
    navController: NavHostController = rememberNavController(),
    app: FoxyVpnApp,
    appearance: AppearanceController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val transition = transitionTransform(appearance.screenTransition, appearance.reduceMotion)

    NavHost(
        navController = navController,
        startDestination = FoxyRoutes.SPLASH,
        enterTransition = { transition.targetContentEnter },
        exitTransition = { transition.initialContentExit },
        popEnterTransition = { transition.targetContentEnter },
        popExitTransition = { transition.initialContentExit },
    ) {
        composable(FoxyRoutes.SPLASH) {
            SplashScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
                onNeedsLogin = {
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.LOGIN) {
            LoginScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.HOME) {
            HomeScreen(
                app = app,
                appearance = appearance,
                onRequestConnect = onRequestConnect,
                onDisconnect = onDisconnect,
                onOpenServers = { navController.navigate(FoxyRoutes.SERVERS) },
                onOpenSettings = { navController.navigate(FoxyRoutes.SETTINGS) },
                onOpenAccounts = { navController.navigate(FoxyRoutes.ACCOUNT) },
            )
        }
        composable(FoxyRoutes.SERVERS) {
            ServerListScreen(
                serverListClient = app.serverListClient,
                proxyStateStore = app.proxyStateStore,
                settingsStore = app.settingsStore,
                appearance = appearance,
                onServerSelected = {
                    if (appearance.homeAfterSelect) {
                        navController.navigate(FoxyRoutes.HOME) {
                            popUpTo(FoxyRoutes.HOME) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.SETTINGS) {
            SettingsScreen(
                settingsStore = app.settingsStore,
                appearance = appearance,
                onOpenLogs = { navController.navigate(FoxyRoutes.LOGS) },
                onOpenAccount = { navController.navigate(FoxyRoutes.ACCOUNT) },
                onSignOut = {
                    onDisconnect()
                    app.tokenStore.clear()
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.ACCOUNT) {
            AccountScreen(
                authRepository = app.authRepository,
                tokenStore = app.tokenStore,
                appearance = appearance,
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.LOGS) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
