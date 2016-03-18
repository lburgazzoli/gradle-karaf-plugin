/**
 * Copyright 2016, Luca Burgazzoli and contributors as indicated by the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.lburgazzoli.gradle.plugin.karaf.kar

import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory

/**
 * @author lburgazzoli
 */
class KarafKarExtension {
    private final Project project
    boolean enabled;
    private File outputDir
    private File explodedDir

    KarafKarExtension(Project project) {
        this.project = project
        this.enabled = false
        this.outputDir = new File("${project.buildDir}/karaf/kar")
        this.explodedDir = new File("${project.buildDir}/karaf/kar/exploded")
    }

    @OutputDirectory
    File getExplodedDir() {
        return explodedDir
    }

    Path getExplodedPath() {
        return explodedDir.toPath()
    }

    @OutputDirectory
    File getOutputDir() {
        return outputDir
    }

    Path getOutputPath() {
        return outputDir.toPath()
    }
}
