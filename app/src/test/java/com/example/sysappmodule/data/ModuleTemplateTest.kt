package com.example.sysappmodule.data

import com.example.sysappmodule.vm.isMetadataValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleTemplateTest {

    @Test
    fun defaultModuleIdUsesStableEightCharacterHash() {
        val templateId = "template-id"

        assertEquals("sysapp_f845a15c", ModuleTemplate.defaultModuleId(templateId))
        assertTrue(ModuleTemplate.defaultModuleId(templateId).matches(Regex("sysapp_[0-9a-f]{8}")))
    }

    @Test
    fun displayNameFallsBackForTemplatesSavedBeforeNamesWereSeparated() {
        assertEquals("旧模块名称", ModuleTemplate(id = "old", name = "旧模块名称").displayName)
        assertEquals(
            "首页模板名称",
            ModuleTemplate(
                id = "new",
                templateName = "首页模板名称",
                name = "module.prop 名称"
            ).displayName
        )
    }

    @Test
    fun metadataValidationRequiresBothNamesAndValidModuleIdentity() {
        val valid = ModuleTemplate(
            id = "id",
            templateName = "常用应用",
            moduleId = "sysapp_1234abcd",
            name = "常用系统应用",
            version = "v1.0.0",
            versionCode = 1
        )

        assertTrue(valid.isMetadataValid())
        assertFalse(valid.copy(templateName = " ").isMetadataValid())
        assertFalse(valid.copy(name = " ").isMetadataValid())
        assertFalse(valid.copy(moduleId = "123-invalid").isMetadataValid())
        assertFalse(valid.copy(versionCode = 0).isMetadataValid())
    }
}
