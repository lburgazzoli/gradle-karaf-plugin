package com.github.lburgazzoli.gradle.plugin.karaf.repo

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskExtension
import org.gradle.api.tasks.Internal
import org.gradle.api.Action
import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnProtocolParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.Dependency
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.Path
import java.nio.file.Paths

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

    /**
     * Returns the path of [fileName] in the Maven layout under [root]: the [group] with each dot as a directory, then
     * [artifactId] and [version]. An empty or null [group] adds no directory.
     *
     * @throws IllegalArgumentException if the coordinates name a path outside [root], for example through `..` or a
     * leading `/`
     */
    fun asRepoPath(root: Path, group: String?, artifactId: String?, version: String?, fileName: String): Path {
        val segments = group.orEmpty().split('.') + listOfNotNull(artifactId, version, fileName)
        val relative = segments.filter { it.isNotEmpty() }.joinToString("/")
        val normalizedRoot = root.normalize()
        val path = normalizedRoot.resolve(relative).normalize()
        require(!Paths.get(relative).isAbsolute && path.startsWith(normalizedRoot) && path != normalizedRoot) {
            "Maven coordinates $group:$artifactId:$version ($fileName) name a path outside the repository $root"
        }
        return path
    }

    private val karafExtension = KarafTaskExtension(this)

    /**
     * The settings this task reads: the extension set with [karaf], or else the project's `karaf` extension.
     */
    @get:Internal
    val karaf: KarafPluginExtension
        get() = karafExtension.extension

    /**
     * Makes this task read [extension] instead of the project's `karaf` extension.
     */
    fun karaf(extension: KarafPluginExtension) {
        karafExtension.set(extension)
    }

    /**
     * Makes this task read a new extension, configured by [action], instead of the project's `karaf` extension.
     */
    fun karaf(action: Action<in KarafPluginExtension>) {
        karafExtension.set(action)
    }

    @TaskAction
    fun run() {
        if (!karaf.hasRepo() && !karaf.hasKar()) {
            return
        }
        val features = karaf.features
        val repo = karaf.repo
        val resolver = features.resolver
        val root = repo.outputPath
        root.toFile().deleteRecursively()
        Files.createDirectories(root)

        features.featureDescriptors.forEach { feature ->
            resolver.resolve(feature).forEach {
                val dep = it.dependency
                copy(dep.file, asRepoPath(root, dep))
            }

            feature.configFiles.forEach {
                if (!it.filename.isNullOrEmpty() && !it.uri.isNullOrEmpty() && it.file != null) {
                    copy(it.file, asRepoPath(root, MvnProtocolParser.parse(it.uri)))
                }
            }
        }

        copy(
            features.outputFile,
            asRepoPath(
                root,
                features.group,
                features.name,
                features.version,
                "${features.name}-${features.version}-features.xml"
            )
        )
    }

    private fun asRepoPath(root: Path, dep: Dependency): Path =
        asRepoPath(
            root,
            dep.group,
            dep.name,
            dep.version,
            if (dep.hasClassifier) {
                "${dep.name}-${dep.version}-${dep.classifier}.${dep.type}"
            } else {
                "${dep.name}-${dep.version}.${dep.type}"
            }
        )

    /**
     * Copies [source] to the file [destination], creating its parent directories.
     */
    private fun copy(source: File?, destination: Path) {
        if (source == null) {
            return
        }
        Files.createDirectories(destination.parent)
        Files.copy(
            source.toPath(),
            destination,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        )
    }
}
