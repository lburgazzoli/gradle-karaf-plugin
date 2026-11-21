package com.github.lburgazzoli.gradle.plugin.karaf.kar

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskExtension
import java.nio.file.Files
import org.gradle.api.Action
import org.gradle.api.tasks.Internal
import org.gradle.jvm.tasks.Jar
import org.gradle.work.DisableCachingByDefault

/**
 * @author lburgazzoli
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class KarafKarTask : Jar() {
    companion object {
        const val GROUP = "karaf"
        const val NAME = "generateKar"
        const val DESCRIPTION = "Generates Karaf KAR Archive"
        const val EXTENSION = "kar"
    }

    init {
        outputs.upToDateWhen {
            false
        }
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

    override fun copy() {
        if (!karaf.hasKar()) {
            return
        }
        val features = karaf.features
        val repo = karaf.repo
        val kar = karaf.kar
        Files.createDirectories(kar.outputPath)
        archiveAppendix.set(null as String?)
        archiveClassifier.set(null as String?)
        archiveBaseName.set(features.name)
        archiveVersion.set(features.version)
        archiveExtension.set(EXTENSION)
        destinationDirectory.set(kar.outputDir)
        if (!kar.archiveName.isNullOrEmpty()) {
            archiveVersion.set(null as String?)
            archiveBaseName.set(kar.archiveName)
        }
        from(repo.outputDir)
        into("repository")
        super.copy()
    }
}
