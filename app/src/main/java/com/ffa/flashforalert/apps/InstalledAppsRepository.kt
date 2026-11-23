package com.ffa.flashforalert.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.ffa.flashforalert.domain.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {
    suspend fun loadInstalledApps(selectedPackages: Set<String>): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } catch (e: Exception) {
            pm.queryIntentActivities(intent, 0)
        }
        val self = context.packageName
        val byPackage = LinkedHashMap<String, String>()
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == self) continue
            if (byPackage.containsKey(pkg)) continue
            val label = try {
                ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
            } catch (_: Exception) { pkg }
            byPackage[pkg] = label
        }
        byPackage.entries
            .map { (pkg, label) -> InstalledApp(pkg, label, selectedPackages.contains(pkg)) }
            .sortedBy { it.label.lowercase() }
    }

    fun loadIcon(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: Exception) {
        null
    }
}