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
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
abstract class FeatureDefinition {
    final List<BundleDescriptor> bundleDescriptors
    final List<FeatureDependencyDescriptor> featureDependencies
    final Set<Config> configs
    final Set<ConfigFile> configFiles
    final List<Capability> capabilities
    
    Boolean includeProject

    protected FeatureDefinition() {
        this.bundleDescriptors = new LinkedList<>()
        this.featureDependencies = new LinkedList<>()
        this.configs = new LinkedHashSet<>()
        this.configFiles = new LinkedHashSet<>()
        this.capabilities = new LinkedList<>()
    }

    // *************************************************************************
    // Bundles
    // *************************************************************************

    def bundle(String pattern) {
        bundleDescriptors << BundleDescriptor.fromPattern(pattern)
    }

    def bundle(String pattern, Closure closure) {
        def descriptor = BundleDescriptor.fromPattern(pattern)
        if(closure) {
            ConfigureUtil.configure(closure, descriptor)
        }

        bundleDescriptors << descriptor
    }

    def bundle(Closure closure) {
        bundleDescriptors << ConfigureUtil.configure(
            closure,
            new BundleExtendedDescriptor()
        )
    }

    List<BundleDescriptor> getBundleDescriptors() {
        return this.bundleDescriptors
    }

    BundleDescriptor findBundleDescriptors(ResolvedArtifact artifact) {
        return this.bundleDescriptors.find {
            it.matcher.matches(
                artifact.moduleVersion.id.group,
                artifact.moduleVersion.id.name,
                artifact.moduleVersion.id.version,
                artifact.type,
                artifact.classifier
            )
        }
    }

    BundleDescriptor findBundleDescriptors(DependencyDescriptor dependency) {
        return this.bundleDescriptors.find {
            it.matcher.matches(dependency)
        }
    }

    BundleDescriptor findBundleDescriptors(ModuleVersionIdentifier identifier) {
        return this.bundleDescriptors.find {
            it.matcher.matches(
                identifier.group,
                identifier.name,
                identifier.version,
                null,
                null
            )
        }
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
    
    def configFile(String uri, String fileName, boolean override, Closure closure) {
        def configFile = new ConfigFile(uri, fileName, override)
        if(closure) {
            ConfigureUtil.configure(closure, configFile)
        }

        configFiles << configFile
    }

    def configFile(Closure closure) {
        def configFile = new ConfigFile()
        if(closure) {
            ConfigureUtil.configure(closure, configFile)
        }

        configFiles << configFile
    }

    // *************************************************************************
    // Capabilities
    // *************************************************************************

    def capability(String ns, Closure closure) {
        capabilities << ConfigureUtil.configure(closure, new Capability(ns))
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
    @EqualsAndHashCode(includes = [ 'uri', 'filename', 'override' ])
    class ConfigFile {
        String uri
        String filename
        File file
        Boolean override

        public ConfigFile() {
            this(null, null, null, null)
        }

        public ConfigFile(String uri, String filename) {
            this(uri, filename, null, null)
        }

        public ConfigFile(String uri, String filename, File file) {
            this(uri, filename, file, null)
        }
        
        public ConfigFile(String uri, String filename, File file, Boolean override) {
            this.uri = name
            this.filename = filename
            this.file = file
            this.override = override
        }
    }

    @ToString(includeNames = true)
    @EqualsAndHashCode(includes = [ "ns" ])
    class Capability {

        final String ns
        String effective
        String filter
        String resolution
        String extra

        Capability(String ns) {
            this.ns = ns
            this.effective = "resolve"
            this.filter = null
            this.resolution = "mandatory";
            this.extra = null
        }

        String format() {
            StringBuilder text = new StringBuilder(ns)
            if (filter) {
                text.append(";filter:='").append(filter).append("'")
            }
            if (effective) {
                text.append(";effective:='").append(effective).append("'")
            }
            if (resolution) {
                text.append(";resolution:='").append(resolution).append("'")
            }
            if (extra) {
                text.append(";").append(extra)
            }

            return text.toString();
        }
    }

    class BundleExtendedDescriptor extends BundleDescriptor {
        protected BundleExtendedDescriptor() {
            super(new DependencyMatcher())
        }

        def match(Closure closure) {
            ConfigureUtil.configure( closure, matcher )
        }

        def match(Map properties) {
            ConfigureUtil.configureByMap( properties, matcher )
        }
    }
}
