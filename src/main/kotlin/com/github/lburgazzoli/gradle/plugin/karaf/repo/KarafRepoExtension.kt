package com.github.lburgazzoli.gradle.plugin.karaf.repo

import org.gradle.api.Project
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

/**
 * @author realPyR3X
 */
open class KarafRepoExtension @Inject constructor(private val project: Project) {
    companion object {
        const val NAME = "repo"
    }

    var enabled: Boolean = false

    /**
     * Returns [enabled]; Groovy build scripts call it as `isEnabled()`.
     */
    fun isEnabled(): Boolean = enabled

    private var explicitOutputDir: File? = null

    /**
     * The directory the repository is written to; `build/karaf/repo` unless set.
     */
    var outputDir: File
        get() = explicitOutputDir ?: project.layout.buildDirectory.dir("karaf/repo").get().asFile
        set(value) {
            explicitOutputDir = value
        }

    val outputPath: Path
        get() = outputDir.toPath()
}
