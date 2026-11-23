package com.ffa.flashforalert.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ffa.flashforalert.ui.FfaUiState
import com.ffa.flashforalert.ui.FfaViewModel
import com.ffa.flashforalert.ui.screens.MainScreen
import com.ffa.flashforalert.ui.screens.OnboardingScreen
import com.ffa.flashforalert.ui.screens.SettingsScreen

@Composable
fun FfaNav(
    uiState: FfaUiState,
    viewModel: FfaViewModel,
    startInSetup: Boolean
) {
    val navController = rememberNavController()
    val start = androidx.compose.runtime.remember {
        when {
            !uiState.settings.onboardingCompleted -> FfaRoute.ONBOARDING.route
            startInSetup && !uiState.notificationAccessGranted -> FfaRoute.ONBOARDING.route
            else -> FfaRoute.MAIN.route
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(FfaRoute.ONBOARDING.route) {
            OnboardingScreen(
                uiState = uiState,
                onOpenNotificationAccess = { viewModel.openNotificationAccessSettings(it) },
                onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings(it) },
                onContinue = {
                    viewModel.setOnboardingCompleted()
                    navController.navigate(FfaRoute.MAIN.route) {
                        popUpTo(FfaRoute.ONBOARDING.route) { inclusive = true }
                    }
                },
                onRefresh = { viewModel.refreshPermissionState() }
            )
        }
        composable(FfaRoute.MAIN.route) {
            MainScreen(
                uiState = uiState,
                onSearchChange = viewModel::onSearchChange,
                onToggleApp = viewModel::toggleApp,
                onToggleAll = viewModel::setAllApps,
                onOpenSettings = { navController.navigate(FfaRoute.SETTINGS.route) },
                onOpenNotificationAccess = { viewModel.openNotificationAccessSettings(it) },
                onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings(it) },
                onDismissBatteryPrompt = viewModel::dismissBatteryPrompt,
                loadIcon = viewModel::loadAppIcon,
                onRefresh = {
                    viewModel.refreshPermissionState()
                    viewModel.reloadApps()
                }
            )
        }
        composable(FfaRoute.SETTINGS.route) {
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onPreset = viewModel::setNotificationPreset,
                onNotificationTiming = viewModel::setNotificationTimingCustom,
                onCallTiming = viewModel::setCallTiming,
                onBrightness = viewModel::setBrightnessMode,
                onVibration = viewModel::setVibrationEnabled,
                onPreviewNotification = viewModel::previewNotificationPattern,
                onPreviewCall = viewModel::previewCallPattern,
                onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings(it) },
                onRefreshStatus = { viewModel.refreshPermissionState() }
            )
        }
    }
}