package com.github.lburgazzoli.gradle.plugin.karaf

import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KarafPluginTest {
    @Test
    fun `plugin apply works`() {
        ProjectBuilder.builder().withName("gradle-karaf").build().run {
            group = "com.lburgazzoli.github"
            version = "1.2.3"
            apply(plugin = "com.github.lburgazzoli.karaf")

            val ext = the<KarafPluginExtension>()
        }
    }
}
