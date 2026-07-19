package com.example.sysappmodule.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.sysappmodule.BuildConfig
import java.io.File
import java.io.IOException

/**
 * 把生成的模块 zip 保存到 Download 目录，并提供「打开安装」「分享」Intent。
 *
 * Android 10+ 使用 MediaStore.Downloads；旧版直接写 Environment.DIRECTORY_DOWNLOADS。
 */
class FileSaver(private val context: Context) {

    /**
     * 把 [src] 保存到公共 Download 目录下名为 [displayName] 的文件。
     * @return 保存后的 Uri（用于打开/分享）
     */
    fun saveToDownloads(src: File, displayName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(src, displayName)
        } else {
            saveLegacy(src, displayName)
        }
    }

    private fun saveViaMediaStore(src: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MagicModules")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        var uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore 插入失败")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("无法打开输出流")
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }

        // 完成
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(src: File, displayName: String): Uri {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val moduleDir = File(downloads, "MagicModules").apply { mkdirs() }
        val dest = File(moduleDir, displayName)
        src.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(dest)
    }

    /**
     * 获取「打开方式」Intent，让用户选择 Magisk/KSU 管理器安装此 zip。
     *
     * 优先使用 FileProvider 共享缓存目录中的 zip（避免权限问题）。
     */
    fun buildOpenInstallIntent(zipFile: File): android.content.Intent {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, zipFile)
        } else {
            Uri.fromFile(zipFile)
        }
        return android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/zip")
            addFlags(
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
    }

    /**
     * 分享 Intent。
     */
    fun buildShareIntent(zipFile: File): android.content.Intent {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, zipFile)
        } else {
            Uri.fromFile(zipFile)
        }
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
    }
}
