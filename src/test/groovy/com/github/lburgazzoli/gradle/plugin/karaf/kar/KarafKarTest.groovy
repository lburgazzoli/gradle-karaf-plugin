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
package com.github.lburgazzoli.gradle.plugin.karaf.kar

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTestSupport
import org.gradle.api.Project
import spock.lang.Ignore

import java.util.zip.ZipFile
/**
 * @author lburgazzoli
 */
class KarafKarTest extends KarafTestSupport {

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

    @Ignore("Fails with 'Cannot change role of dependency configuration ':implementation' after it has been resolved'")
    def 'Simple Kar Project'() {
        given:
            configureProject(project) {
                dependencies {
                    implementation "commons-lang:commons-lang:2.6"
                }
            }

            def karaf = getKarafExtension(project)
            def features = getKarafFeaturesTasks(project)
            def repo = getKarafRepoTasks(project)
            def kar = getKarafKarTasks(project)
        when:
            karaf.features {
                feature {
                    name = "feature-1"
                    description = "my feature n1"
                }
            }
            karaf.kar {
            }
        then:
            features.run()
            repo.run()
            kar.copy()

            def baseName = kar.archiveBaseName.get()
            def ext = kar.archiveExtension.get()

            baseName == project.name
            ext == KarafKarTask.EXTENSION

            def archive = kar.archiveFile.get().asFile
            def zf = new ZipFile(archive)
            null != zf.getEntry("repository/com/lburgazzoli/github/${project.name}/${project.version}/${project.name}-${project.version}-features.xml")
            null != zf.getEntry("repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar")
    }
}
