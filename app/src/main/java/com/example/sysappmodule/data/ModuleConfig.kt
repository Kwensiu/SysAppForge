package com.example.sysappmodule.data

import androidx.compose.runtime.Immutable

/**
 * 模块元数据。对应 module.prop。
 *
 * @param id          模块 ID（只能小写字母、数字、下划线）
 * @param name        模块名称
 * @param version     版本名（如 v1.0.0）
 * @param versionCode 版本号（整数）
 * @param author      作者
 * @param description 描述
 * @param selectedApps 选中应用及其安装方式
 */
@Immutable
data class ModuleConfig(
    val id: String = "sys_app_module",
    val name: String = "我的系统应用模块",
    val version: String = "v1.0.0",
    val versionCode: Long = 1L,
    val author: String = "SysApp Module Builder",
    val description: String = "把选定应用固定为系统应用",
    val selectedApps: List<SelectedApp> = emptyList(),
)

/**
 * 一个被选中的应用与其选择的安装方式。
 */
@Immutable
data class SelectedApp(
    val app: AppInfo,
    val mode: InstallMode = InstallMode.PRIV_APP
)
