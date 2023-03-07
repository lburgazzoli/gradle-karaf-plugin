package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

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
        val ext = project.extensions.create<KarafPluginExtension>(KarafPluginExtension.NAME, project)
        project.configurations.create(CONFIGURATION_NAME)

        val feat = project.tasks.register(KarafFeaturesTask.NAME, KarafFeaturesTask::class.java) {
            group = KarafFeaturesTask.GROUP
            description = KarafFeaturesTask.DESCRIPTION
        }

        // Karaf Repo
        val repo = project.tasks.register<KarafRepoTask>(KarafRepoTask.NAME) {
            group = KarafRepoTask.GROUP
            description = KarafRepoTask.DESCRIPTION
            dependsOn(feat)
        }

        // Karaf KAR
        val kar = project.tasks.register<KarafKarTask>(KarafKarTask.NAME) {
            group = KarafKarTask.GROUP
            description = KarafKarTask.DESCRIPTION
            dependsOn(repo)
        }

        project.afterEvaluate {
            val war = project.tasks.find { it.name == WarPlugin.WAR_TASK_NAME }
            val jar = project.tasks.find { it.name == JavaPlugin.JAR_TASK_NAME }
            if (war != null) {
                feat.configure {
                    dependsOn(war)
                }
            } else if (jar != null) {
                feat.configure {
                    dependsOn(jar)
                }
            }

            if (ext.hasFeatures()) {
                ext.features.featureDescriptors.forEach {
                    it.configurations.get().forEach { configuration ->
                        feat.configure {
                            inputs.files(configuration)
                            dependsOn(configuration)
                        }
                    }

                    KarafUtils.walkDeps(it.configurations.get()) { configuration: Configuration, root: ResolvedComponentResult ->

                        if (root.id is ProjectComponentIdentifier) {
                            val pci = root.id as ProjectComponentIdentifier
                            val prj = project.findProject(pci.projectPath)!!

                            val war = prj.tasks.find { it.name == WarPlugin.WAR_TASK_NAME }
                            val jar = prj.tasks.find { it.name == JavaPlugin.JAR_TASK_NAME }

                            if (war != null) {
                                feat.configure {
                                    dependsOn(war)
                                }
                            } else if (jar != null) {
                                feat.configure {
                                    dependsOn(jar)
                                }
                            }
                        }
                    }
                }

                // if there is an output file, add that as an output
                if (ext.features.outputFile.isPresent) {
                    feat.configure {
                        outputs.file(ext.features.outputFile)
                    }
                }
            }

            if (ext.hasRepo()) {
                // if there is an output directory, add that as an output
                if (ext.repo.outputDir.isPresent) {
                    repo.configure {
                        outputs.dir(ext.repo.outputDir)
                    }
                }
            }

            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, ext.features.outputFile)
            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, ext.repo.outputDir)
            project.artifacts.add(ARTIFACTS_CONFIGURATION_NAME, kar)
        }
    }
}
