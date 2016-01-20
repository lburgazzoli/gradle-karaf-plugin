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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.ConfigureUtil

/**
 * @author lburgazzoli
 */
class FeatureDescriptor {
    private final Project project

    String name
    String version
    String description

    private List<Configuration> configurations
    private List<BundleInstructionDescriptor> bundles

    public FeatureDescriptor(Project project, String name) {
        this.project = project
        this.name = name
        this.version = project.version
        this.configurations = []
        this.bundles = []
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

        bundles << descriptor
    }

    def bundle(Closure closure) {
        bundles << ConfigureUtil.configure(
            closure,
            new BundleExtendedInstructionDescriptor()
        )
    }

    public List<BundleInstructionDescriptor> getBundles() {
        return this.bundles
    }
}
