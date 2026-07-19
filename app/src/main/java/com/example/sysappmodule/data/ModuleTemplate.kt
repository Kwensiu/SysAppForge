package com.example.sysappmodule.data

import kotlinx.serialization.Serializable

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
 * @param moduleId     模块 ID（写入 module.prop 的 id，只能小写字母数字下划线）
 * @param name         模块名称
 * @param version      版本名（如 v1.0.0）
 * @param versionCode  版本号
 * @param author       作者
 * @param description  描述
 * @param selectedApps 选中的应用（包名 + 安装方式）
 * @param minMagisk    最小 Magisk 版本
 * @param supportUrl   支持链接
 * @param donateUrl    捐赠链接
 * @param createdAt    创建时间戳
 * @param updatedAt    最后修改时间戳
 */
@Serializable
data class ModuleTemplate(
    val id: String,
    val moduleId: String = "my_module",
    val name: String = "我的模块",
    val version: String = "v1.0.0",
    val versionCode: Long = 1L,
    val author: String = "",
    val description: String = "",
    val selectedApps: List<SelectedPackage> = emptyList(),
    val minMagisk: String = "",
    val supportUrl: String = "",
    val donateUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
