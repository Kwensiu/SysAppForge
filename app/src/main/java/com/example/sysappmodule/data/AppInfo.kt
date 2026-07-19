package com.example.sysappmodule.data

import android.graphics.drawable.Drawable

/**
 * 已安装应用的轻量信息。
 *
 * @param packageName 包名
 * @param label       显示名
 * @param versionName 版本名
 * @param versionCode 版本号
 * @param isSystem    是否系统应用（FLAG_SYSTEM）
 * @param isUpdatedSystem 是否是被更新过的系统应用
 * @param sourceDir   base.apk 路径
 * @param splitSourceDirs split APK 路径数组，可能为 null
 * @param splitNames  split 名称数组，与 splitSourceDirs 一一对应
 * @param icon        应用图标 Drawable
 * @param enabled     是否启用
 * @param firstInstallTime 首次安装时间
 * @param lastUpdateTime    最后更新时间
 */
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
    val icon: Drawable?,
    val enabled: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long
) {
    val splitCount: Int get() = splitSourceDirs?.size ?: 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppInfo) return false
        return packageName == other.packageName
    }

    override fun hashCode(): Int = packageName.hashCode()
}
