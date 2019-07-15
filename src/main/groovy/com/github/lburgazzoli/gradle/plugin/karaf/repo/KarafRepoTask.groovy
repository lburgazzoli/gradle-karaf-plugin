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
package com.github.lburgazzoli.gradle.plugin.karaf.repo

import com.github.lburgazzoli.gradle.plugin.karaf.KarafTaskTrait
import com.github.lburgazzoli.gradle.plugin.karaf.mvn.MvnProtocolParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * @author realPyR3X
 */
class KarafRepoTask extends DefaultTask implements KarafTaskTrait  {
    public static final String GROUP = 'karaf'
    public static final String NAME = 'generateRepo'
    public static final String DESCRIPTION = 'Generates Karaf Repo Directory'

    KarafRepoTask() {
        outputs.upToDateWhen {
            false
        }
    }

    @TaskAction
    void run() {
        def karaf = getKaraf()
        if (!karaf.hasRepo()) {
            return;
        }

        def features = karaf.features
        def repo = karaf.repo
        def resolver = features.resolver
        def root = repo.outputPath

        root.deleteDir()
        Files.createDirectories(root)

        features.featureDescriptors.each { feature ->
            resolver.resolve(feature).each {
                copy(
                    it.file,
                    asRepoPath(
                        root,
                        "${it.group.replaceAll("\\.", "/")}/${it.name}/${it.version}",
                        it.hasClassifier() && it.classifier
                            ? "${it.name}-${it.version}-${it.classifier}.${it.type}"
                            : "${it.name}-${it.version}.${it.type}"
                    )
                )
            }

            feature.configFiles.each {
                if (it.filename && it.uri && it.file) {
                    def dep = MvnProtocolParser.parse(it.uri)
                    if (dep) {
                        copy(
                            it.file,
                            asRepoPath(
                                root,
                                "${dep.group.replaceAll("\\.", "/")}/${dep.name}/${dep.version}",
                                dep.hasClassifier()
                                    ? "${dep.name}-${dep.version}-${dep.classifier}.${dep.type}"
                                    : "${dep.name}-${dep.version}.${dep.type}"
                            )
                        )
                    }
                }
            }
        }

        copy(
            features.outputPath,
            asRepoPath(
                root,
                "${features.group.replaceAll("\\.", "/")}/${features.name}/${features.version}",
                "${features.name}-${features.version}-features.xml"
            )
        )
    }

    def copy(File source, Path destination) {
        if (source && destination) {
            copy(source.toPath(), destination)
        }
    }

    def copy(File source, File destination) {
        if (source && destination) {
            copy(source.toPath(), destination.toPath())
        }
    }

    def copy(Path source, Path destination) {
        if (source) {
            if (!Files.exists(destination.parent)) {
                Files.createDirectories(destination.parent)
            }

            Files.copy(
                source,
                destination,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
        }
    }

    def asRepoPath(Path root, String path, String name) {
        return root.resolve(path).resolve(name)
    }
}
