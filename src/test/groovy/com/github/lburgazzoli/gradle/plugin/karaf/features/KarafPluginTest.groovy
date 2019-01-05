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

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
import com.github.lburgazzoli.gradle.plugin.karaf.KarafTestSupport
import org.gradle.api.Project

/**
 * @author lburgazzoli
 */
class KarafPluginTest extends KarafTestSupport {

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

    def 'Apply plugin'() {
        when:
            configureProject(project) {
                // no-op
            }
        then:
            getKarafExtension(project) instanceof KarafPluginExtension
            getKarafFeaturesTasks(project) instanceof KarafFeaturesTask
    }
}