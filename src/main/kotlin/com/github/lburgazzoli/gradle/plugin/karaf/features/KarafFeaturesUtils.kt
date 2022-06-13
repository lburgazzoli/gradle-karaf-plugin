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
package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafUtils
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.util.jar.JarFile
import java.util.jar.Manifest

/**
 * @author lburgazzoli
 */
object KarafFeaturesUtils {

    fun collectDependencies(
        featureDescriptor: FeatureDescriptor, container: MutableSet<DependencyDescriptor>
    ): Set<DependencyDescriptor> {
        KarafUtils.walkDeps(featureDescriptor.configurations.get()) { configuration: Configuration, root: ResolvedComponentResult ->
            collectDependencies(featureDescriptor, configuration, root, container)
        }

        return container;
    }

    fun collectDependencies(
        featureDescriptor: FeatureDescriptor,
        configuration: Configuration,
        root: ResolvedComponentResult,
        container: MutableSet<DependencyDescriptor>
    ) {
        val ext = featureDescriptor._project.the<KarafPluginExtension>()

        if (root.id is ProjectComponentIdentifier) {
            val pci = root.id as ProjectComponentIdentifier
            val prj = featureDescriptor._project.findProject(pci.getProjectPath())!!

            val includeProject = ext.features.includeProject.getOrElse(featureDescriptor.includeProject)

            if (prj == featureDescriptor._project && !includeProject) {
                return
            }

            val instruction = featureDescriptor.findBundleDescriptors(root.moduleVersion!!)
            if (instruction == null || instruction.include) {
                val war = prj.tasks.withType<War>().matching { it.name == WarPlugin.WAR_TASK_NAME }.firstOrNull()
                val jar = prj.tasks.withType<Jar>().matching { it.name == JavaPlugin.JAR_TASK_NAME }.firstOrNull()

                if (war != null) {
                    container += DependencyDescriptor.make(root, war, instruction)
                } else if (jar != null) {
                    container += DependencyDescriptor.make(root, jar, instruction)
                }
            }
        } else {
            findArtifact(configuration, root.moduleVersion!!).forEach {
                val instruction = featureDescriptor.findBundleDescriptors(it)
                if (instruction == null || instruction.include) {
                    container += DependencyDescriptor.make(root, it, instruction)
                }
            }
        }
    }

    fun hasOsgiManifestHeaders(file: File?): Boolean {
        if (file != null && file.exists()) {
            val jarFile = JarFile(file)
            val manifest = jarFile.getManifest()
            if (manifest != null) {
                return hasOneOfTheAttributes(manifest, listOf("Bundle-SymbolicName", "Bundle-Name"))
            }
        }

        return false
    }

    fun hasOneOfTheAttributes(manifest: Manifest, attributeNames: Collection<String>): Boolean {
        return attributeNames.any { hasAttribute(manifest, it) }
    }

    fun hasAttribute(manifest: Manifest, attributeName: String): Boolean {
        val value = manifest.mainAttributes.getValue(attributeName)
        return value != null && !value.trim().isEmpty()
    }

    fun matches(v1: ModuleVersionIdentifier, v2: ModuleVersionIdentifier): Boolean {
        return v1.group == v2.group && v1.name == v2.name && v1.version == v2.version
    }

    fun findArtifact(
        configuration: Configuration,
        versionIdentifier: ModuleVersionIdentifier
    ): Collection<ResolvedArtifact> {
        return configuration.resolvedConfiguration.resolvedArtifacts.filter {
            matches(versionIdentifier, it.moduleVersion.id)
        }
    }
}
