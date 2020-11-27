package hdzi.editstarters.buildsystem.gradle

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ThrowableComputable
import hdzi.editstarters.buildsystem.BuildDependency
import hdzi.editstarters.buildsystem.BuildSystem
import hdzi.editstarters.ui.ShowErrorException
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

/**
 * Created by taojinhou on 2019/1/14.
 */
class GradleBuildSystem(context: DataContext) : BuildSystem(
    context,
    {
        val psiFile = context.getData(CommonDataKeys.PSI_FILE)!!
        when (psiFile.name) {
            GradleConstants.DEFAULT_SCRIPT_NAME ->
                BuildGradle(context.getData(CommonDataKeys.PROJECT)!!, psiFile as GroovyFile)
            GradleConstants.KOTLIN_DSL_SCRIPT_NAME ->
                BuildGradleKts(context.getData(CommonDataKeys.PROJECT)!!, psiFile as KtFile)
            else -> throw ShowErrorException("Not support extension!")
        }
    },
    {
        val project = context.getData(CommonDataKeys.PROJECT)!!
        val basePath = context.getData(CommonDataKeys.VIRTUAL_FILE)!!.parent!!.path
        val setting: GradleExecutionSettings =
            ExternalSystemApiUtil.getExecutionSettings(project, basePath, GradleConstants.SYSTEM_ID)

        val progressManager = ProgressManager.getInstance()
        progressManager.runProcessWithProgressSynchronously(
            ThrowableComputable<List<BuildDependency>, Exception> {
                progressManager.progressIndicator.isIndeterminate = true
                GradleExecutionHelper().execute(basePath, setting) { connect ->
                    val ideaModule = connect.getModel(IdeaProject::class.java).modules.getAt(0)
                    ideaModule.dependencies
                        .filter { it is IdeaSingleEntryLibraryDependency && it.gradleModuleVersion != null }
                        .map {
                            val moduleVersion = (it as IdeaSingleEntryLibraryDependency).gradleModuleVersion!!
                            BuildDependency(moduleVersion.group, moduleVersion.name, moduleVersion.version)
                        }
                }
            }, "Load Gradle Project", false, project
        )
    })
