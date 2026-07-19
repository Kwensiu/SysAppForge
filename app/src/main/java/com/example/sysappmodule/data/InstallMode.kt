package com.example.sysappmodule.data

/**
 * 模块中应用的安装方式。
 *
 * - PRIV_APP:        通过 overlay 把 APK 放到 /system/priv-app/<pkg>/，应用获得特权系统应用身份
 *                    （可申请 signature|privileged 权限，如 INSTALL_PACKAGES、WRITE_SECURE_SETTINGS）
 * - SYSTEM_APP:      通过 overlay 把 APK 放到 /system/app/<pkg>/，应用获得普通系统应用身份
 *                    （仅可申请 system 权限，无 privileged 权限）
 * - INSTALL_EXISTING:在 post-fs-data.sh 中调用 pm install-existing <pkg>
 *                    （不会改 /system 结构，需应用包体已存在于 /data/app，常用于跨用户安装）
 */
enum class InstallMode(val directory: String, val descriptionRes: Int) {
    PRIV_APP("system/priv-app", com.example.sysappmodule.R.string.desc_priv_app),
    SYSTEM_APP("system/app", com.example.sysappmodule.R.string.desc_system_app),
    INSTALL_EXISTING("", com.example.sysappmodule.R.string.desc_install_existing);

    /** 转换为模块内 system 子目录的目录名（用于 overlay）。INSTALL_EXISTING 返回 null。 */
    fun overlayFolder(pkg: String): String? = when (this) {
        PRIV_APP -> "system/priv-app/$pkg"
        SYSTEM_APP -> "system/app/$pkg"
        INSTALL_EXISTING -> null
    }
}
