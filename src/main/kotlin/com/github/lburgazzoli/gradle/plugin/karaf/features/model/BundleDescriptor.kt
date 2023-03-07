package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.gradle.api.Action

/**
 * DSL extension allowing instruction on how to produce a `<bundle/>` entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
open class BundleDescriptor protected constructor(val matcher: DependencyMatcher) {
    companion object {
        fun fromPattern(pattern: String): BundleDescriptor =
            BundleDescriptor(DependencyMatcher.from(pattern))
    }

    var remap: Action<in Dependency>? = null
        private set
    var include = true
    var wrap = false
    var attributes = mutableMapOf<String, String>()
    var instructions = mutableMapOf<String, String>()

    fun attribute(key: String, value: String) {
        attributes[key] = value
    }

    fun attributes(instructions: Map<String, String>) {
        attributes.clear()
        attributes.putAll(instructions)
    }

    fun instruction(key: String, value: String) {
        instructions[key] = value
    }

    fun instructions(instructions: Map<String, String>) {
        this.instructions.clear()
        this.instructions.putAll(instructions)
    }

    fun remap(action: Action<in Dependency>) {
        remap = action
    }

    fun hasRemap(): Boolean = remap != null

    fun wrap(action: Action<in WrapInstructionsHelper>) {
        wrap = true
        action.execute(WrapInstructionsHelper())
    }

    inner class WrapInstructionsHelper {
        fun attribute(key: String, value: String) {
            instruction(key, value)
        }

        fun attribute(instructions: Map<String, String>) {
            instruction(instructions)
        }

        fun instruction(key: String, value: String) {
            this@BundleDescriptor.instruction(key, value)
        }

        fun instruction(instructions: Map<String, String>) {
            this@BundleDescriptor.instructions(instructions)
        }
    }
}
