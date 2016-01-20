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

import com.github.lburgazzoli.gradle.plugin.karaf.AbstractKarafTask
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeaturesDescriptor
import groovy.xml.MarkupBuilder
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber
/**
 * @author lburgazzoli
 */
class KarafFeaturesTask extends AbstractKarafTask {
    public static final String FEATURES_XMLNS_PREFIX = 'http://karaf.apache.org/xmlns/features/v'
    public static final VersionNumber XMLNS_V13 = new  VersionNumber(1, 3, 0, null)
    public static final String NAME = "generateFeatures"

    @TaskAction
    def run() {
        generateFeatures(extension.features)
    }

    protected String generateFeatures(FeaturesDescriptor featuresDescriptor) {
        def writer = new StringWriter()

        def builder = new MarkupBuilder(writer)
        builder.setOmitNullAttributes(true)
        builder.setDoubleQuotes(true)

        def xsdVer13 = VersionNumber.parse(featuresDescriptor.xsdVersion).compareTo(XMLNS_V13) >= 0
        def resolver = featuresDescriptor.resolver

        builder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8", standalone: "yes")
        builder.features(xmlns:FEATURES_XMLNS_PREFIX + featuresDescriptor.xsdVersion, name: featuresDescriptor.name) {
            featuresDescriptor.repositories.each {
                builder.repository( it )
            }
            featuresDescriptor.features.each { feature ->
                builder.feature(name: feature.name, version: feature.version, description: feature.description) {
                    /*
                    feature.dependencyFeatures.each {
                        builder.feature(
                            [
                                version:  it.version,
                                dependency: (it.dependency && xsdVer13) ? true : null
                            ],
                            it.name
                        )
                    }
                    */

                    /*
                    // Render bundle dependencies
                    bundleDefinitionCalculator.calculate(feature, extension, extraBundles).each {
                        builder.bundle(
                            [
                                'dependency' : it.dependency,
                                'start-level': it.startLevel
                            ],
                            it.url
                        )
                    }
                    */
                }
            }
        }

        return writer.toString()
    }
}
