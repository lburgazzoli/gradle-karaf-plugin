package com.github.lburgazzoli.gradle.plugin.karaf.repo

import com.github.lburgazzoli.gradle.plugin.karaf.karafProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class KarafRepoTaskTest {
    @TempDir
    lateinit var projectDir: File

    private val root: Path by lazy { projectDir.toPath().resolve("repo") }

    private fun task() = karafProject(projectDir).tasks.getByName(KarafRepoTask.NAME) as KarafRepoTask

    @Test
    fun `coordinates map to the maven layout`() {
        assertThat(task().asRepoPath(root, "org.foo", "bar", "1.0", "bar-1.0.jar"))
            .isEqualTo(root.resolve("org/foo/bar/1.0/bar-1.0.jar"))
    }

    @Test
    fun `an empty group adds no directory`() {
        assertThat(task().asRepoPath(root, "", "bar", "1.0", "bar-1.0.jar"))
            .isEqualTo(root.resolve("bar/1.0/bar-1.0.jar"))
    }

    @Test
    fun `a leading slash in a later coordinate stays under the root`() {
        assertThat(task().asRepoPath(root, "org.foo", "bar", "/1.0", "bar-1.0.jar"))
            .isEqualTo(root.resolve("org/foo/bar/1.0/bar-1.0.jar"))
    }

    @Test
    fun `a leading slash in the first coordinate is rejected`() {
        assertThatThrownBy { task().asRepoPath(root, "", "/bar", "1.0", "bar-1.0.jar") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("outside the repository")
    }

    @Test
    fun `dot-dot segments that leave the root are rejected`() {
        assertThatThrownBy { task().asRepoPath(root, "", "../..", "1.0", "x.jar") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("outside the repository")
    }
}
