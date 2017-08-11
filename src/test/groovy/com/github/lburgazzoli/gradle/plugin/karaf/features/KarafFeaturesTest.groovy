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
import org.gradle.testfixtures.ProjectBuilder

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafTestSupport

/**
 * @author lburgazzoli
 */
@Slf4j
class KarafFeaturesTest extends KarafTestSupport {

    // *************************************************************************
    //
    // *************************************************************************

    def 'Apply plugin'() {
        given:
            def project = ProjectBuilder.builder().build()
        when:
            setupProject(project)
        then:
            getKarafExtension(project) instanceof KarafPluginExtension
            getKarafFeaturesTasks(project) instanceof KarafFeaturesTask
    }

    // *************************************************************************
    //
    // *************************************************************************

    def 'Simple Single Project Setup'() {
        given:
            def project = setupProject(ProjectBuilder.builder().build())
            project.group = 'com.lburgazzoli.github'
            project.version = '1.2.3'
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
            feature.configurations.empty == false
            feature.configurations.size() == 1
            feature.configurations[0] == project.configurations.runtime
    }

    def 'Recursive Dependency'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile 'com.jcabi:jcabi-github:0.28'
                }
            }
            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }
        then:
            task.generateFeatures(extension.features)
            extension != null
            extension.features != null
            extension.features.featureDescriptors != null
            extension.features.featureDescriptors.empty == false
            extension.features.featureDescriptors.size() == 1
    }

    def 'Same GAV'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile group      : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            classifier : 'classes'
                            //ext      : 'jar', // adding ext cause the resolution to fail
                    compile group      : 'ca.uhn.hapi.fhir',
                            name       : 'hapi-fhir-testpage-overlay',
                            version    : '2.1',
                            ext        : 'war'
                }
            }
            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/jar/classes')
            }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war')
            }.size() == 1
    }

    def 'Same GAV and reset type'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile group  : 'ca.uhn.hapi.fhir',
                        name       : 'hapi-fhir-testpage-overlay',
                        version    : '2.1',
                        classifier : 'classes',
                        transitive : true
                    //ext      : 'jar', // adding ext cause the resolution to fail
                    compile group      : 'ca.uhn.hapi.fhir',
                        name       : 'hapi-fhir-testpage-overlay',
                        version    : '2.1',
                        ext        : 'war',
                        transitive : true
                }
            }
            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"

                    bundle ('ca.uhn.hapi.fhir:hapi-fhir-testpage-overlay:2.1:classes') {
                        remap {
                            type = null
                        }
                    }
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1//classes')
            }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('war:mvn:ca.uhn.hapi.fhir/hapi-fhir-testpage-overlay/2.1/war')
            }.size() == 1
    }

    def 'Multi version deps'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile 'com.graphql-java:graphql-java-servlet:0.9.0'
                    compile 'com.google.guava:guava:20.0'
                }
            }
            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('mvn:com.google.guava/guava/20.0')
            }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                it.text().contains('mvn:com.google.guava/guava/19.0')
            }.size() == 0
    }

    def 'Simple Single Project Dependencies'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    runtime "com.google.guava:guava:19.0"
                    runtime "com.squareup.retrofit:retrofit:1.9.0"
                    runtime "com.squareup.retrofit:converter-jackson:1.9.0"
                    compile "org.apache.activemq:activemq-web:5.13.2"
                    compile "org.apache.activemq:activemq-web-console:5.13.2@war"
                    compile "commons-codec:commons-codec:1.10"
                    compile "commons-collections:commons-collections:3.2.2"
                    compile "commons-fileupload:commons-fileupload:1.3.2"
                    compile "commons-io:commons-io:2.5"
                    compile "commons-lang:commons-lang:2.6"
                }
            }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
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

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'
            featuresXml.feature.feature[0].text() == 'dependencyFeatureName1'
            featuresXml.feature.feature[1].text() == 'dependencyFeatureName2'
            featuresXml.feature.feature[1].@version == '5.6.7'
            featuresXml.feature.feature[1].@dependency == 'true'

            featuresStr.contains("xmlns=\"http://karaf.apache.org/xmlns/features/v1.3.0\"") == true

            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.google.guava/guava/19.0')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.google.code.gson/gson/2.3.1')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.squareup.retrofit/retrofit/1.9.0')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.squareup.retrofit/converter-jackson/1.9.0')
                }.size() == 0
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:commons-') && it.@dependency == "true"
                }.size() == 2
    }

    def 'Simple Single Project With ConfigFile'() {
        given:
        def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
            configurations {
                hazelcast
            }
            dependencies {
                hazelcast 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                hazelcast 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                hazelcast 'com.hazelcast:hazelcast-all:3.6.1'
            }
        }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
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

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname == '/etc/hazelcast.xml' && !it.attributes().containsKey('overrides')
                }.size() == 1
    }

    def 'Simple Single Project With ConfigFile Override'() {
        given:
        def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
            configurations {
                hazelcast
            }
            dependencies {
                hazelcast 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                hazelcast 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                hazelcast 'com.hazelcast:hazelcast-all:3.6.1'
            }
        }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
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

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname?.equals('/etc/hazelcast-1.xml') && it.@override == true
                }.size() == 1

            featuresXml.feature.configfile.'**'.findAll {
                    it.@finalname?.equals('/etc/hazelcast-2.xml') && it.@override == false
                }.size() == 1
    }

    def 'Simple Single Project With Conditions'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile "com.google.guava:guava:19.0"
                    compile "com.squareup.retrofit:retrofit:1.9.0"

                    compile 'com.fasterxml.jackson.core:jackson-core:2.7.0'
                    compile 'com.fasterxml.jackson.core:jackson-databind:2.7.0'
                    compile 'com.fasterxml.jackson.core:jackson-annotations:2.7.0'
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
                    includeProject = false

                    conditional('json-p') {
                        feature 'myfeature-1'
                        feature 'myfeature-2'
                        bundle 'com.fasterxml.jackson.core'
                    }
                }
            }

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr
    }

    def 'Simple Single Project With Capabilities'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile "com.google.guava:guava:19.0"
                    compile "com.squareup.retrofit:retrofit:1.9.0"

                    compile 'com.fasterxml.jackson.core:jackson-core:2.7.0'
                    compile 'com.fasterxml.jackson.core:jackson-databind:2.7.0'
                    compile 'com.fasterxml.jackson.core:jackson-annotations:2.7.0'
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

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

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

    def 'Simple Single Project Wit Wrap'() {
        given:
            def project = setupProject('com.lburgazzoli.github', 'gradle-karaf', '1.2.3') {
                dependencies {
                    compile 'org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1'
                    compile 'com.eclipsesource.minimal-json:minimal-json:0.9.2'
                    compile 'com.hazelcast:hazelcast-all:3.6.1'
                }
            }

            def task = getKarafFeaturesTasks(project)
        when:
            def extension = getKarafExtension(project)
            extension.features {
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

            def featuresStr = task.generateFeatures(extension.features)
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            println featuresStr

            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('wrap:mvn:com.eclipsesource.minimal-json/minimal-json/0.9.2$Bundle-SymbolicName=my-bundle-name')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.hazelcast/hazelcast-all/3.6.1')
                }.size() == 1
    }
}
