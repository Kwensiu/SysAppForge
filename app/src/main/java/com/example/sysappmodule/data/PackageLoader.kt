package com.example.sysappmodule.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从 PackageManager 加载已安装应用列表。
 *
 * 注意：Android 11+ 需要 QUERY_ALL_PACKAGES 权限才能获取完整列表。
 */
class PackageLoader(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    suspend fun loadInstalledApps(includeSystem: Boolean = true): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val flags = PackageManager.GET_META_DATA
            @Suppress("DEPRECATION")
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getInstalledPackages(flags)
            }

            packages.mapNotNull { pi ->
                val ai = pi.applicationInfo ?: return@mapNotNull null
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystem && isSystem && (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    return@mapNotNull null
                }
                val isUpdatedSystem = (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode
                } else {
                    @Suppress("DEPRECATION") pi.versionCode.toLong()
                }
                AppInfo(
                    packageName = pi.packageName,
                    label = pm.getApplicationLabel(ai).toString(),
                    versionName = pi.versionName,
                    versionCode = versionCode,
                    isSystem = isSystem,
                    isUpdatedSystem = isUpdatedSystem,
                    sourceDir = ai.sourceDir,
                    splitSourceDirs = ai.splitSourceDirs,
                    splitNames = ai.splitNames,
                    icon = safeLoadIcon(ai),
                    enabled = ai.enabled,
                    firstInstallTime = pi.firstInstallTime,
                    lastUpdateTime = pi.lastUpdateTime
                )
            }.sortedBy { it.label.lowercase() }
        }

    private fun safeLoadIcon(ai: ApplicationInfo): Drawable? = try {
        pm.getApplicationIcon(ai)
    } catch (t: Throwable) {
        null
    }
}
