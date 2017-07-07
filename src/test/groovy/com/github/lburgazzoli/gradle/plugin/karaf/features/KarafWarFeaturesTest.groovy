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

import groovy.util.logging.Slf4j

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTestSupport

/**
 * @author lburgazzoli
 */
@Slf4j
class KarafWarFeaturesTest extends KarafTestSupport {

    def 'Simple War Dependencies'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile("org.apache.activemq:activemq-web-console:5.13.2@war") {
                        transitive = false
                    }
                }
            }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                xsdVersion = "1.3.0"

                feature {
                    name = "karaf-features-simple-project"
                    description = "feature-description"
                    details = "my detailed description"

                    bundle("org.apache.activemq:activemq-web-console:5.13.2") {
                        attribute "startLevel", "50"
                        instruction "Webapp-Context", "activemq-web-console"
                    }
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'

            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('war:mvn:org.apache.activemq/activemq-web-console/5.13.2/war?Webapp-Context=activemq-web-console')
            }.size() == 1
    }

    def 'Project War Dependencies'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                apply plugin: 'war'
                apply plugin: 'osgi'

                war {
                    manifest = osgiManifest {
                        instruction 'Web-ContextPath', '/context-path'
                        instruction 'Webapp-Context', '/context-path'
                        instruction 'Bundle-ClassPath', '.;/WEB-INF/classes'
                    }
                }
            }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    includeProject = true
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.bundle.'**'.findAll {
                it.text().equals('war:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3/war')
            }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                it.text().equals('mvn:com.lburgazzoli.github/gradle-karaf/1.2.3')
            }.size() == 0
    }
}
