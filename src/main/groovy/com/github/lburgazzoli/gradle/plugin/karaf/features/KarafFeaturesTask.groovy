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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskTrait

/**
 * @author lburgazzoli
 */
class KarafFeaturesTask extends DefaultTask implements KarafTaskTrait  {
    public static final String GROUP = "karaf"
    public static final String NAME = "generateFeatures"
    public static final String DESCRIPTION = "Generates Karaf features file"

    public static final String FEATURES_XMLNS_PREFIX = 'http://karaf.apache.org/xmlns/features/v'
    public static final VersionNumber XMLNS_V13 = new  VersionNumber(1, 3, 0, null)

    public KarafFeaturesTask() {
        // TODO: to be improved
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    def run() {
        if(karaf.hasFeatures()) {
            File outputFile = karaf.features.getOutputFile()

            // write out a features repository xml.
            if(!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }

            def out = new BufferedWriter(new FileWriter(outputFile))
            out.write(generateFeatures(karaf.features))
            out.close()
        }
    }

    String generateFeatures(KarafFeaturesExtension karaf) {

        def builder = new KarafFeaturesBuilder()
        def xsdVer13 = VersionNumber.parse(karaf.xsdVersion).compareTo(XMLNS_V13) >= 0
        def resolver = karaf.resolver

        builder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8", standalone: "yes")
        builder.features(xmlns:FEATURES_XMLNS_PREFIX + karaf.xsdVersion, name: karaf.name) {
            karaf.repositories.each {
                builder.repository( it )
            }
            karaf.featureDescriptors.each { feature ->
                builder.feature(name: feature.name, version: feature.version, description: feature.description) {
                    if(feature.details) {
                        builder.details(feature.details)
                    }

                    feature.configs.each {
                        builder.config(
                            [ name: it.name ],
                            it.content
                        )
                    }

                    feature.configFiles.each {
                        if (it.filename && it.uri) {
                            builder.configfile(
                                [ finalname: it.filename ],
                                it.uri
                            )
                        }
                    }

                    feature.featureDependencies.each {
                        builder.feature(
                            [
                                version:  it.version,
                                dependency: (it.dependency && xsdVer13) ? true : null,
                                prerequisite: it.prerequisite
                            ],
                            it.name
                        )
                    }

                    def dependencies = resolver.resolve(feature)

                    dependencies.each {
                        if(!feature.isConditional(it)) {
                            if (it.bundle) {
                                builder.bundle(it.bundle.attributes, it.url)
                            } else {
                                builder.bundle(it.url)
                            }
                        }
                    }

                    feature.conditions.each { condition ->
                        builder.conditional {
                            if(condition.condition) {
                                builder.condition(condition.condition)
                            }

                            condition.configs.each {
                                builder.config(
                                    [ name: it.name ],
                                    it.content
                                )
                            }

                            feature.configFiles.each {
                                if (it.filename && it.uri) {
                                    builder.configfile(
                                        [ finalname: it.filename ],
                                        it.uri
                                    )
                                }
                            }

                            condition.featureDependencies.each {
                                builder.feature(
                                    [
                                        version:  it.version,
                                        dependency: (it.dependency && xsdVer13) ? true : null,
                                        prerequisite: it.prerequisite
                                    ],
                                    it.name
                                )
                            }

                            dependencies.each { dependency ->
                                condition.bundleDescriptors.each {
                                    if(it.matches(dependency)) {
                                        if (dependency.bundle) {
                                            builder.bundle(dependency.bundle.attributes, dependency.url)
                                        } else {
                                            builder.bundle(dependency.url)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (feature.capabilities) {
                        builder.capability(
                            feature.capabilities.collect { it.format() }.join(',')
                        )
                    }
                }
            }
        }

        return builder.toString()
    }
}
