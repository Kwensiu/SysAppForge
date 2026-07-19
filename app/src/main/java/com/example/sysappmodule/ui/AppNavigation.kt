package com.example.sysappmodule.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sysappmodule.vm.SettingsViewModel
import com.example.sysappmodule.vm.TemplateDetailViewModel
import com.example.sysappmodule.vm.TemplateEditorViewModel
import com.example.sysappmodule.vm.TemplateListViewModel

object Routes {
    const val TEMPLATE_LIST = "template_list"
    const val SETTINGS = "settings"
    const val TEMPLATE_CREATE = "template_create"
    const val TEMPLATE_EDIT = "template_edit/{templateId}"
    const val TEMPLATE_DETAIL = "template_detail/{templateId}"
    const val PREVIEW = "preview/{templateId}"

    fun templateEdit(id: String) = "template_edit/$id"
    fun templateDetail(id: String) = "template_detail/$id"
    fun preview(id: String) = "preview/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.TEMPLATE_LIST,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(Routes.TEMPLATE_LIST) {
            val vm: TemplateListViewModel = viewModel()
            val templates by vm.templates.collectAsStateWithLifecycle()

            TemplatesScreen(
                templates = templates,
                onCreateTemplate = { navController.navigate(Routes.TEMPLATE_CREATE) },
                onEditTemplate = { id -> navController.navigate(Routes.templateEdit(id)) },
                onOpenTemplate = { id -> navController.navigate(Routes.templateDetail(id)) },
                onDeleteTemplate = { id -> vm.deleteTemplate(id) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.TEMPLATE_CREATE) {
            val vm: TemplateEditorViewModel = viewModel()
            val draft by vm.draft.collectAsStateWithLifecycle()

            LaunchedEffect(vm) { vm.createDraft() }

            TemplateMetadataScreen(
                draft = draft,
                isCreating = true,
                onUpdate = vm::update,
                onSave = vm::save,
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(vm) {
                vm.events.collect { event ->
                    when (event) {
                        TemplateEditorViewModel.EditorEvent.Saved,
                        TemplateEditorViewModel.EditorEvent.NotFound -> navController.popBackStack()
                    }
                }
            }
        }

        composable(Routes.TEMPLATE_EDIT) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId").orEmpty()
            val vm: TemplateEditorViewModel = viewModel()
            val draft by vm.draft.collectAsStateWithLifecycle()

            LaunchedEffect(templateId) { vm.load(templateId) }

            TemplateMetadataScreen(
                draft = draft,
                isCreating = false,
                onUpdate = vm::update,
                onSave = vm::save,
                onBack = { navController.popBackStack() }
            )

            LaunchedEffect(vm) {
                vm.events.collect { event ->
                    when (event) {
                        TemplateEditorViewModel.EditorEvent.Saved,
                        TemplateEditorViewModel.EditorEvent.NotFound -> navController.popBackStack()
                    }
                }
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
            val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()

            LaunchedEffect(templateId) {
                vm.load(templateId)
            }

            TemplateDetailScreen(
                template = template,
                allApps = allApps,
                searchQuery = searchQuery,
                filter = filter,
                showSystem = showSystem,
                isGenerating = isGenerating,
                onSearch = vm::updateSearchQuery,
                onFilter = vm::updateFilter,
                onToggleShowSystem = vm::toggleShowSystem,
                onToggleApp = vm::toggleApp,
                onSetInstallMode = vm::setInstallMode,
                // Deletion navigation is driven by the ViewModel event so the back stack pops once.
                onDeleteTemplate = vm::deleteTemplate,
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
            var generationEvent by remember {
                mutableStateOf<TemplateDetailViewModel.DetailEvent?>(null)
            }

            LaunchedEffect(templateId) {
                vm.load(templateId)
            }

            PreviewScreen(
                template = template,
                allApps = allApps,
                isGenerating = isGenerating,
                generationEvent = generationEvent,
                onBack = { navController.popBackStack() },
                onGenerate = vm::generateModule,
                onDismissEvent = { generationEvent = null },
                onOpenInstall = { event ->
                    runCatching {
                        navController.context.startActivity(vm.buildOpenInstallIntent(event.file))
                    }.onFailure {
                        generationEvent = TemplateDetailViewModel.DetailEvent.Error(
                            "未找到可打开 ZIP 的模块管理器或文件应用"
                        )
                    }
                },
                onShare = { event ->
                    runCatching {
                        navController.context.startActivity(
                            Intent.createChooser(vm.buildShareIntent(event.file), "分享模块")
                        )
                    }.onFailure {
                        generationEvent = TemplateDetailViewModel.DetailEvent.Error("无法分享生成的模块")
                    }
                }
            )

            LaunchedEffect(vm) {
                vm.events.collect { ev ->
                    generationEvent = ev
                }
            }
        }
    }
}
