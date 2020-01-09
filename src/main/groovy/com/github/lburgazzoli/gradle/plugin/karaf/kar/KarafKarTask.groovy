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

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskTrait
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnProtocolParser
import org.gradle.jvm.tasks.Jar

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * @author lburgazzoli
 */
class KarafKarTask extends Jar implements KarafTaskTrait  {
    public static final String GROUP = 'karaf'
    public static final String NAME = 'generateKar'
    public static final String DESCRIPTION = 'Generates Karaf KAR Archive'
    public static final String EXTENSION = 'kar'

    KarafKarTask() {
        outputs.upToDateWhen {
            false
        }
    }

    @Override
    protected void copy() {
        def karaf = getKaraf()
        if (!karaf.hasKar()) {
            return;
        }

        def features = karaf.features
        def repo = karaf.repo
        def kar = karaf.kar
        Files.createDirectories(kar.outputPath)

        archiveAppendix.set(null)
        archiveClassifier.set(null)
        archiveBaseName.set(features.name)
        archiveVersion.set(features.version)
        archiveExtension.set(EXTENSION)
        destinationDirectory.set(kar.outputDir)

        if (kar.archiveName) {
            archiveVersion.set(null)
            archiveBaseName.set(kar.archiveName)
        }

        from(repo.outputDir)
        into('repository')

        super.copy()
    }
}
