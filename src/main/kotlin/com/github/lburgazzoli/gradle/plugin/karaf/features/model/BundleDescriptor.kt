package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.gradle.api.Action
import java.io.File
import javax.inject.Inject

/**
 * DSL extension allowing instruction on how to produce a `<bundle/>` entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
open class BundleDescriptor @Inject constructor(val matcher: DependencyMatcher) {
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
        attributes.putAll(textMap(instructions))
    }

    fun instruction(key: String, value: String) {
        instructions[key] = value
    }

    fun instructions(instructions: Map<String, String>) {
        this.instructions.clear()
        this.instructions.putAll(textMap(instructions))
    }

    fun remap(action: Action<in Dependency>) {
        remap = action
    }

    /**
     * Sets the properties of each matched [Dependency] from [properties], for example
     * `remap type: 'jar'`. The keys are `group`, `name`, `version`, `type`, `classifier`, and `file`, whose value is
     * a [File].
     *
     * @throws IllegalArgumentException if [properties] has any other key
     */
    fun remap(properties: Map<String, Any?>) {
        val properties = textKeys(properties)
        val unknown = properties.keys - setOf("group", "name", "version", "type", "classifier", "file")
        require(unknown.isEmpty()) {
            "remap supports group, name, version, type, classifier, and file; got $unknown"
        }
        val file = properties["file"]
        require(file == null || file is File) {
            "remap expects a java.io.File for file; got ${file!!::class.java.name}"
        }
        remap = Action<Dependency> {
            properties.forEach { (key, value) ->
                val text = value?.toString()
                when (key) {
                    "group" -> group = text
                    "name" -> name = text
                    "version" -> version = text
                    "type" -> type = text
                    "classifier" -> classifier = text
                    "file" -> this.file = value as File?
                }
            }
        }
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

/**
 * Returns [map] with each key and value converted by `toString()`. A Groovy map literal with an interpolated key or
 * value holds a GString where the declared type says String, so the entries are read without that type.
 */
internal fun textMap(map: Map<String, String>): Map<String, String> =
    (map as Map<*, *>).entries.associate { it.key.toString() to it.value.toString() }

/**
 * Returns [map] with each key converted by `toString()`; see [textMap].
 */
internal fun textKeys(map: Map<String, Any?>): Map<String, Any?> =
    (map as Map<*, *>).entries.associate { it.key.toString() to it.value }
