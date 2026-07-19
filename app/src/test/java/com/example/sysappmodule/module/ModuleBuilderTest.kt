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
            val output = File(root, "module.zip")
            val config = ModuleConfig(
                id = "test_module",
                name = "Name\nmalicious=value $(touch /data/local/tmp/pwned) 'quoted'",
                version = "v1\ninvalid=value",
                author = "Author\r\nInjected=1",
                description = "Description",
                selectedApps = listOf(
                    SelectedApp(testApp("com.example.app"), InstallMode.INSTALL_EXISTING)
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
    fun build_installExistingProducesScriptsWithoutApkPayload() = runBlocking {
        val root = createTempDirectory("module-builder-test-").toFile()
        try {
            val output = File(root, "module.zip")
            val result = ModuleBuilder().build(
                ModuleConfig(
                    id = "test_module",
                    selectedApps = listOf(
                        SelectedApp(testApp("com.example.app"), InstallMode.INSTALL_EXISTING)
                    )
                ),
                File(root, "work"),
                output
            )

            assertEquals(1, result.packageCount)
            assertEquals(0L, result.totalApkBytes)
            ZipFile(output).use { zip ->
                assertTrue(zip.readText("service.sh").contains("pm install-existing \"com.example.app\""))
                assertFalse(zip.entries().asSequence().any { it.name.endsWith(".apk") })
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
