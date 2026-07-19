package com.example.sysappmodule.module

import com.example.sysappmodule.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 从设备已安装应用中提取 APK 文件（含 split APK）到指定目录。
 *
 * 注意：从 Android 11 起，访问 /data/app/<pkg>/base.apk 通常仍是 world-readable，
 * 但在某些 OEM 上可能受限。本类直接读取 sourceDir 路径，失败时抛出 IOException。
 */
class ApkExtractor {

    /**
     * 把应用的 base.apk 和所有 split APK 复制到 [destDir]。
     *
     * @param app     目标应用
     * @param destDir 目标目录（不存在会创建）
     * @return 复制后的文件列表（base 在第一位）
     */
    suspend fun extractTo(app: AppInfo, destDir: File): List<File> =
        withContext(Dispatchers.IO) {
            if (!destDir.exists() && !destDir.mkdirs()) {
                throw IOException("无法创建目录: ${destDir.absolutePath}")
            }

            val result = mutableListOf<File>()

            // base.apk
            val baseSrc = app.sourceDir
                ?: throw IOException("应用 ${app.packageName} 没有 sourceDir")
            val baseFile = File(baseSrc)
            if (!baseFile.canRead()) {
                throw IOException("无法读取 base.apk: $baseSrc（可能需要 root）")
            }
            val baseDest = File(destDir, "base.apk")
            copyTo(baseFile, baseDest)
            result.add(baseDest)

            // splits
            app.splitSourceDirs?.forEachIndexed { i, splitPath ->
                val splitFile = File(splitPath)
                if (!splitFile.canRead()) {
                    throw IOException("无法读取 split APK: $splitPath")
                }
                // 优先使用 splitNames[i]，否则用源文件名
                val name = app.splitNames?.getOrNull(i)?.let { "$it.apk" }
                    ?: splitFile.name
                val dest = File(destDir, name)
                copyTo(splitFile, dest)
                result.add(dest)
            }

            result
        }

    private fun copyTo(src: File, dest: File) {
        src.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        }
        // 设权限 0644，符合 system app 的预期权限
        dest.setReadable(true, false)
        dest.setWritable(false, false)
        dest.setExecutable(false, false)
    }
}
