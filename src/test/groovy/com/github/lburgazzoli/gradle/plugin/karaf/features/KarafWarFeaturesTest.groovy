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

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTestSupport
import org.gradle.api.Project

/**
 * @author lburgazzoli
 */
class KarafWarFeaturesTest extends KarafTestSupport {

    Project project

    def setup() {
        project = setUpProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3')
    }

    def cleanup() {
        project?.buildDir?.deleteDir()
    }

    // *************************
    //
    // Test
    //
    // *************************

    def 'War Dependencies'() {
        given:
            configureProject(project) {
                dependencies {
                    compile("org.apache.activemq:activemq-web-console:5.13.2@war") {
                        transitive = false
                    }
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
           karaf.features {
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

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile()
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'

            findAllBundles(featuresXml, 'war:mvn:org.apache.activemq/activemq-web-console/5.13.2/war\\?Webapp-Context=activemq-web-console').size() == 1
    }

    def 'War Dependencies (Project)'() {
        given:
            configureProject(project) {
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

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                feature {
                    includeProject = true
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile()
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            findAllBundles(featuresXml, '^war:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3/war$').size() == 1
            findAllBundles(featuresXml, '^mvn:com.lburgazzoli.github/gradle-karaf/1.2.3$').size() == 0
    }
}
