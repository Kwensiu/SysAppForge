package com.example.sysappmodule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sysappmodule.vm.SettingsViewModel
import com.example.sysappmodule.vm.TemplateDetailViewModel
import com.example.sysappmodule.vm.TemplateListViewModel

object Routes {
    const val TEMPLATE_LIST = "template_list"
    const val SETTINGS = "settings"
    const val TEMPLATE_DETAIL = "template_detail/{templateId}"
    const val PREVIEW = "preview/{templateId}"

    fun templateDetail(id: String) = "template_detail/$id"
    fun preview(id: String) = "preview/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.TEMPLATE_LIST) {
        composable(Routes.TEMPLATE_LIST) {
            val vm: TemplateListViewModel = viewModel()
            val templates by vm.templates.collectAsStateWithLifecycle()
            val event by vm.events.collectAsStateWithLifecycle()

            TemplatesScreen(
                templates = templates,
                onNewTemplate = { vm.createTemplate() },
                onOpenTemplate = { id -> navController.navigate(Routes.templateDetail(id)) },
                onDeleteTemplate = { id -> vm.deleteTemplate(id) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )

            when (event) {
                is TemplateListViewModel.TemplateListEvent.NavigateToDetail -> {
                    val id = (event as TemplateListViewModel.TemplateListEvent.NavigateToDetail).templateId
                    navController.navigate(Routes.templateDetail(id))
                    vm.consumeEvent()
                }
                null -> Unit
            }
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                themeMode = themeMode,
                onThemeChange = vm::setThemeMode,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEMPLATE_DETAIL) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId").orEmpty()
            val vm: TemplateDetailViewModel = viewModel()
            val template by vm.template.collectAsStateWithLifecycle()
            val allApps by vm.allApps.collectAsStateWithLifecycle()
            val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
            val filter by vm.filter.collectAsStateWithLifecycle()
            val showSystem by vm.showSystem.collectAsStateWithLifecycle()
            val metadataExpanded by vm.metadataExpanded.collectAsStateWithLifecycle()
            val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
            val event by vm.events.collectAsStateWithLifecycle()

            TemplateDetailScreen(
                template = template,
                allApps = allApps,
                searchQuery = searchQuery,
                filter = filter,
                showSystem = showSystem,
                metadataExpanded = metadataExpanded,
                isGenerating = isGenerating,
                onSearch = vm::updateSearchQuery,
                onFilter = vm::updateFilter,
                onToggleShowSystem = vm::toggleShowSystem,
                onToggleMetadataExpanded = vm::toggleMetadataExpanded,
                onUpdateTemplate = vm::updateTemplate,
                onToggleApp = vm::toggleApp,
                onSetInstallMode = vm::setInstallMode,
                onDeleteTemplate = { vm.deleteTemplate(); navController.popBackStack() },
                onNext = { navController.navigate(Routes.preview(templateId)) },
                onBack = { navController.popBackStack() }
            )

            // 处理事件
            androidx.compose.runtime.LaunchedEffect(Unit) {
                vm.events.collect { ev ->
                    when (ev) {
                        is TemplateDetailViewModel.DetailEvent.Deleted -> navController.popBackStack()
                        else -> Unit
                    }
                }
            }
        }

        composable(Routes.PREVIEW) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId").orEmpty()
            val vm: TemplateDetailViewModel = viewModel()
            val template by vm.template.collectAsStateWithLifecycle()
            val allApps by vm.allApps.collectAsStateWithLifecycle()
            val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
            val event by vm.events.collectAsStateWithLifecycle()

            PreviewScreen(
                template = template,
                allApps = allApps,
                isGenerating = isGenerating,
                onBack = { navController.popBackStack() },
                onGenerate = vm::generateModule
            )

            androidx.compose.runtime.LaunchedEffect(Unit) {
                vm.events.collect { ev ->
                    when (ev) {
                        is TemplateDetailViewModel.DetailEvent.ModuleGenerated -> {
                            // 生成完成后留在预览页，对话框由预览页自己展示
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
