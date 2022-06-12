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

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
import groovy.xml.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.ConfigureUtil
import spock.lang.Specification

/**
 * @author lburgazzoli
 */
class KarafTestSupport extends Specification {

    KarafPluginExtension getKarafExtension(Project project) {
        KarafPluginExtension.lookup(project)
    }

    KarafFeaturesTask getKarafFeaturesTasks(Project project) {
        return (KarafFeaturesTask)project.tasks.getByName(KarafFeaturesTask.NAME)
    }

    KarafRepoTask getKarafRepoTasks(Project project) {
        return (KarafRepoTask)project.tasks.getByName(KarafRepoTask.NAME)
    }

    KarafKarTask getKarafKarTasks(Project project) {
        return (KarafKarTask)project.tasks.getByName(KarafKarTask.NAME)
    }

    def setUpProject(String group, String name, String version) {
        Project project = ProjectBuilder.builder().withName(name).build()
        project.group = group
        project.version = version

        setUpProject(project)
    }

    def setUpProject(String group, String name, String version, Closure closure) {
        Project project = ProjectBuilder.builder().withName(name).build()
        project.group = group
        project.version = version

        ConfigureUtil.configure(
            closure,
            setUpProject(project)
        )
    }

    def setUpProject(Project project) {
        project.apply plugin: 'java'

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        new KarafPlugin().apply(project)

        return project
    }

    def configureProject(Project project, Closure closure) {
        ConfigureUtil.configure(closure, project)

        return project
    }

    def findAllBundles(GPathResult features, Closure closure) {
        return features.feature.bundle.'**'.findAll(closure)
    }

    def findAllBundles(GPathResult features, String id) {
        return findAllBundles(features) { it.text() =~ id }
    }
}
