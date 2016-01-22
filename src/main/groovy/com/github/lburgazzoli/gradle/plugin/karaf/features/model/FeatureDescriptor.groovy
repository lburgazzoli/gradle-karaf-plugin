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

package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import groovy.transform.ToString
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
@ToString(includeNames = true)
class FeatureDescriptor {
    final Project project

    String name
    String version
    String description

    final List<Configuration> configurations
    final List<BundleInstructionDescriptor> bundleInstructions
    final List<FeatureDependencyDescriptor> featureDependencies

    boolean includeProject

    public FeatureDescriptor(Project project) {
        this.project = project
        this.name = null
        this.version = project.version
        this.description = null
        this.configurations = []
        this.bundleInstructions = []
        this.featureDependencies = []
        this.includeProject = false

        // If \"runtime\" configuration exists add it as default
        Configuration runtime = project.configurations.findByName("runtime")
        if(runtime) {
            this.configurations << runtime
        }
    }

    // *************************************************************************
    // Configurations
    // *************************************************************************

    void configurations(Configuration... configurations) {
        this.configurations.clear();
        this.configurations.addAll(configurations)
    }

    void configuration(Configuration configuration) {
        this.configurations << configuration
    }

    List<Configuration> getConfigurations() {
        return this.configurations
    }

    // *************************************************************************
    // Bundles
    // *************************************************************************

    def bundle(String pattern, Closure closure) {
        def descriptor = new BundleInstructionDescriptor(DependencyMatcher.from(pattern))
        if(closure) {
            ConfigureUtil.configure(closure, descriptor)
        }

        bundleInstructions << descriptor
    }

    def bundle(Closure closure) {
        bundleInstructions << ConfigureUtil.configure(
            closure,
            new BundleExtendedInstructionDescriptor()
        )
    }

    List<BundleInstructionDescriptor> getBundleInstructions() {
        return this.bundleInstructions
    }

    BundleInstructionDescriptor findBundleInstructions(DependencyDescriptor dependency) {
        return this.bundleInstructions.find { it.matches( dependency ) }
    }

    BundleInstructionDescriptor findBundleInstructions(ModuleVersionIdentifier identifier) {
        return this.bundleInstructions.find { it.matches( identifier ) }
    }

    // *************************************************************************
    // Features
    // *************************************************************************

    def features(Collection<String> featureNames) {
        this.featureDependencies.clear()

        featureNames.each {
            this.feature(it, null)
        }
    }

    def feature(FeatureDescriptor feature) {
        this.feature(feature.name, null)
    }

    def feature(FeatureDescriptor feature, Closure closure) {
        this.feature(feature.name, closure)
    }

    def feature(String featureName) {
        this.feature(featureName, null)
    }

    def feature(String featureName, Closure closure) {
        def featureDependencyDescriptor = new FeatureDependencyDescriptor(featureName)
        if ( closure ) {
            ConfigureUtil.configure( closure, featureDependencyDescriptor )
        }

        this.featureDependencies << featureDependencyDescriptor
    }

    List<FeatureDependencyDescriptor> getFeatureDependencies() {
        return this.featureDependencies
    }
}
