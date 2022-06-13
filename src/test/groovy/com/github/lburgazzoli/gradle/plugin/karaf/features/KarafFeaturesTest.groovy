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
class KarafFeaturesTest extends KarafTestSupport {

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

    def 'Single Project Setup'() {
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }
        then:
            extension != null
            extension.features != null
            extension.features.featureDescriptors != null
            extension.features.featureDescriptors.empty == false
            extension.features.featureDescriptors.size() == 1

            def feature = extension.features.featureDescriptors[0]
            feature.configurations.get().empty == false
            feature.configurations.get().size() == 1
            feature.configurations.get()[0] == project.configurations.runtimeClasspath
    }

    def 'Same GAV'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation group      : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            classifier : 'classes'
                    implementation group      : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            ext        : 'war'
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            println featuresFile.text

            findAllBundles(featuresXml, 'wrap:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/jar/classes').size() == 1
            findAllBundles(featuresXml, 'war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war').size() == 1
    }

    def 'Same GAV and reset type'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation group  : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            classifier : 'classes',
                            transitive : true
                    implementation group      : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            ext        : 'war',
                            transitive : true
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"

                    bundle ('ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1:classes') {
                        wrap = true

                        remap {
                            // Note: this does not run inside "full Gradle",
                            // so there's no automatic Action -> Closure conversion
                            it.type = null
                        }
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            findAllBundles(featuresXml, 'wrap:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1//classes').size() == 1
            findAllBundles(featuresXml, 'war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war').size() == 1
    }

    def 'Multi version deps'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation 'com.graphql-java:graphql-java-servlet:0.9.0'
                    implementation 'com.google.guava:guava:20.0'
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            findAllBundles(featuresXml, 'mvn:com.google.guava/guava/20.0').size() == 1
            findAllBundles(featuresXml, 'mvn:com.google.guava/guava/19.0').size() == 0
    }

    def 'Single Project Dependencies'() {
        given:
            configureProject(project) {
                dependencies {
                    runtimeOnly "com.google.guava:guava:19.0"
                    runtimeOnly "com.squareup.retrofit:retrofit:1.9.0"
                    runtimeOnly "com.squareup.retrofit:converter-jackson:1.9.0"
                    implementation "org.apache.activemq:activemq-web:5.13.2"
                    implementation "org.apache.activemq:activemq-web-console:5.13.2@war"
                    implementation "commons-codec:commons-codec:1.10"
                    implementation "commons-collections:commons-collections:3.2.2"
                    implementation "commons-fileupload:commons-fileupload:1.3.2"
                    implementation "commons-io:commons-io:2.5"
                    implementation "commons-lang:commons-lang:2.6"
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                xsdVersion = "1.3.0"
                repository "mvn:org.apache.karaf.cellar/apache-karaf-cellar/4.0.0/xml/features"
                repository "mvn:org.apache.karaf.features/standard/4.0.0/xml/features"

                feature {
                    name = "karaf-features-simple-project"
                    description = "feature-description"
                    details = "my detailed description"
                    includeProject = true

                    configFile {
                        filename = "/etc/org.apache.karaf.cellar.groups.cfg"
                        uri      = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/cfg/groups"
                    }

                    feature 'dependencyFeatureName1'
                    feature('dependencyFeatureName2') {
                        version = "5.6.7"
                        dependency = true
                    }
                    feature('dependencyFeatureName3') {
                        prerequisite = true
                    }

                    bundle('com.squareup.retrofit:converter-jackson') {
                        include = false
                    }

                    bundle('commons-*:*-c*') {
                        attribute 'dependency', 'true'
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            println featuresFile.text

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'
            featuresXml.feature.feature[0].text() == 'dependencyFeatureName1'
            featuresXml.feature.feature[1].text() == 'dependencyFeatureName2'
            featuresXml.feature.feature[1].@version == '5.6.7'
            featuresXml.feature.feature[1].@dependency == 'true'

            featuresFile.text.contains("xmlns=\"http://karaf.apache.org/xmlns/features/v1.3.0\"") == true

            findAllBundles(featuresXml, 'wrap:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3').size() == 1
            findAllBundles(featuresXml, 'mvn:com.google.guava/guava/19.0').size() == 1
            findAllBundles(featuresXml, 'mvn:com.google.code.gson/gson/2.3.1').size() == 1
            findAllBundles(featuresXml, '^*.mvn:com.squareup.retrofit/retrofit/1.9.0$').size() == 1
            findAllBundles(featuresXml, '^*.mvn:com.squareup.retrofit/converter-jackson/1.9.0$').size() == 0
            findAllBundles(featuresXml, { it.text().contains('mvn:commons-') && it.@dependency == "true" }).size() == 2
    }

    def 'Single Project With ConfigFile'() {
        given:
            configureProject(project) {
                configurations {
                    hazelcast
                }
                dependencies {
                    hazelcast 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                    hazelcast 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                    hazelcast 'com.hazelcast:hazelcast-all:3.6.1'
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                xsdVersion = "1.3.0"
                feature {
                    name        = 'hazelcast'
                    description = 'In memory data grid'

                    configurations {
                        add 'hazelcast'
                    }

                    configFile {
                        filename = "/etc/hazelcast.xml"
                        uri      = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname == '/etc/hazelcast.xml' && !it.attributes().containsKey('overrides')
                }.size() == 1
    }

    def 'Single Project With ConfigFile Override'() {
        given:
            configureProject(project) {
                configurations {
                    hazelcast
                }
                dependencies {
                    hazelcast 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                    hazelcast 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                    hazelcast 'com.hazelcast:hazelcast-all:3.6.1'
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                xsdVersion = "1.3.0"
                feature {
                    name        = 'hazelcast'
                    description = 'In memory data grid'

                    configurations {
                        add 'hazelcast'
                    }

                    configFile {
                        filename = "/etc/hazelcast-1.xml"
                        uri      = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                        override = true
                    }

                    configFile {
                        filename = "/etc/hazelcast-2.xml"
                        uri      = "mvn:org.apache.karaf.cellar/apache-karaf-cellar/${project.version}/xml/hazelcast"
                        override = false
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname?.equals('/etc/hazelcast-1.xml') && it.@override == true
                }.size() == 1

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname?.equals('/etc/hazelcast-2.xml') && it.@override == false
                }.size() == 1
    }

    def 'Single Project With Conditions'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation "com.google.guava:guava:19.0"
                    implementation "com.squareup.retrofit:retrofit:1.9.0"

                    implementation 'com.fasterxml.jackson.core:jackson-core:2.7.0'
                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.7.0'
                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.7.0'
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
                    includeProject = false

                    conditional('json-p') {
                        feature 'myfeature-1'
                        feature 'myfeature-2'
                        bundle 'com.fasterxml.jackson.core'
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null
    }

    def 'Single Project With Capabilities'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation "com.google.guava:guava:19.0"
                    implementation "com.squareup.retrofit:retrofit:1.9.0"

                    implementation 'com.fasterxml.jackson.core:jackson-core:2.7.0'
                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.7.0'
                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.7.0'
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
                    includeProject = false

                    capability('osgi.extender') {
                        effective = 'active'
                        filter    = '(&(osgi.extender=osgi.enroute.configurer)${frange;1.2.3})'
                        resolution = 'effective'
                        extra      = 'someExtraStuffs'
                    }
                    capability('osgi.service') {
                        effective = 'active'
                        filter    = '(&(osgi.extender=osgi.enroute.configurer)${frange;1.2.3})'
                    }
                    capability('osgi.service') {
                        effective = 'active'
                        extra    = 'objectClass=javax.jms.ConnectionFactory'
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.capability.'**'.findAll {
                    it.text().contains('osgi.extender;filter')
                }.size() == 1
            featuresXml.feature.capability.'**'.findAll {
                    it.text().contains('osgi.service;filter')
                }.size() == 1
            featuresXml.feature.capability.'**'.findAll {
                it.text().contains('osgi.service;effective:=\'active\';resolution:=\'mandatory\';objectClass=javax.jms.ConnectionFactory')
            }.size() == 1
    }

    def 'Single Project With Wrap'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                    implementation 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                    implementation 'com.hazelcast:hazelcast-all:3.6.1'
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                xsdVersion = "1.3.0"
                feature {
                    name        = 'hazelcast'
                    description = 'In memory data grid'

                    bundle('com.eclipsesource.minimal-json:minimal-json:0.9.2') {
                        wrap {
                            instruction 'Bundle-SymbolicName', 'my-bundle-name'
                        }
                    }
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            findAllBundles(featuresXml, 'wrap:mvn:com.eclipsesource.minimal-json/minimal-json/0.9.2\\$Bundle-SymbolicName=my-bundle-name').size() == 1
            findAllBundles(featuresXml, 'mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1').size() == 1
            findAllBundles(featuresXml, 'mvn:com.hazelcast/hazelcast-all/3.6.1').size() == 1
    }

    def 'Single Project With Multiple features'() {
        given:
            configureProject(project) {
                configurations {
                    hazelcast
                    squareup
                }
                dependencies {
                    hazelcast 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                    hazelcast 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                    hazelcast 'com.hazelcast:hazelcast-all:3.6.1'

                    squareup "com.squareup.retrofit:retrofit:1.9.0"
                    squareup "com.squareup.retrofit:converter-jackson:1.9.0"
                }
            }

            def karaf = getKarafExtension(project)
            def task = getKarafFeaturesTasks(project)
        when:
            karaf.features {
                xsdVersion = "1.3.0"
                feature {
                    name           = 'hazelcast'
                    description    = 'In memory data grid'
                    includeProject = false

                    configurations 'hazelcast'
                }

                feature {
                    name           = 'squareup'
                    description    = 'Squareup'
                    includeProject = true

                    configurations 'squareup' 
                }
            }

            task.run()
        then:
            assert karaf.hasFeatures()

            def featuresFile = karaf.features.getOutputFile().get().asFile
            featuresFile != null

            def featuresXml = new XmlSlurper().parse(featuresFile)
            featuresXml != null

            featuresXml.feature.'**'.find { it.@name == 'hazelcast'}.bundle.'**'.findAll {
                    it.text().contains('wrap:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3')
                }.size() == 0
            featuresXml.feature.'**'.find { it.@name == 'squareup'}.bundle.'**'.findAll {
                    it.text().contains('wrap:mvn:com.lburgazzoli.github/gradle-karaf/1.2.3')
                }.size() == 1
    }
}
