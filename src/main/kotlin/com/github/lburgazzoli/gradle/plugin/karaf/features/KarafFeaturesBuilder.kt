package com.github.lburgazzoli.gradle.plugin.karaf.features

/**
 * Builds a features XML document: two-space indentation, double-quoted attributes, and
 * `<name ... />` for an element with no text and no children.
 *
 * An attribute with a `null` value is left out.
 */
class KarafFeaturesBuilder {
    private class Node(
        val name: String,
        val attributes: Map<String, Any?>,
        val text: Any?
    ) {
        val children = mutableListOf<Node>()
    }

    private val roots = mutableListOf<Node>()
    private var current: Node? = null

    /**
     * Adds an element under the element whose [body] is running, or at the top level
     * outside any [body].
     */
    fun element(
        name: String,
        attributes: Map<String, Any?> = emptyMap(),
        text: Any? = null,
        body: (() -> Unit)? = null
    ) {
        val node = Node(name, attributes, text)
        (current?.children ?: roots).add(node)
        if (body != null) {
            val parent = current
            current = node
            try {
                body()
            } finally {
                current = parent
            }
        }
    }

    /**
     * Returns the document, starting with an XML declaration, with no line break after
     * the last element.
     */
    override fun toString(): String {
        val out = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        roots.forEach { render(it, 0, out) }
        return out.toString()
    }

    private fun render(node: Node, depth: Int, out: StringBuilder) {
        out.append('\n')
        repeat(depth) { out.append("  ") }
        out.append('<').append(node.name)
        node.attributes.forEach { (key, value) ->
            if (value != null) {
                out.append(' ').append(key).append("=\"").append(escape(value.toString(), true)).append('"')
            }
        }
        when {
            node.children.isNotEmpty() -> {
                out.append('>')
                node.children.forEach { render(it, depth + 1, out) }
                out.append('\n')
                repeat(depth) { out.append("  ") }
                out.append("</").append(node.name).append('>')
            }
            node.text != null ->
                out.append('>').append(escape(node.text.toString(), false)).append("</").append(node.name).append('>')
            else -> out.append(" />")
        }
    }

    private fun escape(value: String, attribute: Boolean): String {
        val out = StringBuilder(value.length)
        value.forEach {
            when {
                it == '&' -> out.append("&amp;")
                it == '<' -> out.append("&lt;")
                it == '>' -> out.append("&gt;")
                it == '"' && attribute -> out.append("&quot;")
                it == '\t' && attribute -> out.append("&#09;")
                it == '\n' && attribute -> out.append("&#10;")
                it == '\r' && attribute -> out.append("&#13;")
                else -> out.append(it)
            }
        }
        return out.toString()
    }
}
