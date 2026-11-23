package com.ffa.flashforalert.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ffa.flashforalert.apps.InstalledAppsRepository
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.core.permissions.PermissionUtils
import com.ffa.flashforalert.data.preferences.FfaSettings
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.domain.models.FlashPattern
import com.ffa.flashforalert.domain.models.InstalledApp
import com.ffa.flashforalert.domain.models.NotificationPreset
import com.ffa.flashforalert.quicksettings.FfaQuickSettingsTileService
import com.ffa.flashforalert.service.FfaServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FfaUiState(
    val settings: FfaSettings = FfaSettings(),
    val apps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val isLoadingApps: Boolean = true,
    val notificationAccessGranted: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val hasFlash: Boolean = false,
    val supportsTorchStrength: Boolean = false,
    val maxTorchStrength: Int = 1
) {
    val filteredApps: List<InstalledApp>
        get() = if (searchQuery.isBlank()) apps else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    val selectedCount: Int get() = apps.count { it.enabled }
    val allSelected: Boolean get() = apps.isNotEmpty() && apps.all { it.enabled }
    val noneSelected: Boolean get() = apps.all { !it.enabled }
}

class FfaViewModel(application: Application) : AndroidViewModel(application) {
    private val appsRepo = InstalledAppsRepository(application)
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _search = MutableStateFlow("")
    private val _loading = MutableStateFlow(true)
    private val _access = MutableStateFlow(false)
    private val _batteryUnrestricted = MutableStateFlow(false)
    private var batteryRecheckJob: Job? = null

    private val settingsFlow = runCatching {
        FfaServices.init(getApplication())
        FfaServices.settingsState
    }.getOrElse {
        MutableStateFlow(FfaSettings())
    }

    val uiState: StateFlow<FfaUiState> = combine(
        settingsFlow,
        _apps,
        _search,
        _loading,
        combine(_access, _batteryUnrestricted) { access, batteryUnrestricted ->
            access to batteryUnrestricted
        }
    ) { settings, apps, search, loading, (access, batteryUnrestricted) ->
        val hasFlash = runCatching { FfaServices.torchController.hasFlashUnit() }.getOrDefault(false)
        val supportsStrength = runCatching { FfaServices.torchController.supportsStrength }.getOrDefault(false)
        val maxStrength = runCatching { FfaServices.torchController.maxStrengthLevel }.getOrDefault(1)
        FfaUiState(
            settings = settings,
            apps = apps.map { it.copy(enabled = settings.selectedPackages.contains(it.packageName)) },
            searchQuery = search,
            isLoadingApps = loading,
            notificationAccessGranted = access,
            isBatteryOptimizationDisabled = batteryUnrestricted,
            hasFlash = hasFlash,
            supportsTorchStrength = supportsStrength,
            maxTorchStrength = maxStrength
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FfaUiState())

    init {
        runCatching { FfaServices.init(getApplication()) }
        refreshPermissionState()
        reloadApps()
    }

    fun refreshPermissionState() {
        val granted = runCatching { PermissionUtils.isNotificationListenerEnabled(getApplication()) }.getOrDefault(false)
        val batteryUnrestricted = runCatching { PermissionUtils.isIgnoringBatteryOptimizations(getApplication()) }.getOrDefault(false)
        _access.value = granted
        _batteryUnrestricted.value = batteryUnrestricted
        runCatching { FfaServices.updateNotificationAccess(granted) }
        requestTileUpdate()

        batteryRecheckJob?.cancel()
        if (!batteryUnrestricted) {
            batteryRecheckJob = viewModelScope.launch {
                for (delayMs in listOf(350L, 900L)) {
                    delay(delayMs)
                    val updated = runCatching { PermissionUtils.isIgnoringBatteryOptimizations(getApplication()) }.getOrDefault(false)
                    _batteryUnrestricted.value = updated
                    if (updated) break
                }
            }
        }
    }

    fun reloadApps() {
        viewModelScope.launch {
            _loading.value = true
            val selected = runCatching { FfaServices.currentSettings().selectedPackages }.getOrDefault(emptySet())
            val list = runCatching { appsRepo.loadInstalledApps(selected) }.getOrDefault(emptyList())
            _apps.value = list
            _loading.value = false
        }
    }

    fun onSearchChange(q: String) { _search.value = q }

    fun setFfaEnabled(enabled: Boolean) {
        viewModelScope.launch {
            FfaServices.preferences.setFfaEnabled(enabled)
            if (!enabled) FfaServices.flashEngine.cancel()
            requestTileUpdate()
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = FfaServices.currentSettings().selectedPackages.toMutableSet()
            if (enabled) current.add(packageName) else current.remove(packageName)
            FfaServices.preferences.setSelectedPackages(current)
            requestTileUpdate()
        }
    }

    fun setAllApps(enabled: Boolean) {
        viewModelScope.launch {
            val pkgs = if (enabled) _apps.value.map { it.packageName }.toSet() else emptySet()
            FfaServices.preferences.setSelectedPackages(pkgs)
            requestTileUpdate()
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch { FfaServices.preferences.setOnboardingCompleted(true) }
    }

    fun setNotificationPreset(preset: NotificationPreset) {
        viewModelScope.launch {
            val on = if (preset == NotificationPreset.CUSTOM) uiState.value.settings.notificationOnMs else preset.onMs
            val off = if (preset == NotificationPreset.CUSTOM) uiState.value.settings.notificationOffMs else preset.offMs
            FfaServices.preferences.setNotificationTiming(preset, on, off)
        }
    }

    fun setNotificationTimingCustom(onMs: Long, offMs: Long) {
        viewModelScope.launch {
            FfaServices.preferences.setNotificationTiming(NotificationPreset.CUSTOM, onMs, offMs)
        }
    }

    fun setCallTiming(onMs: Long, offMs: Long) {
        viewModelScope.launch { FfaServices.preferences.setCallTiming(onMs, offMs) }
    }

    fun setBrightnessMode(mode: FlashBrightnessMode) {
        viewModelScope.launch { FfaServices.preferences.setBrightnessMode(mode) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { FfaServices.preferences.setVibrationEnabled(enabled) }
    }

    fun previewNotificationPattern() {
        val s = uiState.value.settings
        val pattern = FlashPattern(
            initialOffMs = FlashDefaults.NOTIFICATION_INITIAL_OFF_MS,
            onMs = s.notificationOnMs,
            offMs = s.notificationOffMs,
            pulses = FlashDefaults.NOTIFICATION_PULSES
        )
        FfaServices.flashEngine.play(pattern, s.brightnessMode, s.vibrationEnabled)
    }

    fun previewCallPattern() {
        val s = uiState.value.settings
        val pattern = FlashPattern(
            initialOffMs = 0L,
            onMs = s.callOnMs,
            offMs = s.callOffMs,
            pulses = 2
        )
        FfaServices.flashEngine.play(pattern, s.brightnessMode, s.vibrationEnabled)
    }

    fun requestTileUpdate() {
        runCatching {
            val ctx = getApplication<Application>()
            TileService.requestListeningState(ctx, ComponentName(ctx, FfaQuickSettingsTileService::class.java))
        }
    }

    fun loadAppIcon(packageName: String): Drawable? = appsRepo.loadIcon(packageName)

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        PermissionUtils.openBatteryOptimizationSettings(context)
    }

    fun dismissBatteryPrompt() {
        viewModelScope.launch {
            runCatching { FfaServices.preferences.setBatteryPromptDismissed(true) }
        }
    }
}