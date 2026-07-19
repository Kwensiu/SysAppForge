package com.example.sysappmodule.module

import com.example.sysappmodule.data.AppInfo
import com.example.sysappmodule.data.InstallMode
import com.example.sysappmodule.data.ModuleConfig
import com.example.sysappmodule.data.SelectedApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory

class ModuleBuilderTest {

    @Test
    fun build_sanitizesLineOrientedMetadataAndShellQuotesUserText() = runBlocking {
        val root = createTempDirectory("module-builder-test-").toFile()
        try {
            val sourceApk = File(root, "source.apk").apply { writeText("test apk") }
            val output = File(root, "module.zip")
            val config = ModuleConfig(
                id = "test_module",
                name = "Name\nmalicious=value $(touch /data/local/tmp/pwned) 'quoted'",
                version = "v1\ninvalid=value",
                author = "Author\r\nInjected=1",
                description = "Description",
                selectedApps = listOf(
                    SelectedApp(
                        testApp("com.example.app").copy(sourceDir = sourceApk.absolutePath),
                        InstallMode.SYSTEM_APP
                    )
                )
            )

            ModuleBuilder().build(config, File(root, "work"), output)

            ZipFile(output).use { zip ->
                val moduleProp = zip.readText("module.prop")
                assertFalse(moduleProp.lines().any { it.startsWith("malicious=") })
                assertFalse(moduleProp.lines().any { it.startsWith("invalid=") })
                assertFalse(moduleProp.lines().any { it.startsWith("Injected=") })

                val customize = zip.readText("customize.sh")
                assertTrue(
                    customize.contains(
                        "ui_print '- 安装模块: Name malicious=value " +
                            "\$(touch /data/local/tmp/pwned) '\\''quoted'\\'''"
                    )
                )
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun build_overlayPreservesUserInstallAndNeedsNoLifecycleScripts() = runBlocking {
        val root = createTempDirectory("module-builder-test-").toFile()
        try {
            val sourceApk = File(root, "source.apk").apply { writeText("test apk") }
            val privilegedApk = File(root, "privileged.apk").apply { writeText("privileged apk") }
            val arm64Split = File(root, "split-arm64.apk").apply { writeText("arm64 split") }
            val densitySplit = File(root, "split-xxhdpi.apk").apply { writeText("density split") }
            val output = File(root, "module.zip")
            ModuleBuilder().build(
                ModuleConfig(
                    id = "test_module",
                    selectedApps = listOf(
                        SelectedApp(
                            testApp("com.example.app").copy(
                                sourceDir = sourceApk.absolutePath,
                                splitSourceDirs = arrayOf(
                                    arm64Split.absolutePath,
                                    densitySplit.absolutePath
                                ),
                                splitNames = arrayOf("config.arm64_v8a", "config.xxhdpi")
                            ),
                            InstallMode.SYSTEM_APP
                        ),
                        SelectedApp(
                            testApp("com.example.privileged").copy(
                                sourceDir = privilegedApk.absolutePath
                            ),
                            InstallMode.PRIV_APP
                        )
                    )
                ),
                File(root, "work"),
                output
            )

            ZipFile(output).use { zip ->
                val entries = zip.entries().asSequence().map { it.name }.toSet()
                assertFalse("service.sh" in entries)
                assertFalse("post-fs-data.sh" in entries)
                assertFalse("uninstall.sh" in entries)
                assertTrue(zip.entries().asSequence().any {
                    it.name == "system/app/com.example.app/base.apk"
                })
                assertEquals(
                    "privileged apk",
                    zip.readText("system/priv-app/com.example.privileged/base.apk")
                )
                assertEquals(
                    "arm64 split",
                    zip.readText("system/app/com.example.app/config.arm64_v8a.apk")
                )
                assertEquals(
                    "density split",
                    zip.readText("system/app/com.example.app/config.xxhdpi.apk")
                )
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun testApp(packageName: String) = AppInfo(
        packageName = packageName,
        label = "Test app",
        versionName = "1.0",
        versionCode = 1,
        isSystem = false,
        isUpdatedSystem = false,
        sourceDir = null,
        splitSourceDirs = null,
        splitNames = null,
        enabled = true,
        firstInstallTime = 0,
        lastUpdateTime = 0
    )

    private fun ZipFile.readText(name: String): String =
        getInputStream(getEntry(name)).bufferedReader().use { it.readText() }
}
