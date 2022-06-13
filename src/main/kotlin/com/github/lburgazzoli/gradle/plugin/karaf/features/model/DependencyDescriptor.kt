package com.github.lburgazzoli.gradle.plugin.karaf.features.model;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.File;

/**
 * @author lburgazzoli
 */
data class DependencyDescriptor(
    val dependency: Dependency,
    val bundle: BundleDescriptor?
) {
    companion object {
        fun make(component: ResolvedComponentResult, artifact: ResolvedArtifact, descriptor: BundleDescriptor?) =
            DependencyDescriptor(
                component = component,
                classifier = artifact.classifier,
                type = artifact.type,
                file = artifact.file,
                bundle = descriptor
            )

        fun make(component: ResolvedComponentResult, task: AbstractArchiveTask, descriptor: BundleDescriptor?) =
            DependencyDescriptor(
                component = component,
                classifier = task.archiveClassifier.orNull,
                type = task.archiveExtension.orNull,
                file = task.archiveFile.get().asFile,
                bundle = descriptor
            )
    }

    constructor(
        component: ResolvedComponentResult, classifier: String?, type: String?, file: File, bundle: BundleDescriptor?
    ) : this(
        dependency = Dependency(component = component, classifier = classifier, type = type, file = file),
        bundle = bundle
    ) {
        bundle?.remap?.execute(dependency)
    }
}
