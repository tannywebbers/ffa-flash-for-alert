package com.ffa.flashforalert.flash

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.domain.models.FlashPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

sealed interface FlashCommand {
    data class Play(
        val pattern: FlashPattern,
        val brightnessMode: FlashBrightnessMode,
        val vibrate: Boolean
    ) : FlashCommand
    data object Cancel : FlashCommand
    data object Shutdown : FlashCommand
}

class FlashEngine(
    private val context: Context,
    private val torchController: TorchController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()
    private var currentJob: Job? = null

    fun play(pattern: FlashPattern, brightnessMode: FlashBrightnessMode, vibrate: Boolean) {
        scope.launch {
            commandMutex.withLock {
                currentJob?.cancel()
                currentJob = null
                torchController.forceOff()
                currentJob = scope.launch {
                    runPattern(pattern, brightnessMode, vibrate)
                }
            }
        }
    }

    fun cancel() {
        scope.launch {
            commandMutex.withLock {
                currentJob?.cancel()
                currentJob = null
                torchController.forceOff()
            }
        }
    }

    private suspend fun runPattern(pattern: FlashPattern, brightnessMode: FlashBrightnessMode, vibrate: Boolean) {
        try {
            if (vibrate) vibrateOnce()
            if (pattern.initialOffMs > 0) {
                preciseDelay(pattern.initialOffMs)
            }
            repeat(pattern.pulses.coerceIn(FlashDefaults.MIN_PULSES, FlashDefaults.MAX_PULSES)) { index ->
                if (!kotlin.coroutines.coroutineContext.isActive) return
                torchController.setTorch(true, brightnessMode)
                preciseDelay(pattern.onMs)
                torchController.forceOff()
                if (index < pattern.pulses - 1) {
                    preciseDelay(pattern.offMs)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Flash pattern failed", e)
        } finally {
            torchController.forceOff()
        }
    }

    private suspend fun preciseDelay(durationMs: Long) {
        val target = SystemClock.elapsedRealtime() + durationMs.coerceAtLeast(0)
        while (true) {
            val remaining = target - SystemClock.elapsedRealtime()
            if (remaining <= 0) break
            delay(max(1L, remaining))
        }
    }

    private fun vibrateOnce() {
        try {
            val effect = VibrationEffect.createOneShot(FlashDefaults.VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    fun shutdown() {
        scope.launch {
            commandMutex.withLock {
                currentJob?.cancel()
                currentJob = null
            }
            torchController.forceOff()
        }
    }

    companion object {
        private const val TAG = "FlashEngine"
    }
}