package com.ffa.flashforalert.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.domain.models.NotificationPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ffa_settings")

data class FfaSettings(
    val ffaEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val notificationPreset: NotificationPreset = NotificationPreset.IPHONE_SHARP,
    val notificationOnMs: Long = FlashDefaults.NOTIFICATION_ON_MS,
    val notificationOffMs: Long = FlashDefaults.NOTIFICATION_OFF_MS,
    val callOnMs: Long = FlashDefaults.CALL_ON_MS,
    val callOffMs: Long = FlashDefaults.CALL_OFF_MS,
    val brightnessMode: FlashBrightnessMode = FlashBrightnessMode.MAX,
    val vibrationEnabled: Boolean = false,
    val batteryPromptDismissed: Boolean = false
)

class FfaPreferencesRepository(private val context: Context) {
    private object Keys {
        val FFA_ENABLED = booleanPreferencesKey("ffa_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_PACKAGES = stringSetPreferencesKey("selected_packages")
        val NOTIFICATION_PRESET = stringPreferencesKey("notification_preset")
        val NOTIFICATION_ON_MS = longPreferencesKey("notification_on_ms")
        val NOTIFICATION_OFF_MS = longPreferencesKey("notification_off_ms")
        val CALL_ON_MS = longPreferencesKey("call_on_ms")
        val CALL_OFF_MS = longPreferencesKey("call_off_ms")
        val BRIGHTNESS_MODE = stringPreferencesKey("brightness_mode")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val BATTERY_PROMPT_DISMISSED = booleanPreferencesKey("battery_prompt_dismissed")
    }

    val settingsFlow: Flow<FfaSettings> = context.dataStore.data.map { p ->
        FfaSettings(
            ffaEnabled = p[Keys.FFA_ENABLED] ?: false,
            onboardingCompleted = p[Keys.ONBOARDING_COMPLETED] ?: false,
            selectedPackages = p[Keys.SELECTED_PACKAGES] ?: emptySet(),
            notificationPreset = NotificationPreset.fromName(p[Keys.NOTIFICATION_PRESET]),
            notificationOnMs = (p[Keys.NOTIFICATION_ON_MS] ?: FlashDefaults.NOTIFICATION_ON_MS)
                .coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS),
            notificationOffMs = (p[Keys.NOTIFICATION_OFF_MS] ?: FlashDefaults.NOTIFICATION_OFF_MS)
                .coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS),
            callOnMs = (p[Keys.CALL_ON_MS] ?: FlashDefaults.CALL_ON_MS)
                .coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS),
            callOffMs = (p[Keys.CALL_OFF_MS] ?: FlashDefaults.CALL_OFF_MS)
                .coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS),
            brightnessMode = FlashBrightnessMode.fromName(p[Keys.BRIGHTNESS_MODE]),
            vibrationEnabled = p[Keys.VIBRATION_ENABLED] ?: false,
            batteryPromptDismissed = p[Keys.BATTERY_PROMPT_DISMISSED] ?: false
        )
    }

    suspend fun setFfaEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.FFA_ENABLED] = enabled } }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed } }
    suspend fun setSelectedPackages(packages: Set<String>) { context.dataStore.edit { it[Keys.SELECTED_PACKAGES] = packages } }
    suspend fun setNotificationTiming(preset: NotificationPreset, onMs: Long, offMs: Long) {
        context.dataStore.edit {
            it[Keys.NOTIFICATION_PRESET] = preset.name
            it[Keys.NOTIFICATION_ON_MS] = onMs.coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS)
            it[Keys.NOTIFICATION_OFF_MS] = offMs.coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS)
        }
    }
    suspend fun setCallTiming(onMs: Long, offMs: Long) {
        context.dataStore.edit {
            it[Keys.CALL_ON_MS] = onMs.coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS)
            it[Keys.CALL_OFF_MS] = offMs.coerceIn(FlashDefaults.MIN_DURATION_MS, FlashDefaults.MAX_DURATION_MS)
        }
    }
    suspend fun setBrightnessMode(mode: FlashBrightnessMode) { context.dataStore.edit { it[Keys.BRIGHTNESS_MODE] = mode.name } }
    suspend fun setVibrationEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled } }
    suspend fun setBatteryPromptDismissed(dismissed: Boolean) { context.dataStore.edit { it[Keys.BATTERY_PROMPT_DISMISSED] = dismissed } }
}