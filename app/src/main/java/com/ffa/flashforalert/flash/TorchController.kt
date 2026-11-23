package com.ffa.flashforalert.flash

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TorchController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val _torchAvailable = MutableStateFlow(false)
    val torchAvailable: StateFlow<Boolean> = _torchAvailable

    @Volatile
    private var cameraId: String? = null

    val maxStrengthLevel: Int
        get() = cameraId?.let { id ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                }.getOrDefault(1)
            } else 1
        } ?: 1

    val defaultStrengthLevel: Int
        get() = cameraId?.let { id ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
                }.getOrDefault(1)
            } else 1
        } ?: 1

    val supportsStrength: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrengthLevel > 1

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == this@TorchController.cameraId) {
                _torchAvailable.value = false
            }
        }

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == this@TorchController.cameraId || this@TorchController.cameraId == null) {
                if (this@TorchController.cameraId == null) this@TorchController.cameraId = cameraId
                _torchAvailable.value = true
            }
        }
    }

    init {
        cameraId = findFlashCameraId()
        _torchAvailable.value = cameraId != null
        runCatching { cameraManager.registerTorchCallback(torchCallback, null) }
    }

    private fun findFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && (facing == CameraCharacteristics.LENS_FACING_BACK || facing == null)
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to find flash camera", e)
            null
        }
    }

    fun hasFlashUnit(): Boolean = cameraId != null

    fun setTorch(on: Boolean, brightnessMode: FlashBrightnessMode = FlashBrightnessMode.MAX): Boolean {
        val id = cameraId ?: findFlashCameraId()?.also { cameraId = it } ?: return false
        return try {
            if (on) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && supportsStrength) {
                    val level = strengthForMode(brightnessMode)
                    cameraManager.turnOnTorchWithStrengthLevel(id, level)
                } else {
                    cameraManager.setTorchMode(id, true)
                }
            } else {
                cameraManager.setTorchMode(id, false)
            }
            true
        } catch (e: CameraAccessException) {
            Log.w(TAG, "CameraAccessException setting torch=$on", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "IllegalArgumentException setting torch=$on", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException setting torch=$on", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Exception setting torch=$on", e)
            false
        }
    }

    private fun strengthForMode(mode: FlashBrightnessMode): Int {
        val max = maxStrengthLevel.coerceAtLeast(1)
        val def = defaultStrengthLevel.coerceIn(1, max)
        if (max <= 1) return 1
        return when (mode) {
            FlashBrightnessMode.MAX -> max
            FlashBrightnessMode.HIGH -> (max * 0.75f).toInt().coerceIn(1, max)
            FlashBrightnessMode.MEDIUM -> (max * 0.5f).toInt().coerceIn(1, max)
            FlashBrightnessMode.LOW -> (max * 0.25f).toInt().coerceAtLeast(1).coerceAtMost(max)
        }.coerceAtLeast(1)
    }

    fun forceOff() {
        runCatching { setTorch(false) }
    }

    fun release() {
        runCatching { cameraManager.unregisterTorchCallback(torchCallback) }
        forceOff()
    }

    companion object {
        private const val TAG = "TorchController"
    }
}