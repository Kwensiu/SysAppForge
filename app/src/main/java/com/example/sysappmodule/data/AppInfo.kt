package com.example.sysappmodule.data

import java.util.Arrays

data class AppInfo(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isUpdatedSystem: Boolean,
    val sourceDir: String?,
    val splitSourceDirs: Array<String>?,
    val splitNames: Array<String>?,
    val enabled: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long
) {
    val splitCount: Int get() = splitSourceDirs?.size ?: 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppInfo) return false
        return packageName == other.packageName &&
            Arrays.equals(splitSourceDirs, other.splitSourceDirs) &&
            Arrays.equals(splitNames, other.splitNames)
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + (splitSourceDirs?.contentHashCode() ?: 0)
        result = 31 * result + (splitNames?.contentHashCode() ?: 0)
        return result
    }
}
