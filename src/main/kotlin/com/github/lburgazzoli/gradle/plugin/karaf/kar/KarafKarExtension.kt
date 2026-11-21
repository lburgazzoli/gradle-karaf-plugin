package com.github.lburgazzoli.gradle.plugin.karaf.kar

import org.gradle.api.Project
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
open class KarafKarExtension @Inject constructor(private val project: Project) {
    companion object {
        const val NAME = "kar"
    }

    var enabled: Boolean = false

    /**
     * Returns [enabled]; Groovy build scripts call it as `isEnabled()`.
     */
    fun isEnabled(): Boolean = enabled

    var archiveName: String? = null

    private var explicitOutputDir: File? = null

    /**
     * The directory the KAR archive is written to; `build/karaf/kar` unless set.
     */
    var outputDir: File
        get() = explicitOutputDir ?: project.layout.buildDirectory.dir("karaf/kar").get().asFile
        set(value) {
            explicitOutputDir = value
        }

    val outputPath: Path
        get() = outputDir.toPath()
}
