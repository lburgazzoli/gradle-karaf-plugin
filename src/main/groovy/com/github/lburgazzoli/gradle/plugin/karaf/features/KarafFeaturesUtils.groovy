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

import java.util.jar.JarFile
import java.util.jar.Manifest
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPlugin
import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafUtils
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
/**
 * @author lburgazzoli
 */
class KarafFeaturesUtils extends KarafUtils {

    static Set<DependencyDescriptor> collectDependencies(
        FeatureDescriptor featureDescriptor, Set<DependencyDescriptor> container) {

        KarafUtils.walkDeps(featureDescriptor.configurations) {
            Configuration configuration, ResolvedComponentResult root ->
                collectDependencies(featureDescriptor, configuration, root, container)
        }

        return container;
    }

    static void collectDependencies(
            FeatureDescriptor featureDescriptor, Configuration configuration, ResolvedComponentResult root,  Set<DependencyDescriptor> container) {

        def ext = KarafPluginExtension.lookup(featureDescriptor.project)
        def instruction = featureDescriptor.findBundleDescriptors(root.moduleVersion)
        if(instruction && !instruction.include) {
            return
        }

        if(root.id instanceof ProjectComponentIdentifier) {
            ProjectComponentIdentifier pci = root.id as ProjectComponentIdentifier
            Project prj = featureDescriptor.project.findProject(pci.getProjectPath())

            if(prj == featureDescriptor.project && !ext.features.includeProject) {
                return
            }

            KarafUtils.forEachTask(prj, KarafPlugin.ARTIFACT_TASKS) {
                container << DependencyDescriptor.make(root, it, instruction)
            }
        } else {

            findArtifact(configuration, root.moduleVersion)?.each {
                container << DependencyDescriptor.make(root, it, instruction)
            }
        }
    }

    static boolean hasOsgiManifestHeaders(File file) {
        if(file != null && file.exists()) {
            JarFile jarFile = new JarFile(file)
            Manifest manifest = jarFile.getManifest()
            if (manifest != null) {
                return hasOneOfTheAttributes(manifest, ["Bundle-SymbolicName", "Bundle-Name"])
            }
        }

        return false
    }

    static boolean hasAttribute(Manifest manifest, String attributeName) {
        String value = manifest.mainAttributes.getValue( attributeName )
        return value != null && !value.trim().isEmpty()
    }

    static boolean hasOneOfTheAttributes(Manifest manifest, Collection<String> attributeNames) {
        return attributeNames.find { hasAttribute(manifest, it) } != null
    }

    static boolean matches(ModuleVersionIdentifier v1, ModuleVersionIdentifier v2) {
        return v1.group.equals(v2.group) && v1.name.equals(v2.name) && v1.version.equals(v2.version)
    }

    static Collection<ResolvedArtifact> findArtifact(Configuration configuration, ModuleVersionIdentifier versionIdentifier) {
        return configuration.resolvedConfiguration.resolvedArtifacts.findAll {
            matches(versionIdentifier, it.moduleVersion.id)
        }
    }
}
