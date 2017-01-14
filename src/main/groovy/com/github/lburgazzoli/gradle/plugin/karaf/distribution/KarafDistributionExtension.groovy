/**
 * Copyright 2017, Luca Burgazzoli and contributors as indicated by the @author tags
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
package com.github.lburgazzoli.gradle.plugin.karaf.distribution

import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
class KarafDistributionExtension {
    private final Project project

    boolean enabled

    @InputDirectory
    File sourceDirectory
    @OutputDirectories
    File workDirectory

    File featuresCfgFile
    File startupPropertiesFile
    File systemDirectory

    List<KarafEditInstruction> editInstructions

    KarafDistributionExtension(Project project) {
        this.project = project
        this.enabled = false
        this.editInstructions = new ArrayList<>()
    }

    // ************************************************************************
    // Edit instructions
    // ************************************************************************

    void edit(Closure closure) {
        editInstructions << ConfigureUtil.configure(
            closure,
            new KarafEditInstruction(project)
        )
    }

    List<KarafEditInstruction> getEditInstructions() {
        return this.editInstructions
    }

    boolean hasEditInstructions() {
        return !this.editInstructions.empty
    }
}
