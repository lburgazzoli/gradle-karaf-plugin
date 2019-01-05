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

import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.ConfigureUtil

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import spock.lang.Specification

/**
 * @author lburgazzoli
 */
class KarafTestSupport extends Specification {

    // *************************************************************************
    //
    // *************************************************************************

    KarafPluginExtension getKarafExtension(Project project) {
        KarafPluginExtension.lookup(project)
    }

    KarafFeaturesTask  getKarafFeaturesTasks(Project project) {
        project.tasks.getByName(KarafFeaturesTask.NAME)
    }

    KarafKarTask getKarafKarTasks(Project project) {
        project.tasks.getByName(KarafKarTask.NAME)
    }

    // *************************************************************************
    //
    // *************************************************************************

    def setupProject(String name) {
        setupProject(ProjectBuilder.builder().withName(name).build())
    }

    def setupProject(String name, Closure closure) {
        ConfigureUtil.configure(
            closure,
            setupProject(ProjectBuilder.builder().withName(name).build())
        )
    }

    def setupProject(String group, String name, String version) {
        Project project = ProjectBuilder.builder().withName(name).build()
        project.group = group
        project.version = version

        setupProject(project)
    }

    def setupProject(String group, String name, String version, Closure closure) {
        Project project = ProjectBuilder.builder().withName(name).build()
        project.group = group
        project.version = version

        ConfigureUtil.configure(
            closure,
            setupProject(project)
        )
    }

    def setupProject(Project project) {
        project.apply plugin: 'java'
        project.apply plugin: 'maven'

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        new KarafPlugin().apply(project)

        return project
    }
}
