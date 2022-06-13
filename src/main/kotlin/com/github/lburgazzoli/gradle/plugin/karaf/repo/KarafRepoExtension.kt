package com.github.lburgazzoli.gradle.plugin.karaf.repo

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

/**
 * @author realPyR3X
 */
abstract class KarafRepoExtension {
    companion object {
        const val NAME = "repo"
    }

    abstract val enabled: Property<Boolean>

    @get:Inject
    protected abstract val layout: ProjectLayout

    abstract val outputDir: DirectoryProperty

    val outputPath: Path
        get() = outputDir.get().asFile.toPath()

    init {
        enabled.convention(false)
        outputDir.convention(layout.buildDirectory.dir("karaf/repo"))
    }
}
