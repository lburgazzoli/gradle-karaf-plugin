package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesExtension
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarExtension
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
abstract class KarafPluginExtension(val project: Project) {
    companion object {
        const val NAME = "karaf"
    }

    @get:Inject
    protected abstract val objects: ObjectFactory

    val features: KarafFeaturesExtension =
        (this as ExtensionAware).extensions.create(KarafFeaturesExtension.NAME, project)
    val repo: KarafRepoExtension =
        (this as ExtensionAware).extensions.create(KarafRepoExtension.NAME)
    val kar: KarafKarExtension =
        (this as ExtensionAware).extensions.create(KarafKarExtension.NAME)

    fun hasFeatures(): Boolean = features.featureDescriptors.isNotEmpty()

    fun repo(action: Action<in KarafRepoExtension>) {
        action.execute(repo)
        repo.enabled.set(true)
    }

    fun hasRepo(): Boolean = repo.enabled.get()

    fun kar(action: Action<in KarafKarExtension>) {
        action.execute(kar)
        kar.enabled.set(true)
    }

    fun hasKar(): Boolean = kar.enabled.get()
}
