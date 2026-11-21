package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesExtension
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarExtension
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoExtension
import org.gradle.api.Action
import org.gradle.api.Project
import javax.inject.Inject

/**
 * @author lburgazzoli
 */
open class KarafPluginExtension @Inject constructor(val project: Project) {
    companion object {
        const val NAME = "karaf"

        @JvmStatic
        fun lookup(project: Project): KarafPluginExtension? =
            project.extensions.findByName(NAME) as KarafPluginExtension?
    }

    val features: KarafFeaturesExtension =
        project.objects.newInstance(KarafFeaturesExtension::class.java, project)
    val repo: KarafRepoExtension =
        project.objects.newInstance(KarafRepoExtension::class.java, project)
    val kar: KarafKarExtension =
        project.objects.newInstance(KarafKarExtension::class.java, project)

    fun features(action: Action<in KarafFeaturesExtension>) {
        action.execute(features)
    }

    fun hasFeatures(): Boolean = features.featureDescriptors.isNotEmpty()

    fun repo(action: Action<in KarafRepoExtension>) {
        repo.enabled = true
        action.execute(repo)
    }

    fun hasRepo(): Boolean = repo.enabled

    fun kar(action: Action<in KarafKarExtension>) {
        kar.enabled = true
        action.execute(kar)
    }

    fun hasKar(): Boolean = kar.enabled
}
