package com.ffa.flashforalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ffa.flashforalert.core.permissions.PermissionUtils
import com.ffa.flashforalert.service.FfaServices
import com.ffa.flashforalert.ui.FfaViewModel
import com.ffa.flashforalert.ui.navigation.FfaNav
import com.ffa.flashforalert.ui.theme.FfaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FfaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (!FfaServices.isInitialized()) FfaServices.init(applicationContext)
        } catch (_: Exception) { }
        enableEdgeToEdge()
        setContent {
            FfaTheme(darkTheme = true) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val openSetup = intent?.getBooleanExtra("open_setup", false) == true
                val needsSetup = try {
                    !PermissionUtils.isNotificationListenerEnabled(this)
                } catch (_: Exception) { true }
                FfaNav(
                    uiState = uiState,
                    viewModel = viewModel,
                    startInSetup = openSetup || needsSetup
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()
        viewModel.reloadApps()
    }
}