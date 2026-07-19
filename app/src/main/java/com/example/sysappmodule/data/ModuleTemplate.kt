package com.example.sysappmodule.data

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.UUID

/**
 * 一个被选中的应用与其选择的安装方式。
 * 持久化时只存包名 + 模式；运行时通过 PackageManager 反查 AppInfo。
 */
@Serializable
data class SelectedPackage(
    val packageName: String,
    val mode: InstallMode = InstallMode.PRIV_APP
)

/**
 * 模块模板。用户可以在首页创建多张模板卡片，每张卡片是一个独立的模块配置。
 * 持久化到 DataStore。
 *
 * @param id           模板 UUID（运行时生成）
 * @param templateName 模板显示名称，仅用于在应用内识别模板
 * @param moduleId     模块 ID（写入 module.prop 的 id，只能小写字母数字下划线）
 * @param name         模块名称（写入 module.prop）
 * @param version      版本名（如 v1.0.0）
 * @param versionCode  版本号
 * @param author       作者
 * @param description  描述
 * @param selectedApps 选中的应用（包名 + 安装方式）
 * @param createdAt    创建时间戳
 * @param updatedAt    最后修改时间戳
 */
@Serializable
data class ModuleTemplate(
    val id: String,
    val templateName: String = "",
    val moduleId: String = "my_module",
    val name: String = "我的模块",
    val version: String = "v1.0.0",
    val versionCode: Long = 1L,
    val author: String = "",
    val description: String = "",
    val selectedApps: List<SelectedPackage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Older templates did not have a separate display name; keep them readable without migration. */
    val displayName: String
        get() = templateName.ifBlank { name }

    companion object {
        fun createDraft(): ModuleTemplate {
            val id = UUID.randomUUID().toString()
            return ModuleTemplate(
                id = id,
                moduleId = defaultModuleId(id),
                name = "我的系统应用模块",
                version = "v1.0.0",
                versionCode = 1L,
                description = "把选定应用固定为系统应用"
            )
        }

        internal fun defaultModuleId(templateId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(templateId.toByteArray(Charsets.UTF_8))
            val suffix = digest.take(4).joinToString("") { "%02x".format(it) }
            return "sysapp_$suffix"
        }
    }
}
