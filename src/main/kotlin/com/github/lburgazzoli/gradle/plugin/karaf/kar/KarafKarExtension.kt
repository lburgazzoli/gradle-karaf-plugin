package com.github.lburgazzoli.gradle.plugin.karaf.kar

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import java.nio.file.Path
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
abstract class KarafKarExtension {
    companion object {
        const val NAME = "kar"
    }

    abstract val enabled: Property<Boolean>

    abstract val archiveName: Property<String>

    @get:Inject
    protected abstract val layout: ProjectLayout

    abstract val outputDir: DirectoryProperty

    val outputPath: Path
        get() = outputDir.get().asFile.toPath()

    init {
        enabled.convention(false)
        archiveName.convention(null)
        outputDir.convention(layout.buildDirectory.dir("karaf/kar"))
    }
}
