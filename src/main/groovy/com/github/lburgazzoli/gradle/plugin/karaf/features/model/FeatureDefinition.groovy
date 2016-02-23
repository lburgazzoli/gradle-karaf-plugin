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

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
abstract class FeatureDefinition {
    final List<BundleInstructionDescriptor> bundleInstructions
    final List<FeatureDependencyDescriptor> featureDependencies
    final Set<Config> configs
    final Set<ConfigFile> configFiles

    protected FeatureDefinition() {
        this.bundleInstructions = new LinkedList<>()
        this.featureDependencies = new LinkedList<>()
        this.configs = new LinkedHashSet<>()
        this.configFiles = new LinkedHashSet<>()
    }

    // *************************************************************************
    // Bundles
    // *************************************************************************

    def bundle(String pattern) {
        bundleInstructions << BundleInstructionDescriptor.fromPattern(pattern)
    }

    def bundle(String pattern, Closure closure) {
        def descriptor = BundleInstructionDescriptor.fromPattern(pattern)
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

    // *************************************************************************
    // Config/ConfigFile
    // *************************************************************************

    def config(String name, Closure closure) {
        configs << ConfigureUtil.configure(closure, new Config(name))
    }

    def configFile(String uri, String fileName, Closure closure) {
        def configFile = new ConfigFile(uri, fileName)
        if(closure) {
            ConfigureUtil.configure(closure, configFile)
        }

        configFiles << configFile
    }

    // *************************************************************************
    //
    // *************************************************************************

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = [ 'name' ])
    class Config {
        final String name
        boolean append
        String content

        public Config(String name) {
            this.name = name
            this.append = false
            this.content = null
        }
    }

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = [ 'uri', 'filename' ])
    class ConfigFile {
        final String uri
        final String filename
        boolean override

        public ConfigFile(String uri, String filename) {
            this.uri = name
            this.filename = filename
            this.override = false
        }
    }
}
