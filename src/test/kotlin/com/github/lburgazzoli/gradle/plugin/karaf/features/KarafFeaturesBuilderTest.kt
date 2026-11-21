package com.github.lburgazzoli.gradle.plugin.karaf.features

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KarafFeaturesBuilderTest {
    /**
     * The expected text is what Groovy's MarkupBuilder, which wrote the file before, writes for the same attribute.
     */
    @Test
    fun `an attribute value escapes markup, quotes, and whitespace other than spaces`() {
        val builder = KarafFeaturesBuilder()
        builder.element("feature", mapOf("description" to "a&b <c> \"d\" 'e'\tf\ng\rh"))

        assertThat(builder.toString()).endsWith(
            "<feature description=\"a&amp;b &lt;c&gt; &quot;d&quot; 'e'&#09;f&#10;g&#13;h\" />"
        )
    }

    @Test
    fun `element text escapes markup and keeps quotes and whitespace`() {
        val builder = KarafFeaturesBuilder()
        builder.element("details", text = "a&b <c> \"d\"\tf")

        assertThat(builder.toString()).endsWith("<details>a&amp;b &lt;c&gt; \"d\"\tf</details>")
    }
}
