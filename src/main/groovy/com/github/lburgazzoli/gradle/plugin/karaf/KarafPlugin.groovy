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
package com.github.lburgazzoli.gradle.plugin.karaf

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
/**
 * @author lburgazzoli
 */
class KarafPlugin implements Plugin<Project> {
    static final String CONFIGURATION_NAME = 'karaf'
    static final String COMPONENT_NAME = 'karaf'

    @Override
    void apply(Project project) {
        KarafPluginExtension.create(project)

        project.configurations.create(CONFIGURATION_NAME)

        // Karaf Features
        def feat = project.task( KarafFeaturesTask.NAME , type: KarafFeaturesTask) {
            group       = KarafFeaturesTask.GROUP
            description = KarafFeaturesTask.DESCRIPTION
        }

        // Karaf KAR
        def kar = project.task( KarafKarTask.NAME , type: KarafKarTask) {
            group       = KarafKarTask.GROUP
            description = KarafKarTask.DESCRIPTION
        }

        def assemble = project.tasks.findByName(BasePlugin.ASSEMBLE_TASK_NAME)

        kar.dependsOn feat
        assemble?.dependsOn kar

        //PublishArtifact karArtifact = project.artifacts.add(CONFIGURATION_NAME, kar)
        //project.components.add(new KarJavaLibrary(karArtifact, project.configurations.karaf.allDependencies))
    }

    /*
    class KarJavaLibrary extends JavaLibrary {
        KarJavaLibrary(PublishArtifact jarArtifact, DependencySet runtimeDependencies) {
            super(jarArtifact, runtimeDependencies)
        }

        @Override
        String getName() {
            return COMPONENT_NAME
        }
    }
    */
}
