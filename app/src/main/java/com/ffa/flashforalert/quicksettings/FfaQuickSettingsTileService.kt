package com.ffa.flashforalert.quicksettings

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.ffa.flashforalert.core.permissions.PermissionUtils
import com.ffa.flashforalert.service.FfaServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FfaQuickSettingsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        ensureInit()
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        ensureInit()
        val settings = FfaServices.currentSettings()
        val hasAccess = PermissionUtils.isNotificationListenerEnabled(this)
        FfaServices.updateNotificationAccess(hasAccess)
        val hasFlash = FfaServices.torchController.hasFlashUnit()

        if (!settings.ffaEnabled) {
            // Trying to enable
            if (!hasAccess || !hasFlash) {
                // Direct user to app/setup; TileService can start activity on unlock
                val intent = android.content.Intent(this, com.ffa.flashforalert.MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("open_setup", true)
                }
                try {
                    unlockAndRun { startActivityAndCollapse(intent) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open setup", e)
                }
                updateTile()
                return
            }
            scope.launch {
                FfaServices.preferences.setFfaEnabled(true)
                updateTile()
            }
        } else {
            // Disable
            scope.launch {
                FfaServices.preferences.setFfaEnabled(false)
                FfaServices.flashEngine.cancel()
                updateTile()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureInit() {
        if (!FfaServices.isInitialized()) FfaServices.init(applicationContext)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (!FfaServices.isInitialized()) {
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }
        val settings = FfaServices.currentSettings()
        val hasAccess = PermissionUtils.isNotificationListenerEnabled(this)
        val hasFlash = FfaServices.torchController.hasFlashUnit()
        val active = settings.ffaEnabled && hasAccess && hasFlash
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "FFA"
        tile.subtitle = when {
            !hasFlash -> "No flash"
            !hasAccess -> "Access needed"
            settings.ffaEnabled && settings.selectedPackages.isEmpty() -> "No apps"
            settings.ffaEnabled -> "Active"
            else -> "Off"
        }
        tile.contentDescription = tile.subtitle
        tile.updateTile()
    }

    companion object {
        private const val TAG = "FfaTileService"
    }
}