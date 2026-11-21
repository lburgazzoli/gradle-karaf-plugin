package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import java.util.concurrent.Callable

/**
 * @author lburgazzoli
 */
class KarafPlugin : Plugin<Project> {
    companion object {
        const val ARTIFACTS_CONFIGURATION_NAME = "archives"
        const val CONFIGURATION_NAME = "karaf"

        @JvmStatic
        val ARTIFACT_TASKS = listOf(JavaPlugin.JAR_TASK_NAME, WarPlugin.WAR_TASK_NAME)

        @JvmStatic
        val ARCHIVE_TASKS = listOf(BasePlugin.ASSEMBLE_TASK_NAME)
    }

    override fun apply(project: Project) {
        project.extensions.create(KarafPluginExtension.NAME, KarafPluginExtension::class.java, project)
        project.configurations.create(CONFIGURATION_NAME)

        // Inputs, outputs, and dependencies are computed when Gradle asks for them, from each task's own karaf
        // settings, so nothing is resolved or realized while the build is configured. configureEach reaches the
        // tasks a build script registers as well.
        project.tasks.withType(KarafFeaturesTask::class.java).configureEach {
            val task = this
            dependsOn(Callable { archiveTasks(task.project, task.karaf) })
            inputs.files(Callable { task.karaf.features.featureDescriptors.flatMap { it.configurations } })
            outputs.files(Callable { if (task.karaf.hasFeatures()) listOf(task.karaf.features.outputFile) else emptyList() })
        }
        project.tasks.withType(KarafRepoTask::class.java).configureEach {
            val task = this
            outputs.dirs(Callable {
                if (task.karaf.hasRepo() || task.karaf.hasKar()) listOf(task.karaf.repo.outputDir) else emptyList()
            })
        }

        val feat = project.tasks.register(KarafFeaturesTask.NAME, KarafFeaturesTask::class.java) {
            group = KarafFeaturesTask.GROUP
            description = KarafFeaturesTask.DESCRIPTION
        }

        // Karaf Repo
        val repo = project.tasks.register(KarafRepoTask.NAME, KarafRepoTask::class.java) {
            group = KarafRepoTask.GROUP
            description = KarafRepoTask.DESCRIPTION
            dependsOn(feat)
        }

        // Karaf KAR
        val kar = project.tasks.register(KarafKarTask.NAME, KarafKarTask::class.java) {
            group = KarafKarTask.GROUP
            description = KarafKarTask.DESCRIPTION
            dependsOn(repo)
        }

        // The archives configuration exists only once a base plugin is applied, which may happen after this one.
        project.afterEvaluate {
            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, feat.map { it.karaf.features.outputFile })
            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, repo.map { it.karaf.repo.outputDir })
            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, kar)
        }
    }

    /**
     * Returns the archive tasks a features task depends on: the `war` task, or else the `jar` task, of [project], and
     * of every project that the configurations of [karaf]'s features depend on.
     */
    private fun archiveTasks(project: Project, karaf: KarafPluginExtension): List<Task> {
        val tasks = mutableListOf<Task>()
        archiveTask(project)?.let { tasks += it }
        if (karaf.hasFeatures()) {
            karaf.features.featureDescriptors.forEach {
                KarafUtils.walkDeps(it.configurations) { _: Configuration, root: ResolvedComponentResult ->
                    val id = root.id
                    if (id is ProjectComponentIdentifier) {
                        // findProject returns null for a path this build does not have, instead of failing the build.
                        project.findProject(id.projectPath)?.let(::archiveTask)?.let { task -> tasks += task }
                    }
                }
            }
        }
        return tasks
    }

    private fun archiveTask(project: Project): Task? =
        project.tasks.findByName(WarPlugin.WAR_TASK_NAME) ?: project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
}
