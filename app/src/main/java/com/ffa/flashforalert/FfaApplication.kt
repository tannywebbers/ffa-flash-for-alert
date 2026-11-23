package com.ffa.flashforalert

import android.app.Application
import android.os.Build
import android.util.Log
import com.ffa.flashforalert.service.FfaServices
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FfaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        writeStartupMarker()
        try {
            FfaServices.init(this)
        } catch (e: Exception) {
            Log.e("FFA", "Service init failed", e)
            writeCrashReport("service-init", e)
        }
    }

    private fun writeStartupMarker() {
        runCatching {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
            val text = "FFA started at $timestamp\npackage=${packageName}\nversion=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n"
            crashDirs().forEach { dir ->
                if (!dir.exists()) dir.mkdirs()
                File(dir, "app-started.txt").writeText(text)
            }
        }
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                writeCrashReport("uncaught-${t.name}", e)
            } catch (_: Exception) { }
            previous?.uncaughtException(t, e)
        }
    }

    private fun writeCrashReport(source: String, e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
            val report = buildString {
                appendLine("FFA Crash Report")
                appendLine("timestamp: $timestamp")
                appendLine("source: $source")
                appendLine("package: ${packageName}")
                appendLine("versionName: ${BuildConfig.VERSION_NAME}")
                appendLine("versionCode: ${BuildConfig.VERSION_CODE}")
                appendLine("android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine()
                appendLine(sw.toString())
            }

            val fileName = "crash-$timestamp.txt"
            crashDirs().forEach { dir ->
                runCatching {
                    if (!dir.exists()) dir.mkdirs()
                    File(dir, fileName).writeText(report)
                    File(dir, "latest-crash.txt").writeText(report)
                }
            }
            Log.e("FFA_CRASH", report)
        } catch (_: Exception) { }
    }

    private fun crashDirs(): List<File> {
        val dirs = mutableListOf<File>()
        // App-specific external: Android/data/com.ffa.flashforalert/files/crash-reports/
        runCatching { getExternalFilesDir(null) }.getOrNull()?.let { ext ->
            dirs.add(File(ext, "crash-reports"))
            // Explicit package-name folder as requested
            dirs.add(File(ext, "com.ffa.flashforalert/crash-reports"))
        }
        // Internal fallbacks
        dirs.add(File(filesDir, "crash-reports"))
        dirs.add(File(cacheDir, "crash-reports"))
        return dirs.distinctBy { it.absolutePath }
    }
}