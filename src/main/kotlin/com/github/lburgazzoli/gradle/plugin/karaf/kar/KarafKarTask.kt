package com.github.lburgazzoli.gradle.plugin.karaf.kar

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.the
import java.nio.file.Files

/**
 * @author lburgazzoli
 */
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

    override fun copy() {
        val karaf = project.the<KarafPluginExtension>()

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
        archiveVersion.set(features.version.map { it.toString() })
        archiveExtension.set(EXTENSION)
        destinationDirectory.set(kar.outputDir)
        if (kar.archiveName.isPresent) {
            archiveVersion.set(null as String?)
            archiveBaseName.set(kar.archiveName)
        }
        from(repo.outputDir)
        into("repository")
        super.copy()
    }
}
