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
package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.tasks.bundling.AbstractArchiveTask

/**
 * @author lburgazzoli
 */
class DependencyDescriptor extends Dependency {

    BundleDescriptor bundle

    DependencyDescriptor(ResolvedComponentResult component, String classifier, String type, File file, BundleDescriptor bundle) {
        super(component, classifier, type, file)
        this.bundle = bundle

        if(bundle && bundle.remap) {
            this.group   = bundle.remap.group ?: this.group
            this.name    = bundle.remap.name ?: this.name
            this.version = bundle.remap.version ?: this.version
            this.type    = bundle.remap.type ?: this.type
            this.file    = bundle.remap.file ?: this.file
            this.kind    = bundle.remap.file ?: this.kind
        }
    }

    // *************************************************************************
    // Helpers
    // *************************************************************************

    static DependencyDescriptor make(
            ResolvedComponentResult componentResult, ResolvedArtifact artifact, BundleDescriptor descriptor) {

        return new DependencyDescriptor(componentResult, artifact.classifier, artifact.type, artifact.file, descriptor)
    }

    static DependencyDescriptor make(
        ResolvedComponentResult componentResult, AbstractArchiveTask task, BundleDescriptor descriptor) {

        return new DependencyDescriptor(componentResult, task.classifier, task.extension, task.archivePath, descriptor)
    }
}
