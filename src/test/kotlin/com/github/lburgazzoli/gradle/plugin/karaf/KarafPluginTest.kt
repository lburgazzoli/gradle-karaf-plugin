package com.github.lburgazzoli.gradle.plugin.karaf

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KarafPluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `applying the plugin registers the extension and the three tasks`() {
        val project = karafProject(projectDir)

        assertSoftly {
            it.assertThat(project.extensions.findByName(KarafPluginExtension.NAME))
                .isInstanceOf(KarafPluginExtension::class.java)
            it.assertThat(project.tasks.findByName(KarafFeaturesTask.NAME)).isInstanceOf(KarafFeaturesTask::class.java)
            it.assertThat(project.tasks.findByName(KarafRepoTask.NAME)).isInstanceOf(KarafRepoTask::class.java)
            it.assertThat(project.tasks.findByName(KarafKarTask.NAME)).isInstanceOf(KarafKarTask::class.java)
        }
    }

    @Test
    fun `an enabled flag set inside the repo or kar block wins over the block's default`() {
        val project = karafProject(projectDir)

        project.karaf.repo { enabled = false }
        project.karaf.kar { enabled = false }

        assertSoftly {
            it.assertThat(project.karaf.hasRepo()).`as`("hasRepo() after repo { enabled = false }").isFalse()
            it.assertThat(project.karaf.hasKar()).`as`("hasKar() after kar { enabled = false }").isFalse()
        }
    }

    @Test
    fun `an empty repo or kar block enables it`() {
        val project = karafProject(projectDir)

        project.karaf.repo { }
        project.karaf.kar { }

        assertSoftly {
            it.assertThat(project.karaf.hasRepo()).`as`("hasRepo() after repo { }").isTrue()
            it.assertThat(project.karaf.hasKar()).`as`("hasKar() after kar { }").isTrue()
        }
    }
}
