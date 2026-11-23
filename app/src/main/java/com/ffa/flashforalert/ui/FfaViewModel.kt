package com.ffa.flashforalert.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.core.permissions.PermissionUtils
import com.ffa.flashforalert.data.preferences.FfaSettings
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.domain.models.FlashPattern
import com.ffa.flashforalert.quicksettings.FfaQuickSettingsTileService
import com.ffa.flashforalert.service.FfaServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FfaUiState(
    val settings: FfaSettings = FfaSettings(),
    val notificationAccessGranted: Boolean = false,
    val hasFlash: Boolean = false,
    val supportsTorchStrength: Boolean = false,
    val maxTorchStrength: Int = 1
)

class FfaViewModel(application: Application) : AndroidViewModel(application) {
    private val _access = MutableStateFlow(false)

    private val settingsFlow = runCatching {
        FfaServices.init(getApplication())
        FfaServices.settingsState
    }.getOrElse { MutableStateFlow(FfaSettings()) }

    val uiState: StateFlow<FfaUiState> = combine(settingsFlow, _access) { settings, access ->
        val hasFlash = runCatching { FfaServices.torchController.hasFlashUnit() }.getOrDefault(false)
        val supportsStrength = runCatching { FfaServices.torchController.supportsStrength }.getOrDefault(false)
        val maxStrength = runCatching { FfaServices.torchController.maxStrengthLevel }.getOrDefault(1)
        FfaUiState(
            settings = settings,
            notificationAccessGranted = access,
            hasFlash = hasFlash,
            supportsTorchStrength = supportsStrength,
            maxTorchStrength = maxStrength
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FfaUiState())

    init {
        runCatching { FfaServices.init(getApplication()) }
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        val granted = runCatching { PermissionUtils.isNotificationListenerEnabled(getApplication()) }.getOrDefault(false)
        _access.value = granted
        runCatching { FfaServices.updateNotificationAccess(granted) }
        requestTileUpdate()
    }

    fun setFfaEnabled(enabled: Boolean) {
        viewModelScope.launch {
            FfaServices.preferences.setFfaEnabled(enabled)
            if (!enabled) FfaServices.flashEngine.cancel()
            requestTileUpdate()
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch { FfaServices.preferences.setOnboardingCompleted(true) }
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

    fun previewCallPattern() {
        val s = uiState.value.settings
        val pattern = FlashPattern(
            initialOffMs = 0L,
            onMs = s.callOnMs,
            offMs = s.callOffMs,
            pulses = 2,
            repeatUntilCancelled = false
        )
        FfaServices.flashEngine.play(pattern, s.brightnessMode, s.vibrationEnabled)
    }

    fun requestTileUpdate() {
        runCatching {
            val ctx = getApplication<Application>()
            TileService.requestListeningState(ctx, ComponentName(ctx, FfaQuickSettingsTileService::class.java))
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }
}