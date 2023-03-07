package com.github.lburgazzoli.gradle.plugin.karaf.repo

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnProtocolParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.the
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

/**
 * @author realPyR3X
 */
abstract class KarafRepoTask : DefaultTask() {
    companion object {
        const val GROUP = "karaf"
        const val NAME = "generateRepo"
        const val DESCRIPTION = "Generates Karaf Repo Directory"
    }

    init {
        outputs.upToDateWhen {
            false
        }
    }

    @get:Inject
    abstract val copyOps: FileSystemOperations

    fun asRepoPath(root: Path, path: String?, name: String?): Path = root.resolve(path).resolve(name)

    @TaskAction
    fun run() {
        val karaf = project.the<KarafPluginExtension>()
        if (!karaf.hasRepo() && !karaf.hasKar()) {
            return
        }
        val features = karaf.features
        val repo = karaf.repo
        val resolver = features.resolver
        val root = repo.outputPath
        copyOps.delete {
            delete(root)
        }
        Files.createDirectories(root)

        features.featureDescriptors.forEach { feature ->
            resolver.resolve(feature).forEach {
                val dep = it.dependency
                copyOps.copy {
                    from(dep.file!!)
                    into(asRepoPath(
                        root,
                        "${dep.group!!.replace(".", "/")}/${dep.name}/${dep.version}",
                        "${dep.name}-${dep.version}${dep.classifier?.let { "-$it" } ?: ""}.${dep.type}"
                    ))
                }
            }

            feature.configFiles.forEach {
                if (it.filename != null && it.uri != null && it.file != null) {
                    val dep = MvnProtocolParser.parse(it.uri)
                    copyOps.copy {
                        from(it.file)
                        into(asRepoPath(
                            root,
                            "${dep.group!!.replace(".", "/")}/${dep.name}/${dep.version}",
                            "${dep.name}-${dep.version}${dep.classifier?.let { "-$it" } ?: ""}.${dep.type}"
                        ))
                    }
                }
            }
        }

        copyOps.copy {
            from(features.outputFile)
            into(
                asRepoPath(
                    root,
                    "${
                        features.group.get().toString().replace(".", "/")
                    }/${features.name.get()}/${features.version.get()}",
                    "${features.name.get()}-${features.version.get()}-features.xml"
                )
            )
        }
    }
}
