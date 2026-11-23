package com.ffa.flashforalert.service

import android.content.Context
import com.ffa.flashforalert.data.preferences.FfaPreferencesRepository
import com.ffa.flashforalert.data.preferences.FfaSettings
import com.ffa.flashforalert.flash.FlashEngine
import com.ffa.flashforalert.flash.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

object FfaServices {
    @Volatile
    private var initialized = false
    private lateinit var appContext: Context

    lateinit var preferences: FfaPreferencesRepository
        private set
    lateinit var torchController: TorchController
        private set
    lateinit var flashEngine: FlashEngine
        private set
    lateinit var settingsState: StateFlow<FfaSettings>
        private set

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _notificationAccess = MutableStateFlow(false)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            if (!::preferences.isInitialized) {
                preferences = FfaPreferencesRepository(appContext)
            }
            if (!::torchController.isInitialized) {
                torchController = TorchController(appContext)
            }
            if (!::flashEngine.isInitialized) {
                flashEngine = FlashEngine(appContext, torchController)
            }
            if (!::settingsState.isInitialized) {
                settingsState = runCatching {
                    preferences.settingsFlow.stateIn(appScope, SharingStarted.Eagerly, FfaSettings())
                }.getOrElse {
                    MutableStateFlow(FfaSettings())
                }
            }
            initialized = true
        }
    }

    fun context(): Context = appContext

    fun isInitialized(): Boolean = initialized

    fun updateNotificationAccess(granted: Boolean) {
        _notificationAccess.value = granted
    }

    fun currentSettings(): FfaSettings =
        if (::settingsState.isInitialized) settingsState.value else FfaSettings()
}