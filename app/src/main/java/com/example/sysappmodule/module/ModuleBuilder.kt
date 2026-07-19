package com.example.sysappmodule.module

import com.example.sysappmodule.data.AppInfo
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleConfig
import com.example.sysappmodule.data.SelectedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 生成 Magisk / KSU 双格式兼容模块。
 *
 * 输出 zip 内部结构示例（PRIV_APP 模式）：
 * ```
 * module.prop
 * customize.sh
 * system/
 *   priv-app/<pkg>/
 *     base.apk
 *     split_config.arm64_v8a.apk
 *     ...
 * ```
 *
 * INSTALL_EXISTING 模式不带 APK，并额外生成 service.sh 调用 pm install-existing。
 */
class ModuleBuilder(
    private val apkExtractor: ApkExtractor = ApkExtractor()
) {

    /** 生成结果 */
    data class Result(val zipFile: File, val packageCount: Int, val totalApkBytes: Long)

    /**
     * 构建 Magisk/KSU 模块 zip。
     *
     * @param config   模块配置
     * @param workDir  临时工作目录（构建完成后会被清空）
     * @param outputZip 最终 zip 输出路径
     */
    suspend fun build(
        config: ModuleConfig,
        workDir: File,
        outputZip: File
    ): Result = withContext(Dispatchers.IO) {
        require(config.selectedApps.isNotEmpty()) { "未选择任何应用" }
        require(MODULE_ID_REGEX.matches(config.id)) { "模块 ID 非法: ${config.id}" }

        // 清理工作目录
        workDir.deleteRecursively()
        workDir.mkdirs()
        outputZip.parentFile?.mkdirs()
        if (outputZip.exists()) outputZip.delete()

        var totalBytes = 0L
        val installExistingPackages = mutableListOf<String>()
        val overlayPackages = mutableListOf<Pair<AppInfo, InstallMode>>()

        // 1. 复制 APK 到 system/priv-app/<pkg>/ 或 system/app/<pkg>/
        config.selectedApps.forEach { selected ->
            when (selected.mode) {
                InstallMode.INSTALL_EXISTING -> {
                    installExistingPackages += selected.app.packageName
                }
                InstallMode.PRIV_APP,
                InstallMode.SYSTEM_APP -> {
                    val folder = selected.mode.overlayFolder(selected.app.packageName)
                        ?: return@forEach
                    val destDir = File(workDir, folder)
                    val files = try {
                        apkExtractor.extractTo(selected.app, destDir)
                    } catch (e: IOException) {
                        throw IOException("提取 ${selected.app.packageName} APK 失败: ${e.message}", e)
                    }
                    files.forEach { totalBytes += it.length() }
                    overlayPackages += selected.app to selected.mode
                }
            }
        }

        // 2. 写 module.prop
        File(workDir, "module.prop").writeText(buildModuleProp(config))

        // 3. 写 customize.sh
        File(workDir, "customize.sh").writeText(
            buildCustomizeSh(config, overlayPackages, installExistingPackages)
        )

        // install-existing 需要等待 PackageManager 就绪；纯 overlay 模块不需要启动脚本。
        if (installExistingPackages.isNotEmpty()) {
            File(workDir, "service.sh").writeText(buildServiceSh(installExistingPackages))
        }

        // 4. 设置脚本可执行权限（zip 不保留 unix 权限，所以这部分在 customize.sh 里 set_perm）
        // 5. 打包 zip
        zipDirectory(workDir, outputZip)

        // 6. 清理工作目录
        workDir.deleteRecursively()

        Result(
            zipFile = outputZip,
            packageCount = config.selectedApps.size,
            totalApkBytes = totalBytes
        )
    }

    private fun buildModuleProp(config: ModuleConfig): String = buildString {
        append("id=").append(config.id).append('\n')
        append("name=").append(modulePropValue(config.name)).append('\n')
        append("version=").append(modulePropValue(config.version)).append('\n')
        append("versionCode=").append(config.versionCode).append('\n')
        append("author=").append(modulePropValue(config.author)).append('\n')
        append("description=").append(modulePropValue(config.description)).append('\n')
        if (config.minMagisk.isNotBlank()) {
            append("minMagisk=").append(modulePropValue(config.minMagisk)).append('\n')
        }
        if (config.supportUrl.isNotBlank()) {
            append("support=").append(modulePropValue(config.supportUrl)).append('\n')
        }
        if (config.donateUrl.isNotBlank()) {
            append("donate=").append(modulePropValue(config.donateUrl)).append('\n')
        }
        // KSU 支持读这个字段，标识模块需要 overlay 生效
        append("replace=").append('\n')
    }

    /**
     * customize.sh 在模块被模块管理器安装时执行（root），
     * 主要用于设置文件权限和打印安装日志。
     */
    private fun buildCustomizeSh(
        config: ModuleConfig,
        overlayPackages: List<Pair<AppInfo, InstallMode>>,
        installExisting: List<String>
    ): String = buildString {
        appendLine("#!/system/bin/sh")
        appendLine("# customize.sh - 由 SysApp Module Builder 自动生成")
        appendLine("# 模块: ${modulePropValue(config.name)} (${config.id}) ${modulePropValue(config.version)}")
        appendLine("# 此脚本在模块安装时执行，设置文件权限")
        appendLine()
        appendLine("ui_print ${shellQuote(modulePropValue("- 安装模块: ${config.name}"))}")
        appendLine("ui_print \"- 包含 ${overlayPackages.size} 个 overlay 应用, ${installExisting.size} 个 install-existing 应用\"")
        appendLine()

        if (overlayPackages.isNotEmpty()) {
            appendLine("# 设置 APK 文件权限为 0644，目录权限为 0755")
            overlayPackages.forEach { (app, mode) ->
                val folder = mode.overlayFolder(app.packageName) ?: return@forEach
                appendLine("set_perm_recursive \"\$MODPATH/$folder\" 0 0 0755 0644")
            }
            appendLine()
        }

        appendLine("# 设置脚本权限")
        if (installExisting.isNotEmpty()) {
            appendLine("set_perm \"\$MODPATH/service.sh\" 0 0 0755")
        }
        appendLine()
        appendLine("ui_print \"- 安装完成，重启后生效\"")
    }

    /**
     * service.sh 在 late_start 服务阶段、Zygote 启动后执行。
     * 此时 PackageManager 已就绪，可以调用 pm。
     * 纯 overlay 模式保留原 /data/app，由 PackageManager 将其识别为系统应用更新；
     * 因此只有 install-existing 模式需要此脚本。
     */
    private fun buildServiceSh(installExisting: List<String>): String = buildString {
        appendLine("#!/system/bin/sh")
        appendLine("# service.sh - 由 SysApp Module Builder 自动生成")
        appendLine("# 在 late_start 阶段、PackageManager 就绪后执行")
        appendLine()
        appendLine("# 等待开机完成")
        appendLine("while [ \"\$(getprop sys.boot_completed)\" != \"1\" ]; do")
        appendLine("    sleep 2")
        appendLine("done")
        appendLine("sleep 3")
        appendLine()

        appendLine("# === install-existing 模式：恢复已卸载应用 ===")
        installExisting.forEach { pkg ->
            appendLine("pm install-existing --user 0 \"$pkg\" >/dev/null 2>&1 || true")
        }
        appendLine()

        appendLine("# 完成")
    }

    private fun zipDirectory(rootDir: File, outputZip: File) {
        ZipOutputStream(BufferedOutputStream(outputZip.outputStream())).use { zos ->
            rootDir.walkTopDown().forEach { file ->
                if (file == rootDir) return@forEach
                val relPath = file.relativeTo(rootDir).path.replace(File.separatorChar, '/')
                if (file.isDirectory) {
                    // 跳过空目录条目（Magisk 不需要）
                    return@forEach
                }
                val entry = ZipEntry(relPath)
                entry.time = file.lastModified()
                zos.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /** module.prop is line-oriented; embedded newlines would create attacker-controlled keys. */
    private fun modulePropValue(value: String): String =
        value.replace('\r', ' ').replace('\n', ' ')

    /** Quote user-authored text for Android's POSIX-compatible /system/bin/sh. */
    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    companion object {
        val MODULE_ID_REGEX = Regex("^[a-z][a-z0-9_]*$")
    }
}
