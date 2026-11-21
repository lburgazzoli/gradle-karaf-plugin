package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CapabilityTest {
    @Test
    fun `empty directives are left out`() {
        val capability = FeatureDefinition.Capability("osgi.service").apply {
            filter = ""
            effective = ""
            resolution = ""
            extra = ""
        }

        assertThat(capability.format()).isEqualTo("osgi.service")
    }

    @Test
    fun `non-empty directives are written in order`() {
        val capability = FeatureDefinition.Capability("osgi.service").apply {
            filter = "(a=b)"
            extra = "x=y"
        }

        assertThat(capability.format())
            .isEqualTo("osgi.service;filter:='(a=b)';effective:='resolve';resolution:='mandatory';x=y")
    }
}
