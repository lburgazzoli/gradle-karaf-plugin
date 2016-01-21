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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile

/**
 * @author lburgazzoli
 */
class FeaturesDescriptor {
    public static final String DEFAULT_XSD_VERSION = "1.2.0"

    private final Project project
    private final DependencyResolver resolver
    private final def Set<String> repositories
    private final NamedDomainObjectContainer<FeatureDescriptor> features;

    String name
    String xsdVersion

    @OutputFile
    private File outputFile

    FeaturesDescriptor(Project project) {
        this.project = project
        this.name = project.name
        this.resolver = new DependencyResolverMvn()
        this.xsdVersion = DEFAULT_XSD_VERSION
        this.repositories = []
        this.outputFile = null

        // Create a dynamic container for FeatureDescriptor definitions by the user
        this.features = project.container(
            FeatureDescriptor,
            new FeatureDescriptorFactory( project )
        )
    }

    def feature(Closure closure) {
        features.configure( closure )
    }

    def getFeatures() {
        return features
    }

    def DependencyResolver getResolver() {
        return this.resolver
    }

    void setOutputFile(String file) {
        this.outputFile = new File(file)
    }

    void setOutputFile(File file) {
        this.outputFile = file
    }

    File getOutputFile() {
        if(outputFile == null) {
            def path = "${project.buildDir}/karaf-feautures"
            def name = "${name}-${project.version}.xml"

            return new File(path, name)
        }

        return outputFile
    }
}
