package com.github.lburgazzoli.gradle.plugin.karaf.kar

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.karaf
import com.github.lburgazzoli.gradle.plugin.karaf.karafProject
import com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class KarafKarTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `the kar holds the features file and the dependency jars in Maven layout`() {
        val project = karafProject(projectDir) {
            dependencies.add("implementation", "commons-lang:commons-lang:2.6")
        }
        project.karaf.features.feature {
            name = "feature-1"
            description = "my feature n1"
        }
        project.karaf.kar { }
        val kar = project.tasks.getByName(KarafKarTask.NAME) as KarafKarTask

        (project.tasks.getByName(KarafFeaturesTask.NAME) as KarafFeaturesTask).run()
        (project.tasks.getByName(KarafRepoTask.NAME) as KarafRepoTask).run()
        kar.actions.forEach { it.execute(kar) }

        val files = ZipFile(kar.archiveFile.get().asFile).use { zip ->
            zip.entries().toList().filter { !it.isDirectory }.map { it.name }
        }
        assertSoftly {
            it.assertThat(kar.archiveBaseName.get()).isEqualTo(project.name)
            it.assertThat(kar.archiveExtension.get()).isEqualTo(KarafKarTask.EXTENSION)
            it.assertThat(files).contains(
                "repository/com/lburgazzoli/github/gradle-karaf/1.2.3/gradle-karaf-1.2.3-features.xml",
                "repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
            )
        }
    }
}
