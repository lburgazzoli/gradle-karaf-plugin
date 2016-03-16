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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.jvm.tasks.Jar

import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension

/**
 * @author lburgazzoli
 */
class KarafKarTask extends Jar {
    public static final String GROUP = 'karaf'
    public static final String NAME = 'generateKar'
    public static final String DESCRIPTION = 'Generates Karaf KAR Archive'
    public static final String EXTENSION = 'kar'

    public KarafKarTask() {
        // TODO: to be improved
        outputs.upToDateWhen {
            false
        }
    }

    @Override
    protected void copy() {
        def ext = KarafPluginExtension.lookup(project)
        if (!ext.hasKar()) {
            return;
        }

        def features = ext.features
        def kar = ext.kar
        def resolver = features.resolver

        Path root = kar.repositoryPath
        String rootPath = root.toAbsolutePath().toString();
        if(!Files.exists(root)) {
            Files.createDirectories(root)
        }

        features.featureDescriptors.each { feature ->
            resolver.resolve(feature).each {
                def path = "${it.group}/${it.name}/${it.version}"
                def name = "${it.name}-${it.version}.${it.type}"

                copy(it.file.toPath(), Paths.get(rootPath, "${path}/${name}"))
            }
        }

        def path = "${features.project.group}/${features.name}/${features.project.version}"
        def name = "${features.name}-${features.project.version}.xml"

        copy(features.outputPath, Paths.get(rootPath, "${path}/${name}"))

        baseName = features.name
        version = features.project.version
        extension = EXTENSION
        destinationDir = kar.outputDir

        from(kar.repositoryDir)

        super.copy()
    }

    def copy(Path source, Path destination) {
        if(source) {
            if (!Files.exists(destination.parent)) {
                Files.createDirectories(destination.parent)
            }

            if (!Files.exists(destination)) {
                Files.copy(source, destination)
            }
        }
    }
}
