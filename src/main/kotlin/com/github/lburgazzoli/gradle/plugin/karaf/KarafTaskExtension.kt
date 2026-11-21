package com.github.lburgazzoli.gradle.plugin.karaf

import org.gradle.api.Action
import org.gradle.api.Task

/**
 * The [KarafPluginExtension] a Karaf task reads: the one set on the task, or else the project's.
 */
internal class KarafTaskExtension(private val task: Task) {
    private var override: KarafPluginExtension? = null

    val extension: KarafPluginExtension
        get() = override ?: task.project.extensions.getByType(KarafPluginExtension::class.java)

    fun set(extension: KarafPluginExtension) {
        override = extension
    }

    /**
     * Sets a new extension, separate from the project's, configured by [action].
     */
    fun set(action: Action<in KarafPluginExtension>) {
        val project = task.project
        override = project.objects.newInstance(KarafPluginExtension::class.java, project).also { action.execute(it) }
    }
}
