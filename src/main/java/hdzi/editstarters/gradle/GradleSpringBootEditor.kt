package hdzi.editstarters.gradle

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ThrowableComputable
import hdzi.editstarters.springboot.SpringBootEditor
import hdzi.editstarters.springboot.bean.ProjectDependency
import hdzi.editstarters.springboot.bean.StarterInfo
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Created by taojinhou on 2019/1/14.
 */
class GradleSpringBootEditor(context: DataContext) : SpringBootEditor(context, {
    val project = context.getData(DataKeys.PROJECT)!!
    val basePath = context.getData(DataKeys.VIRTUAL_FILE)!!.parent!!.path
    val setting: GradleExecutionSettings = ExternalSystemApiUtil.getExecutionSettings(
        project,
        basePath,
        GradleConstants.SYSTEM_ID
    )

    ProgressManager.getInstance()
        .runProcessWithProgressSynchronously(ThrowableComputable<List<ProjectDependency>, Exception> {
            GradleExecutionHelper().execute(basePath, setting) { connect ->
                val ideaModule = connect.getModel(IdeaProject::class.java).modules.getAt(0)
                ideaModule.dependencies
                    .filter { it is IdeaSingleEntryLibraryDependency && it.gradleModuleVersion != null }
                    .map {
                        val moduleVersion = (it as IdeaSingleEntryLibraryDependency).gradleModuleVersion!!
                        ProjectDependency(moduleVersion.group, moduleVersion.name, moduleVersion.version)
                    }
            }
        }, "Load Gradle Project", false, project)
}) {

    override fun addDependencies(starterInfos: Collection<StarterInfo>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeDependencies(starterInfos: Collection<StarterInfo>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
