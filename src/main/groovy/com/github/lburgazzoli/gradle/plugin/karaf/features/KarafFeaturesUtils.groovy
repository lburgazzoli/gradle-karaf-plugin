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

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyDescriptor
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

import java.util.jar.JarFile
import java.util.jar.Manifest

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor

/**
 * @author lburgazzoli
 */
class KarafFeaturesUtils {

    static void collectDependencies(FeatureDescriptor featureDescriptor, Set<DependencyDescriptor> container) {
        featureDescriptor.configurations.each {
            collectDependencies(featureDescriptor, it, container)
        }
    }

    static void collectDependencies(FeatureDescriptor featureDescriptor, Configuration configuration, Set<DependencyDescriptor> container) {
        collectDependencies(featureDescriptor, configuration, configuration.incoming.resolutionResult.root, container)
    }

    static void collectDependencies(FeatureDescriptor featureDescriptor, Configuration configuration, ResolvedComponentResult root, Set<DependencyDescriptor> container) {
        def instruction = featureDescriptor.findBundleInstructions(root.moduleVersion)
        if(!instruction || (instruction && instruction.include)) {
            root.dependencies.findAll {
                it instanceof ResolvedDependencyResult
            }.collect {
                (ResolvedDependencyResult) it
            }.each {
                collectDependencies(featureDescriptor, configuration, it.selected, container)
            }

            container << new DependencyDescriptor(
                root,
                findArtifact(configuration, root.moduleVersion),
                instruction
            )
        }
    }

    static boolean hasOsgiManifestHeaders(File file) {
        if(file) {
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

    static ResolvedArtifact findArtifact(Configuration configuration, ModuleVersionIdentifier versionIdentifier) {
        return configuration.resolvedConfiguration.resolvedArtifacts.find {
            matches(versionIdentifier, it.moduleVersion.id)
        }
    }

    static DependencyDescriptor applyRemap(DependencyDescriptor remap, DependencyDescriptor descriptor) {
        def newDescriptor = descriptor
        if(remap) {
            newDescriptor = new DependencyDescriptor()
            newDescriptor.group = remap.group ?: descriptor.group
            newDescriptor.name = remap.name ?: descriptor.name
            newDescriptor.version = remap.version ?: descriptor.version
            newDescriptor.type = remap.type ?: descriptor.type
            newDescriptor.file = remap.file ?: descriptor.file
            newDescriptor.kind = remap.file ?: descriptor.kind
        }

        return newDescriptor
    }
}
